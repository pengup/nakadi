package org.zalando.nakadi.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zalando.nakadi.config.NakadiSettings;
import org.zalando.nakadi.domain.AdminResource;
import org.zalando.nakadi.domain.AllDataAccessResource;
import org.zalando.nakadi.domain.Permission;
import org.zalando.nakadi.domain.ResourceAuthorization;
import org.zalando.nakadi.exceptions.UnableProcessException;
import org.zalando.nakadi.exceptions.runtime.DbWriteOperationsBlockedException;
import org.zalando.nakadi.plugin.api.authz.AuthorizationService;
import org.zalando.nakadi.plugin.api.authz.Resource;
import org.zalando.nakadi.repository.db.AuthorizationDbRepository;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.zalando.nakadi.domain.AdminResource.ADMIN_RESOURCE;
import static org.zalando.nakadi.domain.AllDataAccessResource.ALL_DATA_ACCESS_RESOURCE;

@Service
public class AdminService {

    private static final Logger LOG = LoggerFactory.getLogger(AdminService.class);

    private final AuthorizationDbRepository authorizationDbRepository;
    private final AuthorizationService authorizationService;
    private final FeatureToggleService featureToggleService;
    private final NakadiSettings nakadiSettings;
    private Cache<String, List<Permission>> resourceCache;

    @Autowired
    public AdminService(final AuthorizationDbRepository authorizationDbRepository,
                        final AuthorizationService authorizationService,
                        final FeatureToggleService featureToggleService,
                        final NakadiSettings nakadiSettings) {
        this.authorizationDbRepository = authorizationDbRepository;
        this.authorizationService = authorizationService;
        this.featureToggleService = featureToggleService;
        this.nakadiSettings = nakadiSettings;
        this.resourceCache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();
    }

    public List<Permission> getAdmins() {
        return addDefaultAdmin(authorizationDbRepository.listAdmins());
    }

    public void updateAdmins(final List<Permission> newAdmins) throws DbWriteOperationsBlockedException {
        if (featureToggleService.isFeatureEnabled(FeatureToggleService.Feature.DISABLE_DB_WRITE_OPERATIONS)) {
            throw new DbWriteOperationsBlockedException("Cannot update admins: write operations on DB " +
                    "are blocked by feature flag.");
        }
        validateAllAdmins(newAdmins);
        final List<Permission> currentAdmins = authorizationDbRepository.listAdmins();
        final List<Permission> add = removeDefaultAdmin(newAdmins.stream()
                .filter(p -> !currentAdmins.stream().anyMatch(Predicate.isEqual(p))).collect(Collectors.toList()));
        final List<Permission> delete = removeDefaultAdmin(currentAdmins.stream()
                .filter(p -> !newAdmins.stream().anyMatch(Predicate.isEqual(p))).collect(Collectors.toList()));
        authorizationDbRepository.update(add, delete);
    }

    public boolean isAdmin(final AuthorizationService.Operation operation) {
        final List<Permission> permissions = getAdmins();
        final Resource resource = new AdminResource(ADMIN_RESOURCE,
                ResourceAuthorization.fromPermissionsList(permissions));
        return authorizationService.isAuthorized(operation, resource);
    }

    public boolean hasAllDataAccess(final AuthorizationService.Operation operation) {
        try {
            final List<Permission> permissions = resourceCache.get(ALL_DATA_ACCESS_RESOURCE,
                    () -> authorizationDbRepository.listAllDataAccess());
            final Resource resource = new AllDataAccessResource(ALL_DATA_ACCESS_RESOURCE,
                    ResourceAuthorization.fromPermissionsList(permissions));
            return authorizationService.isAuthorized(operation, resource);
        } catch (ExecutionException e) {
            LOG.error("Could not determine whether this application has all data access", e);
            return false;
        }
    }

    private List<Permission> addDefaultAdmin(final List<Permission> permissions) {
        for (final AuthorizationService.Operation operation : AuthorizationService.Operation.values()) {
            permissions.add(new Permission(ADMIN_RESOURCE, operation, nakadiSettings.getDefaultAdmin()));
        }
        return permissions;
    }

    private List<Permission> removeDefaultAdmin(final List<Permission> permissions) {
        return permissions.stream()
                .filter(p -> !p.getAuthorizationAttribute().equals(nakadiSettings.getDefaultAdmin()))
                .collect(Collectors.toList());
    }

    private void validateAllAdmins(final List<Permission> admins) throws UnableProcessException {
        final List<Permission> invalid = admins.stream().filter(permission ->
                !authorizationService.isAuthorizationAttributeValid(permission.getAuthorizationAttribute()))
                .collect(Collectors.toList());
        if (!invalid.isEmpty()) {
            final String message = invalid.stream()
                    .map(permission -> String.format("authorization attribute %s:%s is invalid",
                            permission.getAuthorizationAttribute().getDataType(),
                            permission.getAuthorizationAttribute().getValue()))
                    .collect(Collectors.joining(", "));
            throw new UnableProcessException(message);
        }
    }
}

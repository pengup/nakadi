package org.zalando.nakadi.plugin.auth;

import org.zalando.nakadi.plugin.api.authz.AuthorizationAttribute;
import org.zalando.nakadi.plugin.api.authz.AuthorizationService;
import org.zalando.nakadi.plugin.api.authz.Resource;

public class DefaultAuthorizationService implements AuthorizationService {

    @Override
    public boolean isAuthorized(final Operation operation, final Resource resource) {
        return true;
    }

    @Override
    public boolean isAuthorizationAttributeValid(final AuthorizationAttribute authorizationAttribute) {
        return true;
    }
}

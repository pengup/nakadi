package org.zalando.nakadi.controller;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;
import org.zalando.nakadi.config.SecuritySettings;
import org.zalando.nakadi.domain.Storage;
import org.zalando.nakadi.plugin.api.authz.AuthorizationService;
import org.zalando.nakadi.service.AdminService;
import org.zalando.nakadi.service.Result;
import org.zalando.nakadi.service.StorageService;
import org.zalando.problem.spring.web.advice.Responses;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.ResponseEntity.status;

@RestController
public class StoragesController {

    private final SecuritySettings securitySettings;
    private final StorageService storageService;
    private final AdminService adminService;

    @Autowired
    public StoragesController(final SecuritySettings securitySettings, final StorageService storageService,
                              final AdminService adminService) {
        this.securitySettings = securitySettings;
        this.storageService = storageService;
        this.adminService = adminService;
    }

    @RequestMapping(value = "/storages", method = RequestMethod.GET)
    public ResponseEntity<?> listStorages(final NativeWebRequest request) {
        if (!adminService.isAdmin(AuthorizationService.Operation.READ)) {
            return status(FORBIDDEN).build();
        }
        final Result<List<Storage>> result = storageService.listStorages();
        if (result.isSuccessful()) {
            return status(OK).body(result.getValue());
        }
        return Responses.create(result.getProblem(), request);
    }

    @RequestMapping(value = "/storages", method = RequestMethod.POST)
    public ResponseEntity<?> createStorage(@RequestBody final String storage,
                                           final NativeWebRequest request) {
        if (!adminService.isAdmin(AuthorizationService.Operation.WRITE)) {
            return status(FORBIDDEN).build();
        }
        final Result<Void> result = storageService.createStorage(new JSONObject(storage));
        if (result.isSuccessful()) {
            return status(CREATED).build();
        }
        return Responses.create(result.getProblem(), request);
    }

    @RequestMapping(value = "/storages/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getStorage(@PathVariable("id") final String id, final NativeWebRequest request) {
        if (!adminService.isAdmin(AuthorizationService.Operation.READ)) {
            return status(FORBIDDEN).build();
        }
        final Result<Storage> result = storageService.getStorage(id);
        if (result.isSuccessful()) {
            return status(OK).body(result.getValue());
        }
        return Responses.create(result.getProblem(), request);
    }

    @RequestMapping(value = "/storages/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteStorage(@PathVariable("id") final String id, final NativeWebRequest request) {
        if (!adminService.isAdmin(AuthorizationService.Operation.WRITE)) {
            return status(FORBIDDEN).build();
        }
        final Result<Void> result = storageService.deleteStorage(id);
        if (result.isSuccessful()) {
            return status(NO_CONTENT).build();
        }
        return Responses.create(result.getProblem(), request);
    }

    @RequestMapping(value = "/storages/default/{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> setDefaultStorage(@PathVariable("id") final String id, final NativeWebRequest request) {
        if (!adminService.isAdmin(AuthorizationService.Operation.WRITE)) {
            return status(FORBIDDEN).build();
        }
        final Result<Storage> result = storageService.setDefaultStorage(id);
        if (result.isSuccessful()) {
            return status(OK).body(result.getValue());
        }
        return Responses.create(result.getProblem(), request);
    }
}

package com.onetattva.infron.core.controllers;

import com.onetattva.infron.api.ComputeProfilesApi;
import com.onetattva.infron.api.model.*;
import com.onetattva.infron.core.auth.Permission;
import com.onetattva.infron.core.auth.RequiresPermission;
import com.onetattva.infron.core.services.ComputeProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for compute profile (VM spec template) operations.
 * Extends BaseController to inherit metadata handling.
 */
@RestController
public class ComputeProfilesController extends BaseController implements ComputeProfilesApi {

    @Autowired
    private ComputeProfileService computeProfileService;

    @Override
    @RequiresPermission(Permission.COMPUTE_PROFILE_CREATE)
    public ResponseEntity<ComputeProfile> createProfile(ComputeProfileCreate computeProfileCreate) {
        ComputeProfile profile = computeProfileService.createProfile(computeProfileCreate);
        return ResponseEntity.status(201).body(profile);
    }

    @Override
    @RequiresPermission(Permission.COMPUTE_PROFILE_READ)
    public ResponseEntity<ComputeProfile> getProfile(UUID profileId) {
        ComputeProfile profile = computeProfileService.getProfile(profileId);
        return ResponseEntity.ok(profile);
    }

    @Override
    @RequiresPermission(Permission.COMPUTE_PROFILE_READ)
    public ResponseEntity<ComputeProfileList> listProfiles(Integer page, Integer perPage, String sort,
                                                          UUID tenantDatacenterGrantId, String name, List<String> tags) {
        ComputeProfileList profileList = computeProfileService.listProfiles(
                page, perPage, sort, tenantDatacenterGrantId, name, tags);
        return ResponseEntity.ok(profileList);
    }

    @Override
    @RequiresPermission(Permission.COMPUTE_PROFILE_EDIT)
    public ResponseEntity<ComputeProfile> updateProfile(UUID profileId, ComputeProfileUpdate computeProfileUpdate) {
        ComputeProfile profile = computeProfileService.updateProfile(profileId, computeProfileUpdate);
        return ResponseEntity.ok(profile);
    }

    @Override
    @RequiresPermission(Permission.COMPUTE_PROFILE_MANAGE)
    public ResponseEntity<Void> deleteProfile(UUID profileId) {
        computeProfileService.deleteProfile(profileId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @RequiresPermission(Permission.COMPUTE_PROFILE_READ)
    public ResponseEntity<Map<String, String>> getProfileMetadata(UUID profileId) {
        return getMetadata(profileId);
    }

    @Override
    @RequiresPermission(Permission.COMPUTE_PROFILE_EDIT)
    public ResponseEntity<Map<String, String>> updateProfileMetadata(UUID profileId, Map<String, String> metadata) {
        return updateMetadata(profileId, metadata);
    }

    // Implement abstract methods from BaseController
    @Override
    protected Map<String, String> getEntityMetadata(UUID id) {
        return computeProfileService.getMetadata(id);
    }

    @Override
    protected Map<String, String> updateEntityMetadata(UUID id, Map<String, String> metadata) {
        return computeProfileService.updateMetadata(id, metadata);
    }
}

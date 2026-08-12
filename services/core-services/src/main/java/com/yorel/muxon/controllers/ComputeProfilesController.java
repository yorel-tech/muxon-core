/*
 * Copyright 2026 Yorel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yorel.muxon.controllers;

import com.yorel.muxon.api.ComputeProfilesApi;
import com.yorel.muxon.api.model.ComputeProfile;
import com.yorel.muxon.api.model.ComputeProfileCreate;
import com.yorel.muxon.api.model.ComputeProfileList;
import com.yorel.muxon.api.model.ComputeProfileUpdate;
import com.yorel.muxon.auth.Permission;
import com.yorel.muxon.auth.RequiresPermission;
import com.yorel.muxon.services.ComputeProfileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for compute profile (VM spec template) operations. Extends BaseController to inherit
 * metadata handling.
 */
@RestController
public class ComputeProfilesController implements ComputeProfilesApi {

  @Autowired private ComputeProfileService computeProfileService;

  @Override
  @RequiresPermission(Permission.COMPUTE_PROFILE_CREATE)
  public ResponseEntity<ComputeProfile> createComputeProfile(
      @Valid @RequestBody ComputeProfileCreate computeProfileCreate) {
    ComputeProfile profile = computeProfileService.createProfile(computeProfileCreate);
    return ResponseEntity.status(201).body(profile);
  }

  @Override
  @RequiresPermission(Permission.COMPUTE_PROFILE_READ)
  public ResponseEntity<ComputeProfile> getComputeProfile(
      @NotNull @PathVariable("profileId") UUID profileId) {
    ComputeProfile profile = computeProfileService.getProfile(profileId);
    return ResponseEntity.ok(profile);
  }

  @Override
  @RequiresPermission(Permission.COMPUTE_PROFILE_READ)
  public ResponseEntity<ComputeProfileList> listComputeProfiles(
      @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
      @RequestParam(value = "perPage", required = false, defaultValue = "20") Integer perPage,
      @RequestParam(value = "sort", required = false, defaultValue = "name") String sort,
      @RequestParam(value = "tenantDatacenterGrantId", required = false)
          UUID tenantDatacenterGrantId,
      @RequestParam(value = "name", required = false) String name,
      @RequestParam(value = "tags", required = false) String tags) {
    // Convert comma-separated tags string to List<String>
    List<String> tagsList = tags != null ? List.of(tags.split(",")) : null;
    ComputeProfileList profileList =
        computeProfileService.listProfiles(
            page, perPage, sort, tenantDatacenterGrantId, name, tagsList);
    return ResponseEntity.ok(profileList);
  }

  @Override
  @RequiresPermission(Permission.COMPUTE_PROFILE_EDIT)
  public ResponseEntity<ComputeProfile> updateComputeProfile(
      @NotNull @PathVariable("profileId") UUID profileId,
      @Valid @RequestBody ComputeProfileUpdate computeProfileUpdate) {
    ComputeProfile profile = computeProfileService.updateProfile(profileId, computeProfileUpdate);
    return ResponseEntity.ok(profile);
  }

  @Override
  @RequiresPermission(Permission.COMPUTE_PROFILE_MANAGE)
  public ResponseEntity<Void> deleteComputeProfile(
      @NotNull @PathVariable("profileId") UUID profileId) {
    computeProfileService.deleteProfile(profileId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @RequiresPermission(Permission.COMPUTE_PROFILE_READ)
  public ResponseEntity<Map<String, String>> getComputeProfileMetadata(
      @NotNull @PathVariable("profileId") UUID profileId) {
    Map<String, String> metadata = computeProfileService.getEntityMetadata(profileId);
    return ResponseEntity.ok(metadata);
  }

  @Override
  @RequiresPermission(Permission.COMPUTE_PROFILE_EDIT)
  public ResponseEntity<Map<String, String>> updateComputeProfileMetadata(
      @NotNull @PathVariable("profileId") UUID profileId,
      @Valid @RequestBody Map<String, String> requestBody) {
    Map<String, String> metadata =
        computeProfileService.updateEntityMetadata(profileId, requestBody);
    return ResponseEntity.ok(metadata);
  }

  @Override
  @RequiresPermission(Permission.COMPUTE_PROFILE_EDIT)
  public ResponseEntity<ComputeProfile> patchComputeProfile(
      @NotNull @PathVariable("profileId") UUID profileId,
      @Valid @RequestBody ComputeProfileUpdate computeProfileUpdate) {
    // For PATCH, we can use the same update method since it handles partial updates
    ComputeProfile profile = computeProfileService.updateProfile(profileId, computeProfileUpdate);
    return ResponseEntity.ok(profile);
  }
}

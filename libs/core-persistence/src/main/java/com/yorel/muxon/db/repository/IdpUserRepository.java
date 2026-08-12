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
package com.yorel.muxon.db.repository;

import com.yorel.muxon.db.model.IdpUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IdpUserRepository extends JpaRepository<IdpUserEntity, java.util.UUID> {

  /** Find idp_user by identity provider and external id */
  @Query(
      "SELECT u FROM IdpUserEntity u WHERE u.identityProviderId = :idpId AND u.externalId = :externalId")
  IdpUserEntity findByIdentityProviderIdAndExternalId(
      @Param("idpId") java.util.UUID idpId, @Param("externalId") String externalId);

  /** Find idp_user by identity provider */
  @Query("SELECT u FROM IdpUserEntity u WHERE u.identityProviderId = :idpId")
  java.util.List<IdpUserEntity> findByIdentityProviderId(@Param("idpId") java.util.UUID idpId);
}

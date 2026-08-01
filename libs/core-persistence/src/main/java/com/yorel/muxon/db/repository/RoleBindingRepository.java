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

import com.yorel.muxon.api.enums.RoleBindingSubjectType;
import com.yorel.muxon.api.enums.RoleScopeType;
import com.yorel.muxon.db.model.RoleBindingEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleBindingRepository
    extends JpaRepository<RoleBindingEntity, UUID>, JpaSpecificationExecutor<RoleBindingEntity> {

  List<RoleBindingEntity> findByRoleName(String roleName);

  List<RoleBindingEntity> findBySubjectTypeAndSubjectId(
      RoleBindingSubjectType subjectType, String subjectId);

  List<RoleBindingEntity> findByRoleNameAndScopeTypeAndScopeId(
      String roleName, RoleScopeType scopeType, UUID scopeId);
}

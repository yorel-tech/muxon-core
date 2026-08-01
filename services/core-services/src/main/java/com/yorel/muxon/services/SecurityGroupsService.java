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
package com.yorel.muxon.services;

import com.yorel.muxon.db.model.SecurityGroupEntity;
import com.yorel.muxon.db.model.SecurityGroupRuleEntity;
import com.yorel.muxon.db.model.SecurityGroupRuleEntity.SgDirection;
import com.yorel.muxon.db.model.SecurityGroupRuleEntity.SgProtocol;
import com.yorel.muxon.db.model.VpcEntity;
import com.yorel.muxon.db.repository.SecurityGroupRepository;
import com.yorel.muxon.db.repository.SecurityGroupRuleRepository;
import com.yorel.muxon.db.repository.VpcRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecurityGroupsService {

  @Autowired private SecurityGroupRepository sgRepository;

  @Autowired private SecurityGroupRuleRepository ruleRepository;

  @Autowired private VpcRepository vpcRepository;

  public List<SecurityGroupEntity> listByVpc(UUID vpcId) {
    return sgRepository.findByVpcId(vpcId);
  }

  @Transactional
  public SecurityGroupEntity create(UUID vpcId, String name, String description) {
    VpcEntity vpc =
        vpcRepository
            .findById(vpcId)
            .orElseThrow(() -> new EntityNotFoundException("VPC not found: " + vpcId));

    SecurityGroupEntity entity = new SecurityGroupEntity();
    entity.setVpc(vpc);
    entity.setName(name);
    entity.setDescription(description);
    SecurityGroupEntity saved = sgRepository.save(entity);

    // Auto-create default egress allow-all rule
    SecurityGroupRuleEntity egressAll = new SecurityGroupRuleEntity();
    egressAll.setSecurityGroup(saved);
    egressAll.setDirection(SgDirection.EGRESS);
    egressAll.setProtocol(SgProtocol.ALL);
    egressAll.setCidr("0.0.0.0/0");
    ruleRepository.save(egressAll);

    return saved;
  }

  public SecurityGroupEntity getById(UUID sgId) {
    return sgRepository
        .findById(sgId)
        .orElseThrow(() -> new EntityNotFoundException("Security group not found: " + sgId));
  }

  @Transactional
  public void delete(UUID sgId) {
    if (!sgRepository.existsById(sgId)) {
      throw new EntityNotFoundException("Security group not found: " + sgId);
    }
    sgRepository.deleteById(sgId);
  }

  public List<SecurityGroupRuleEntity> listRules(UUID sgId) {
    return ruleRepository.findBySecurityGroupId(sgId);
  }

  @Transactional
  public SecurityGroupRuleEntity addRule(
      UUID sgId,
      SgDirection direction,
      SgProtocol protocol,
      Integer portMin,
      Integer portMax,
      String cidr,
      UUID sourceSgId) {
    SecurityGroupEntity sg = getById(sgId);

    if (sourceSgId != null && cidr != null) {
      throw new IllegalArgumentException("Provide either cidr or sourceSecurityGroupId, not both");
    }

    if (sourceSgId != null) {
      SecurityGroupEntity sourceSg =
          sgRepository
              .findById(sourceSgId)
              .orElseThrow(
                  () ->
                      new EntityNotFoundException(
                          "Source security group not found: " + sourceSgId));
      // Validate same VPC
      if (!sourceSg.getVpc().getId().equals(sg.getVpc().getId())) {
        throw new IllegalArgumentException(
            "INVALID: Source security group must be in the same VPC");
      }
    }

    SecurityGroupRuleEntity rule = new SecurityGroupRuleEntity();
    rule.setSecurityGroup(sg);
    rule.setDirection(direction);
    rule.setProtocol(protocol != null ? protocol : SgProtocol.ALL);
    rule.setPortRangeMin(portMin);
    rule.setPortRangeMax(portMax);
    rule.setCidr(cidr);
    if (sourceSgId != null) {
      rule.setSourceSecurityGroup(sgRepository.getReferenceById(sourceSgId));
    }
    return ruleRepository.save(rule);
  }

  @Transactional
  public void deleteRule(UUID ruleId) {
    if (!ruleRepository.existsById(ruleId)) {
      throw new EntityNotFoundException("Security group rule not found: " + ruleId);
    }
    ruleRepository.deleteById(ruleId);
  }
}

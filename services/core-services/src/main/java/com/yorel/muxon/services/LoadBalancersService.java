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

import com.yorel.muxon.db.model.FloatingIpEntity;
import com.yorel.muxon.db.model.LoadBalancerEntity;
import com.yorel.muxon.db.model.LoadBalancerEntity.LbScheme;
import com.yorel.muxon.db.model.SubnetEntity;
import com.yorel.muxon.db.model.VpcEntity;
import com.yorel.muxon.db.repository.FloatingIpRepository;
import com.yorel.muxon.db.repository.LoadBalancerRepository;
import com.yorel.muxon.db.repository.SubnetRepository;
import com.yorel.muxon.db.repository.VpcRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoadBalancersService {

  @Autowired private LoadBalancerRepository lbRepository;

  @Autowired private VpcRepository vpcRepository;

  @Autowired private SubnetRepository subnetRepository;

  @Autowired private FloatingIpRepository floatingIpRepository;

  // Listener and target repositories would normally be injected here
  // Using entity manager or separate repositories; simplified for brevity

  public List<LoadBalancerEntity> listByVpc(UUID vpcId) {
    return lbRepository.findByVpcId(vpcId);
  }

  @Transactional
  public LoadBalancerEntity create(
      UUID vpcId, String name, UUID subnetId, LbScheme scheme, UUID floatingIpId) {
    VpcEntity vpc =
        vpcRepository
            .findById(vpcId)
            .orElseThrow(() -> new EntityNotFoundException("VPC not found: " + vpcId));
    SubnetEntity subnet =
        subnetRepository
            .findById(subnetId)
            .orElseThrow(() -> new EntityNotFoundException("Subnet not found: " + subnetId));

    if (scheme == LbScheme.INTERNET_FACING && floatingIpId == null) {
      throw new IllegalArgumentException(
          "UNPROCESSABLE: INTERNET_FACING load balancer requires a floatingIpId");
    }

    LoadBalancerEntity entity = new LoadBalancerEntity();
    entity.setVpc(vpc);
    entity.setSubnet(subnet);
    entity.setName(name);
    entity.setScheme(scheme);
    entity.setStatus("ACTIVE");

    if (floatingIpId != null) {
      FloatingIpEntity fip =
          floatingIpRepository
              .findById(floatingIpId)
              .orElseThrow(
                  () -> new EntityNotFoundException("Floating IP not found: " + floatingIpId));
      entity.setFloatingIp(fip);
    }

    return lbRepository.save(entity);
  }

  public LoadBalancerEntity getById(UUID lbId) {
    return lbRepository
        .findById(lbId)
        .orElseThrow(() -> new EntityNotFoundException("Load balancer not found: " + lbId));
  }

  @Transactional
  public void delete(UUID lbId) {
    if (!lbRepository.existsById(lbId)) {
      throw new EntityNotFoundException("Load balancer not found: " + lbId);
    }
    lbRepository.deleteById(lbId);
  }
}

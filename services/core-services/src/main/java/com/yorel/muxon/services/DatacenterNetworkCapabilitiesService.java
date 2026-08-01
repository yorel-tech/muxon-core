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

import com.yorel.muxon.db.model.DatacenterEntity;
import com.yorel.muxon.db.model.DatacenterNetworkCapabilitiesEntity;
import com.yorel.muxon.db.repository.DatacenterNetworkCapabilitiesRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatacenterNetworkCapabilitiesService {

  @Autowired private DatacenterNetworkCapabilitiesRepository capabilitiesRepository;

  @Transactional
  public DatacenterNetworkCapabilitiesEntity createDefaults(DatacenterEntity datacenter) {
    DatacenterNetworkCapabilitiesEntity entity = new DatacenterNetworkCapabilitiesEntity();
    entity.setDatacenter(datacenter);
    // Safe defaults: all false
    entity.setPublicIpSupported(false);
    entity.setVpnSupported(false);
    entity.setBgpSupported(false);
    entity.setHaGatewaySupported(false);
    entity.setVxlanSupported(false);
    entity.setMultiRegionSupported(false);
    entity.setL7LbSupported(false);
    entity.setIpv6Supported(false);
    entity.setDualStackSupported(false);
    return capabilitiesRepository.save(entity);
  }

  public DatacenterNetworkCapabilitiesEntity getByDatacenterId(UUID datacenterId) {
    return capabilitiesRepository
        .findByDatacenterId(datacenterId)
        .orElseThrow(
            () ->
                new EntityNotFoundException(
                    "Network capabilities not found for datacenter: " + datacenterId));
  }

  @Transactional
  public DatacenterNetworkCapabilitiesEntity update(
      UUID datacenterId,
      Boolean publicIpSupported,
      Boolean vpnSupported,
      Boolean bgpSupported,
      Boolean haGatewaySupported,
      Boolean vxlanSupported,
      Boolean multiRegionSupported,
      Boolean l7LbSupported,
      Boolean ipv6Supported,
      Boolean dualStackSupported) {
    DatacenterNetworkCapabilitiesEntity entity = getByDatacenterId(datacenterId);
    if (publicIpSupported != null) entity.setPublicIpSupported(publicIpSupported);
    if (vpnSupported != null) entity.setVpnSupported(vpnSupported);
    if (bgpSupported != null) entity.setBgpSupported(bgpSupported);
    if (haGatewaySupported != null) entity.setHaGatewaySupported(haGatewaySupported);
    if (vxlanSupported != null) entity.setVxlanSupported(vxlanSupported);
    if (multiRegionSupported != null) entity.setMultiRegionSupported(multiRegionSupported);
    if (l7LbSupported != null) entity.setL7LbSupported(l7LbSupported);
    if (ipv6Supported != null) entity.setIpv6Supported(ipv6Supported);
    if (dualStackSupported != null) entity.setDualStackSupported(dualStackSupported);
    return capabilitiesRepository.save(entity);
  }
}

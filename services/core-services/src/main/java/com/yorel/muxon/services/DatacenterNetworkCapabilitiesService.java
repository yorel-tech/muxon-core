package com.yorel.muxon.services;

import com.yorel.muxon.db.model.DatacenterEntity;
import com.yorel.muxon.db.model.DatacenterNetworkCapabilitiesEntity;
import com.yorel.muxon.db.repository.DatacenterNetworkCapabilitiesRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DatacenterNetworkCapabilitiesService {

    @Autowired
    private DatacenterNetworkCapabilitiesRepository capabilitiesRepository;

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
        return capabilitiesRepository.findByDatacenterId(datacenterId)
                .orElseThrow(() -> new EntityNotFoundException("Network capabilities not found for datacenter: " + datacenterId));
    }

    @Transactional
    public DatacenterNetworkCapabilitiesEntity update(UUID datacenterId,
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

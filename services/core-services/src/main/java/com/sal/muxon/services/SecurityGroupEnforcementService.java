package com.sal.muxon.services;

import com.sal.muxon.db.model.SecurityGroupEntity;
import com.sal.muxon.db.model.SecurityGroupRuleEntity;
import com.sal.muxon.db.repository.SecurityGroupRepository;
import com.sal.muxon.db.repository.SecurityGroupRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Translates security group rules into nftables/iptables rules applied at the virtual NIC level.
 *
 * <p>Security groups are enforced at the vNIC, not at the subnet bridge. This enables
 * micro-segmentation: two VMs on the same subnet can have completely different traffic policies.
 *
 * <p>Called from LibvirtNetworkProvider.applyNicPolicy() after VM NIC attachment.
 */
@Service
public class SecurityGroupEnforcementService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityGroupEnforcementService.class);

    @Autowired
    private SecurityGroupRepository sgRepository;

    @Autowired
    private SecurityGroupRuleRepository ruleRepository;

    /**
     * Translate security group rules for a set of security group IDs into
     * nftables/iptables rule strings for the given VM NIC.
     *
     * @param securityGroupIds List of security group IDs attached to this NIC
     * @param vmExternalId     Provider VM identifier
     * @param nicMacAddress    MAC address of the virtual NIC
     * @return List of nftables/iptables rule strings
     */
    public List<String> buildNicRules(List<UUID> securityGroupIds, String vmExternalId, String nicMacAddress) {
        List<String> rules = new ArrayList<>();

        for (UUID sgId : securityGroupIds) {
            SecurityGroupEntity sg = sgRepository.findById(sgId).orElse(null);
            if (sg == null) {
                logger.warn("Security group {} not found, skipping", sgId);
                continue;
            }

            List<SecurityGroupRuleEntity> sgRules = ruleRepository.findBySecurityGroupId(sgId);
            for (SecurityGroupRuleEntity rule : sgRules) {
                String nftRule = translateToNftables(rule, nicMacAddress);
                if (nftRule != null) {
                    rules.add(nftRule);
                }
            }
        }

        return rules;
    }

    /**
     * Translate a single security group rule to an nftables rule string.
     * In production this would produce full nft syntax; simplified here to a descriptor.
     */
    private String translateToNftables(SecurityGroupRuleEntity rule, String nicMacAddress) {
        StringBuilder sb = new StringBuilder();
        sb.append("# SG rule mac=").append(nicMacAddress);
        sb.append(" direction=").append(rule.getDirection());
        sb.append(" proto=").append(rule.getProtocol());

        if (rule.getPortRangeMin() != null) {
            sb.append(" dport=").append(rule.getPortRangeMin());
            if (rule.getPortRangeMax() != null && !rule.getPortRangeMax().equals(rule.getPortRangeMin())) {
                sb.append("-").append(rule.getPortRangeMax());
            }
        }

        if (rule.getCidr() != null) {
            sb.append(" cidr=").append(rule.getCidr());
        } else if (rule.getSourceSecurityGroup() != null) {
            sb.append(" src-sg=").append(rule.getSourceSecurityGroup().getId());
        }

        sb.append(" action=accept");
        return sb.toString();
    }
}

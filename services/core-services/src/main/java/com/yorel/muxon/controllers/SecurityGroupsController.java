package com.yorel.muxon.controllers;

import com.yorel.muxon.db.model.SecurityGroupEntity;
import com.yorel.muxon.db.model.SecurityGroupRuleEntity;
import com.yorel.muxon.db.model.SecurityGroupRuleEntity.SgDirection;
import com.yorel.muxon.db.model.SecurityGroupRuleEntity.SgProtocol;
import com.yorel.muxon.services.SecurityGroupsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/vpcs/{vpcId}/security-groups")
public class SecurityGroupsController {

    @Autowired
    private SecurityGroupsService securityGroupsService;

    @GetMapping
    public ResponseEntity<List<SecurityGroupEntity>> listSecurityGroups(
            @PathVariable UUID tenantId, @PathVariable UUID vpcId) {
        return ResponseEntity.ok(securityGroupsService.listByVpc(vpcId));
    }

    @PostMapping
    public ResponseEntity<SecurityGroupEntity> createSecurityGroup(
            @PathVariable UUID tenantId, @PathVariable UUID vpcId,
            @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        return ResponseEntity.status(201).body(securityGroupsService.create(vpcId, name, description));
    }

    @GetMapping("/{securityGroupId}")
    public ResponseEntity<SecurityGroupEntity> getSecurityGroup(
            @PathVariable UUID tenantId, @PathVariable UUID vpcId,
            @PathVariable UUID securityGroupId) {
        return ResponseEntity.ok(securityGroupsService.getById(securityGroupId));
    }

    @DeleteMapping("/{securityGroupId}")
    public ResponseEntity<Void> deleteSecurityGroup(
            @PathVariable UUID tenantId, @PathVariable UUID vpcId,
            @PathVariable UUID securityGroupId) {
        securityGroupsService.delete(securityGroupId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{securityGroupId}/rules")
    public ResponseEntity<List<SecurityGroupRuleEntity>> listRules(
            @PathVariable UUID tenantId, @PathVariable UUID vpcId,
            @PathVariable UUID securityGroupId) {
        return ResponseEntity.ok(securityGroupsService.listRules(securityGroupId));
    }

    @PostMapping("/{securityGroupId}/rules")
    public ResponseEntity<SecurityGroupRuleEntity> createRule(
            @PathVariable UUID tenantId, @PathVariable UUID vpcId,
            @PathVariable UUID securityGroupId,
            @RequestBody Map<String, Object> body) {
        SgDirection direction = SgDirection.valueOf((String) body.get("direction"));
        SgProtocol protocol = body.get("protocol") != null
                ? SgProtocol.valueOf((String) body.get("protocol")) : SgProtocol.ALL;
        Integer portMin = body.get("portRangeMin") != null ? ((Number) body.get("portRangeMin")).intValue() : null;
        Integer portMax = body.get("portRangeMax") != null ? ((Number) body.get("portRangeMax")).intValue() : null;
        String cidr = (String) body.get("cidr");
        UUID sourceSgId = body.get("sourceSecurityGroupId") != null
                ? UUID.fromString((String) body.get("sourceSecurityGroupId")) : null;
        return ResponseEntity.status(201).body(
                securityGroupsService.addRule(securityGroupId, direction, protocol, portMin, portMax, cidr, sourceSgId));
    }

    @DeleteMapping("/{securityGroupId}/rules/{ruleId}")
    public ResponseEntity<Void> deleteRule(
            @PathVariable UUID tenantId, @PathVariable UUID vpcId,
            @PathVariable UUID securityGroupId, @PathVariable UUID ruleId) {
        securityGroupsService.deleteRule(ruleId);
        return ResponseEntity.noContent().build();
    }
}

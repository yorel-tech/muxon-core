package com.krito.muxon.services;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.krito.muxon.db.model.AuditLogEntity;
import com.krito.muxon.db.model.IdpUserEntity;
import com.krito.muxon.db.model.TenantEntity;
import com.krito.muxon.db.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for logging audit actions.
 */
@Service
public class AuditService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    
    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Log an audit action.
     *
     * @param action The action being performed (e.g., "provider:create", "user:update")
     * @param actorUser The user performing the action (can be null for system actions)
     * @param tenant The tenant context (can be null for system-level actions)
     * @param resourceId The ID of the resource being acted upon (can be null)
     * @param requestData Additional request data to log (can be null)
     */
    @Transactional
    public void logAction(String action, IdpUserEntity actorUser, TenantEntity tenant,
                         UUID resourceId, Object requestData) {
        try {
            AuditLogEntity auditLog = new AuditLogEntity();
            auditLog.setId(UUID.randomUUID());
            auditLog.setAction(action);
            auditLog.setActorUser(actorUser);
            auditLog.setTenant(tenant);
            auditLog.setCreatedAt(Instant.now());
            
            // Build payload with resource ID and request data
            if (resourceId != null || requestData != null) {
                Map<String, Object> payload = new HashMap<>();
                if (resourceId != null) {
                    payload.put("resourceId", resourceId.toString());
                    payload.put("resourceType", extractResourceType(action));
                }
                if (requestData != null) {
                    payload.put("data", requestData);
                }
                try {
                    auditLog.setPayload(objectMapper.writeValueAsString(payload));
                } catch (JacksonException e) {
                    logger.warn("Failed to serialize audit payload: {}", e.getMessage());
                }
            }
            
            auditLogRepository.save(auditLog);
            
            logger.debug("Audit log created for action: {} by user: {}", action,
                actorUser != null ? actorUser.getId() : "system");
        } catch (Exception e) {
            logger.error("Failed to log audit entry for action: {}", action, e);
        }
    }
    
    /**
     * Log an audit action with simplified parameters.
     *
     * @param action The action being performed
     * @param resourceId The ID of the resource (can be null)
     * @param requestData Additional data (can be null)
     */
    public void logAction(String action, UUID resourceId, Object requestData) {
        logAction(action, null, null, resourceId, requestData);
    }
    
    /**
     * Log an audit action for a specific resource.
     *
     * @param action The action being performed
     * @param resourceId The ID of the resource
     */
    public void logAction(String action, UUID resourceId) {
        logAction(action, null, null, resourceId, null);
    }
    
    /**
     * Log a simple audit action.
     *
     * @param action The action being performed
     */
    public void logAction(String action) {
        logAction(action, null, null, null, null);
    }
    
    private String extractResourceType(String action) {
        if (action == null || !action.contains(":")) {
            return "unknown";
        }
        return action.split(":")[0];
    }
}
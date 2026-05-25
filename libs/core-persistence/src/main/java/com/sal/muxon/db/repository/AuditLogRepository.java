package com.sal.muxon.db.repository;

import com.sal.muxon.db.model.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, java.util.UUID> {

}

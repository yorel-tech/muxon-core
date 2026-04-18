package com.krito.muxon.db.repository;

import com.krito.muxon.db.model.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, java.util.UUID> {

}

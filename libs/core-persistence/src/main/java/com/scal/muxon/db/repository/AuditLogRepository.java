package com.scal.muxon.db.repository;

import com.scal.muxon.db.model.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, java.util.UUID> {

}

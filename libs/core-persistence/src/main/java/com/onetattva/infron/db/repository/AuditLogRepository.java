package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, java.util.UUID> {

}

package com.fag.mautc.auditor.repository;

import com.fag.mautc.auditor.model.AuditError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditErrorRepository extends JpaRepository<AuditError, String> {
}

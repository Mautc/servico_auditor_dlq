package com.fag.mautc.auditor.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_errors")
public class AuditError {

    @Id
    @Column(name = "error_id", nullable = false, updatable = false)
    private String errorId;

    @Column(name = "queue_name", nullable = false)
    private String queueName;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AuditStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private Severity severity;

    public AuditError() {}

    public AuditError(String queueName, String payload, Severity severity) {
        this.errorId = UUID.randomUUID().toString();
        this.queueName = queueName;
        this.payload = payload;
        this.timestamp = Instant.now();
        this.status = AuditStatus.PENDING_ANALYSIS;
        this.severity = severity;
    }

    public String getErrorId() {
        return errorId;
    }

    public String getQueueName() {
        return queueName;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public AuditStatus getStatus() {
        return status;
    }

    public Severity getSeverity() {
        return severity;
    }
}

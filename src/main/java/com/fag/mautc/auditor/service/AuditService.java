package com.fag.mautc.auditor.service;

import com.fag.mautc.auditor.dto.OrderEventDTO;
import com.fag.mautc.auditor.model.AuditError;
import com.fag.mautc.auditor.model.Severity;
import com.fag.mautc.auditor.repository.AuditErrorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditErrorRepository repository;

    public AuditService(AuditErrorRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AuditError registrarErro(OrderEventDTO evento, String payloadBruto) {
        Severity severity = classificarSeveridade(evento.totalItems());

        log.info("Triagem de severidade: total de itens={}, severity={}", evento.totalItems(), severity);

        AuditError auditError = new AuditError(evento.getOrigin(), payloadBruto, severity);
        AuditError salvo = repository.save(auditError);

        log.info("Erro registrado com id={}, severity={}", salvo.getErrorId(), salvo.getSeverity());
        return salvo;
    }

    private Severity classificarSeveridade(int totalItens) {
        if (totalItens > 100) {
            return Severity.HIGH;
        } else if (totalItens >= 50) {
            return Severity.MEDIUM;
        } else {
            return Severity.LOW;
        }
    }
}

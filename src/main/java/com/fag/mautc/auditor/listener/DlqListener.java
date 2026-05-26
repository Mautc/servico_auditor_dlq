package com.fag.mautc.auditor.listener;

import com.fag.mautc.auditor.dto.OrderEventDTO;
import com.fag.mautc.auditor.model.AuditError;
import com.fag.mautc.auditor.service.AuditService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
public class DlqListener {

    private static final Logger log = LoggerFactory.getLogger(DlqListener.class);

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public DlqListener(AuditService auditService, ObjectMapper objectMapper) {
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @SqsListener("${queue.dlq-name}")
    public void consumir(Message<String> message, Acknowledgement acknowledgement) {
        String payload = message.getPayload();
        log.info("Mensagem recebida da DLQ: {}", payload);

        try {
            OrderEventDTO evento = objectMapper.readValue(payload, OrderEventDTO.class);
            AuditError erro = auditService.registrarErro(evento, payload);

            acknowledgement.acknowledge();
            log.info("Mensagem processada e removida da DLQ. errorId={}", erro.getErrorId());

        } catch (JsonProcessingException e) {
            log.error("Falha ao desserializar mensagem da DLQ. Payload={}", payload, e);

            throw new RuntimeException("Falha ao desserializar mensagem da DLQ", e);
        } catch (Exception e) {
            log.error("Erro inesperado ao processar mensagem da DLQ. Payload={}", payload, e);
            throw new RuntimeException("Erro ao processar mensagem da DLQ", e);
        }
    }
}

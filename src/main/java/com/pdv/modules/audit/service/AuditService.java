package com.pdv.modules.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdv.modules.audit.entity.AuditLog;
import com.pdv.modules.audit.repository.AuditLogRepository;
import com.pdv.modules.auth.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Registra uma ação de auditoria de forma assíncrona.
     * Usa Propagation.REQUIRES_NEW para garantir que o log seja salvo
     * mesmo se a transação principal fizer rollback.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String acao, String entidade, String entidadeId,
                           Object payloadAntes, Object payloadDepois) {
        try {
            UUID tenantId = null;
            UUID usuarioId = null;

            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal p) {
                tenantId = p.getTenantId();
                usuarioId = p.getId();
            }

            String ip = extrairIp();
            String userAgent = extrairUserAgent();

            AuditLog log = AuditLog.builder()
                    .tenantId(tenantId)
                    .usuarioId(usuarioId)
                    .acao(acao)
                    .entidade(entidade)
                    .entidadeId(entidadeId)
                    .payloadAntes(payloadAntes != null ? toJson(payloadAntes) : null)
                    .payloadDepois(payloadDepois != null ? toJson(payloadDepois) : null)
                    .ip(ip)
                    .userAgent(userAgent)
                    .build();

            auditLogRepository.save(log);
        } catch (Exception e) {
            // Audit nunca deve quebrar a operação principal
            log.warn("Falha ao registrar audit log: acao={} entidade={} erro={}",
                    acao, entidade, e.getMessage());
        }
    }

    /**
     * Versão simplificada sem payload.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String acao, String entidade, String entidadeId) {
        registrar(acao, entidade, entidadeId, null, null);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    private String extrairIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest request = attrs.getRequest();
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    private String extrairUserAgent() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            return attrs.getRequest().getHeader("User-Agent");
        } catch (Exception e) {
            return null;
        }
    }
}

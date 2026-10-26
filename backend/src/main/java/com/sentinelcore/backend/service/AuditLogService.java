package com.sentinelcore.backend.service;

import com.sentinelcore.backend.dto.AuditLogDTO;
import com.sentinelcore.backend.entity.AuditLog;
import com.sentinelcore.backend.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private static final int MAX_DETAILS_LENGTH = 2000;
    // Defence in depth: redact anything that looks like a credential before it is persisted
    private static final Pattern SECRET_PATTERN = Pattern.compile(
            "(?i)(password|passwd|secret|token|authorization|api[_-]?key|access[_-]?key|credential)(\\s*[=:]\\s*)[^,;\\s}\\]]+");

    private final AuditLogRepository auditLogRepository;

    public Page<AuditLogDTO> getAuditLogs(String search, Pageable pageable) {
        log.info("Fetching audit logs");

        Specification<AuditLog> spec = Specification.where(null);

        if (StringUtils.hasText(search)) {
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("action")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("username")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("details")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("ipAddress")), "%" + search.toLowerCase() + "%")
            ));
        }

        return auditLogRepository.findAll(spec, pageable).map(this::convertToDTO);
    }

    public void logAction(String action, String username, String details, String ipAddress) {
        log.info("Logging action: {} by user: {}", sanitize(action, 255), sanitize(username, 255));
        AuditLog logEntity = AuditLog.builder()
                .action(sanitize(action, 255))
                .username(StringUtils.hasText(username) ? sanitize(username, 255) : "ANONYMOUS")
                .details(sanitize(details, MAX_DETAILS_LENGTH))
                .ipAddress(sanitize(ipAddress, 64))
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(logEntity);
    }

    /**
     * Records a security-relevant event (failed login, access denied, privileged operation) for the
     * current request. Runs in its own transaction so the record survives a rollback of the caller.
     * Never throws: audit failures are logged but must not break request handling.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSecurityEvent(String action, String details) {
        logSecurityEvent(action, currentUsername(), details);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSecurityEvent(String action, String username, String details) {
        try {
            logAction(action, username, details, currentIpAddress());
        } catch (Exception e) {
            log.error("Failed to persist security audit event {}: {}", action, e.getClass().getSimpleName());
        }
    }

    public static String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return "ANONYMOUS";
        }
        return authentication.getName();
    }

    public static String currentIpAddress() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            return request.getRemoteAddr();
        }
        return "0.0.0.0";
    }

    static String sanitize(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String cleaned = SECRET_PATTERN.matcher(value).replaceAll("$1$2[REDACTED]")
                .replaceAll("[\\r\\n\\t]", " ");
        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength) + "…" : cleaned;
    }

    private AuditLogDTO convertToDTO(AuditLog entity) {
        return AuditLogDTO.builder()
                .id(entity.getId())
                .action(entity.getAction())
                .username(entity.getUsername())
                .details(entity.getDetails())
                .ipAddress(entity.getIpAddress())
                .timestamp(entity.getTimestamp())
                .build();
    }
}

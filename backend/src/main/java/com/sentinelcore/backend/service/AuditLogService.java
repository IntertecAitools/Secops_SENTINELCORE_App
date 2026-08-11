package com.sentinelcore.backend.service;

import com.sentinelcore.backend.dto.AuditLogDTO;
import com.sentinelcore.backend.entity.AuditLog;
import com.sentinelcore.backend.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public Page<AuditLogDTO> getAuditLogs(String search, Pageable pageable) {
        log.info("Fetching audit logs with search: {}", search);

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
        log.info("Logging action: {} by user: {}", action, username);
        AuditLog logEntity = AuditLog.builder()
                .action(action)
                .username(username)
                .details(details)
                .ipAddress(ipAddress)
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(logEntity);
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

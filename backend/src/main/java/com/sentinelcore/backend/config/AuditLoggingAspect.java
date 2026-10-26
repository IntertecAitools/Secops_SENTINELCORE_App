package com.sentinelcore.backend.config;

import com.sentinelcore.backend.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.List;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLoggingAspect {

    private final AuditLogService auditLogService;

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerPointcut() {}

    @Pointcut("@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public void writeOperations() {}

    @AfterReturning(pointcut = "controllerPointcut() && writeOperations()", returning = "result")
    public void logWriteOperations(JoinPoint joinPoint, Object result) {
        try {
            String methodName = joinPoint.getSignature().getName();
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String action = className + "." + methodName;

            String username = AuditLogService.currentUsername();

            String ipAddress = "0.0.0.0";
            String requestLine = "";
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
                HttpServletRequest request = attributes.getRequest();
                ipAddress = request.getRemoteAddr();
                requestLine = request.getMethod() + " " + request.getRequestURI();
            }

            // Request bodies are never serialized into the audit trail: they can contain passwords
            // or other sensitive values. Only identifiers are recorded.
            List<Object> identifiers = Arrays.stream(joinPoint.getArgs())
                    .filter(arg -> arg instanceof Long || arg instanceof Integer)
                    .toList();

            String details = "Executed action: " + action
                    + (requestLine.isEmpty() ? "" : " (" + requestLine + ")")
                    + (identifiers.isEmpty() ? "" : " ids=" + identifiers);

            auditLogService.logAction(action, username, details, ipAddress);
        } catch (Exception e) {
            log.error("Failed to write audit log in Aspect: {}", e.getMessage());
        }
    }
}

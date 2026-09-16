package com.sentinelcore.backend.service;

import com.sentinelcore.backend.dto.ChatRequestDTO;
import com.sentinelcore.backend.dto.ChatResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {

    public ChatResponseDTO processQuery(ChatRequestDTO request, String username) {
        String msg = request.getMessage() != null ? request.getMessage().trim().toLowerCase(Locale.ROOT) : "";
        String reply;
        String category = "GENERAL";
        String actionUrl = null;
        String actionText = null;

        if (msg.contains("alert") || msg.contains("threat") || msg.contains("ddos") || msg.contains("ransomware")) {
            category = "ALERTS";
            actionUrl = "/alerts";
            actionText = "View Active Alerts";
            reply = "🛡️ **Security Alert Analysis:**\n\n"
                    + "SentinelCore monitors incoming network traffic and endpoint logs for suspicious patterns.\n\n"
                    + "• **Critical Rule:** When a Critical alert fires, immediately isolate the affected host and preserve memory logs.\n"
                    + "• **Recommended Next Step:** Check the Alerts dashboard for unassigned high-severity incidents.";
        } else if (msg.contains("incident") || msg.contains("breach") || msg.contains("containment")) {
            category = "INCIDENTS";
            actionUrl = "/incidents";
            actionText = "Open Incident Portal";
            reply = "⚡ **Incident Response Protocol:**\n\n"
                    + "1. **Identification**: Verify indicator of compromise (IoC).\n"
                    + "2. **Containment**: Segment network subnets and disable affected user tokens.\n"
                    + "3. **Eradication**: Apply emergency patches and clean malware artifacts.\n"
                    + "4. **Recovery**: Restore system states from verified clean backups.";
        } else if (msg.contains("vulnerab") || msg.contains("cve") || msg.contains("cwe") || msg.contains("cvss")) {
            category = "VULNERABILITIES";
            actionUrl = "/vulnerabilities";
            actionText = "Review Vulnerabilities";
            reply = "🔍 **Vulnerability Management Overview:**\n\n"
                    + "SentinelCore prioritizes CVE vulnerabilities based on CVSS 3.1 score and asset criticality.\n\n"
                    + "• **CVSS 9.0–10.0**: Remediate within 24 hours.\n"
                    + "• **CVSS 7.0–8.9**: Remediate within 7 days.\n"
                    + "• **CVSS 4.0–6.9**: Apply standard monthly patch cycle.";
        } else if (msg.contains("patch") || msg.contains("update") || msg.contains("remediate")) {
            category = "PATCH_MANAGEMENT";
            actionUrl = "/patch-management";
            actionText = "Go to Patch Management";
            reply = "📦 **Patch Management System:**\n\n"
                    + "You can view pending operating system and container security patches in the Patch Management dashboard.\n\n"
                    + "Ensure staging builds pass automated smoke tests before deploying kernel or framework updates to production nodes.";
        } else if (msg.contains("asset") || msg.contains("server") || msg.contains("host") || msg.contains("ip")) {
            category = "ASSETS";
            actionUrl = "/assets";
            actionText = "View Managed Assets";
            reply = "💻 **Asset Inventory:**\n\n"
                    + "Track server nodes, cloud instances, endpoints, and security appliances registered under SentinelCore enterprise governance.";
        } else if (msg.contains("risk") || msg.contains("score") || msg.contains("compliance")) {
            category = "RISK_ASSESSMENT";
            actionUrl = "/risk-assessment";
            actionText = "Check Risk Scores";
            reply = "📊 **Enterprise Risk Assessment:**\n\n"
                    + "SentinelCore calculates systemic risk scores using asset value, threat likelihood, and active vulnerability metrics compliance.";
        } else if (msg.contains("log") || msg.contains("audit") || msg.contains("history")) {
            category = "AUDIT_LOGS";
            actionUrl = "/audit-logs";
            actionText = "Inspect Audit Logs";
            reply = "📜 **Audit Logging System:**\n\n"
                    + "All administrative actions, authentication attempts, role changes, and API calls are immutably logged for compliance (ISO 27001 / SOC 2).";
        } else if (msg.contains("hello") || msg.contains("hi") || msg.contains("hey") || msg.contains("who are you")) {
            category = "GREETING";
            reply = "👋 Hello, " + (username != null ? username : "Agent") + "! I am **Sentinel AI Assistant**.\n\n"
                    + "I can help you analyze security alerts, review incident response procedures, check vulnerability metrics, or navigate SentinelCore platform features. How can I assist you today?";
        } else {
            category = "GENERAL";
            reply = "🤖 **Sentinel AI Assistant Response:**\n\n"
                    + "I have analyzed your request: *\"" + request.getMessage() + "\"*\n\n"
                    + "As your Security Platform Assistant, I recommend reviewing active system alerts or checking vulnerability status. You can ask me specific questions about **alerts**, **incidents**, **vulnerabilities**, **patches**, or **audit compliance**!";
        }

        return ChatResponseDTO.builder()
                .reply(reply)
                .category(category)
                .actionUrl(actionUrl)
                .actionText(actionText)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

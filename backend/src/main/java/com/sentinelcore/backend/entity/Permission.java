package com.sentinelcore.backend.entity;

public enum Permission {
    // User management permissions
    USER_CREATE,
    USER_EDIT,
    USER_DELETE,
    USER_ROLE_ASSIGN,
    USER_PASSWORD_RESET,
    USER_STATUS_TOGGLE,

    // Core asset and security management permissions
    ASSET_MANAGE,
    ALERT_MANAGE,
    INCIDENT_MANAGE,
    VULNERABILITY_MANAGE,
    RISK_MANAGE,
    PATCH_MANAGE,
    REPORT_MANAGE,
    AUDIT_LOG_VIEW,
    SETTINGS_MANAGE,

    // Read only view permissions
    ASSET_VIEW,
    ALERT_VIEW,
    INCIDENT_VIEW,
    VULNERABILITY_VIEW,
    THREAT_INTEL_VIEW,
    RISK_VIEW,
    REPORT_VIEW,
    PROFILE_MANAGE
}

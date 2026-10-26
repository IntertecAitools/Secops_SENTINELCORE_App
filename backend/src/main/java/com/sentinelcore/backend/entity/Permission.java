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
    THREAT_INTEL_MANAGE,
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
    PROFILE_MANAGE,

    // Cloud security permissions (always additionally scoped to the caller's tenant)
    CLOUD_ACCOUNT_VIEW,      // view cloud accounts, inventory, findings and remediation history
    CLOUD_ACCOUNT_MANAGE,    // register / update / verify cloud account connections
    CLOUD_SCAN_RUN,          // trigger read-only assessments
    REMEDIATION_REQUEST,     // propose an allowlisted remediation
    REMEDIATION_APPROVE,     // approve or reject a remediation requested by someone else
    REMEDIATION_EXECUTE      // execute an approved remediation against the provider
}

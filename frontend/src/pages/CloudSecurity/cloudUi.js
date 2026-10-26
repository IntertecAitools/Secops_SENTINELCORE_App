// Shared display helpers for the Cloud Security pages.

export const formatDateTime = (value) => (value ? new Date(value).toLocaleString() : "—");

export const severityColor = (severity) => {
  switch (severity) {
    case "CRITICAL":
    case "HIGH":
      return "error";
    case "MEDIUM":
      return "warning";
    default:
      return "primary";
  }
};

export const connectionColor = (status) => {
  switch (status) {
    case "CONNECTED":
      return "success";
    case "FAILED":
      return "error";
    default:
      return "default";
  }
};

export const scanColor = (status) => {
  switch (status) {
    case "SUCCEEDED":
      return "success";
    case "PARTIAL":
      return "warning";
    case "FAILED":
      return "error";
    default:
      return "info";
  }
};

export const remediationColor = (status) => {
  switch (status) {
    case "VERIFIED":
      return "success";
    case "APPROVED":
    case "SUCCEEDED":
      return "info";
    case "PENDING_APPROVAL":
    case "EXECUTING":
    case "OUTCOME_UNKNOWN":
      return "warning";
    case "FAILED":
    case "VERIFICATION_FAILED":
    case "REJECTED":
      return "error";
    default:
      return "default";
  }
};

export const REMEDIATION_LABELS = {
  S3_BLOCK_PUBLIC_ACCESS: "Enable S3 Block Public Access",
};

// Ordered happy-path lifecycle used for the progress display
export const REMEDIATION_LIFECYCLE = [
  "PENDING_APPROVAL",
  "APPROVED",
  "EXECUTING",
  "SUCCEEDED",
  "VERIFIED",
];

export const bucketName = (resourceId) =>
  resourceId && resourceId.startsWith("arn:aws:s3:::") ? resourceId.substring("arn:aws:s3:::".length) : resourceId;

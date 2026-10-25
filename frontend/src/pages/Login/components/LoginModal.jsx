import { useEffect, useRef } from "react";
import CloseIcon from "@mui/icons-material/Close";

function LoginModal({ type, onClose }) {
  const closeButtonRef = useRef(null);

  useEffect(() => {
    // Focus the close button upon opening for keyboard navigation
    if (closeButtonRef.current) {
      closeButtonRef.current.focus();
    }

    const handleKeyDown = (e) => {
      if (e.key === "Escape") {
        onClose();
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [onClose]);

  if (!type) return null;

  const contentMap = {
    forgot: {
      title: "Password Recovery",
      body: (
        <>
          <p>
            Self-service password recovery is disabled by your workspace security policy to safeguard investigation assets.
          </p>
          <p>To request credential recovery or identity reset:</p>
          <ul>
            <li>Contact your designated workspace security administrator.</li>
            <li>Submit an identity verification ticket with IT Support.</li>
            <li>Direct inquiries to <code>security-admin@sentinelcore.local</code>.</li>
          </ul>
          <p>Temporary reset credentials will be issued upon authorized supervisor verification.</p>
        </>
      ),
    },
    access: {
      title: "Request Workspace Access",
      body: (
        <>
          <p>
            SentinelCore utilizes enterprise Role-Based Access Control (RBAC) across security modules.
          </p>
          <p>To obtain authorized platform clearance:</p>
          <ul>
            <li>Request provisioning from your organization’s SentinelCore Administrator.</li>
            <li>Ensure your work email domain is verified within your organization’s tenant.</li>
            <li>Designate your assigned operational role (e.g., Security Analyst, Auditor).</li>
          </ul>
          <p>Once provisioned, an automated onboarding confirmation will be delivered to your work email.</p>
        </>
      ),
    },
    privacy: {
      title: "Enterprise Privacy Notice",
      body: (
        <>
          <p>
            SentinelCore processes authentication telemetry and session metadata strictly to ensure tenant isolation, access authorization, and platform security.
          </p>
          <p>Key security guarantees:</p>
          <ul>
            <li>Encrypted in transit using TLS 1.3 cryptographic protocols.</li>
            <li>Immutable audit logging for compliance and investigation tracking.</li>
            <li>Zero secondary sharing or unauthorized credential persistence.</li>
          </ul>
        </>
      ),
    },
    terms: {
      title: "Terms of Service",
      body: (
        <>
          <p>
            Use of SentinelCore is restricted to authorized security personnel under organizational enterprise licensing agreements.
          </p>
          <p>Compliance and usage mandates:</p>
          <ul>
            <li>All security actions, threat investigations, and asset queries are audited.</li>
            <li>Sharing credentials or bypassing authentication controls is prohibited.</li>
            <li>Operations must adhere to organizational SecOps standard operating procedures.</li>
          </ul>
        </>
      ),
    },
  };

  const current = contentMap[type] || {
    title: "Information",
    body: <p>Information is unavailable.</p>,
  };

  return (
    <div
      className="login-modal-backdrop"
      onClick={onClose}
      role="presentation"
    >
      <div
        className="login-modal-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="login-modal-title"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="login-modal-header">
          <h2 id="login-modal-title" className="login-modal-title">
            {current.title}
          </h2>
          <button
            ref={closeButtonRef}
            type="button"
            className="login-modal-close"
            onClick={onClose}
            aria-label="Close dialog"
          >
            <CloseIcon sx={{ fontSize: 18 }} />
          </button>
        </div>

        <div className="login-modal-body">{current.body}</div>

        <div className="login-modal-footer">
          <button
            type="button"
            className="login-modal-btn"
            onClick={onClose}
          >
            Understood
          </button>
        </div>
      </div>
    </div>
  );
}

export default LoginModal;

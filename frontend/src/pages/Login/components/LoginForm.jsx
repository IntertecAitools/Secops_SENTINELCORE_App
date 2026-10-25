import { useState, useRef } from "react";
import EmailIcon from "@mui/icons-material/Email";
import ErrorOutlinedIcon from "@mui/icons-material/ErrorOutlined";
import SecurityIcon from "@mui/icons-material/Security";
import InfoIcon from "@mui/icons-material/Info";

import PasswordField from "./PasswordField";
import LoginModal from "./LoginModal";
import { login } from "../../../services/authService";

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function LoginForm({ onLoginSuccess, sessionExpired = false }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const [emailTouched, setEmailTouched] = useState(false);
  const [passwordTouched, setPasswordTouched] = useState(false);

  const [loading, setLoading] = useState(false);
  const [authError, setAuthError] = useState("");
  const [activeModal, setActiveModal] = useState(null);

  const emailInputRef = useRef(null);
  const passwordInputRef = useRef(null);

  // Field validation calculations
  const getEmailError = () => {
    if (!emailTouched) return "";
    const trimmed = email.trim();
    if (!trimmed) return "Work email is required";
    if (!EMAIL_REGEX.test(trimmed)) {
      return "Please enter a valid work email address (e.g. name@company.com)";
    }
    return "";
  };

  const getPasswordError = () => {
    if (!passwordTouched) return "";
    if (!password) return "Password is required";
    if (password.length < 4) return "Password must be at least 4 characters";
    return "";
  };

  const emailError = getEmailError();
  const passwordError = getPasswordError();

  const handleSubmit = async (e) => {
    e.preventDefault();

    // Mark both fields as touched
    setEmailTouched(true);
    setPasswordTouched(true);

    const trimmedEmail = email.trim();
    const hasEmailErr = !trimmedEmail || !EMAIL_REGEX.test(trimmedEmail);
    const hasPassErr = !password || password.length < 4;

    if (hasEmailErr) {
      if (emailInputRef.current) {
        emailInputRef.current.focus();
      }
      return;
    }

    if (hasPassErr) {
      if (passwordInputRef.current) {
        passwordInputRef.current.focus();
      }
      return;
    }

    setLoading(true);
    setAuthError("");

    try {
      // Pass work email as the username parameter to the existing authService contract
      const result = await login(trimmedEmail, password);

      if (result.success) {
        onLoginSuccess(result.role, result.user);
      } else {
        setLoading(false);
        // Generic, safe authentication failure message
        const message =
          result.message ||
          "Invalid work email or password. Please verify your credentials and try again.";
        setAuthError(message);
      }
    } catch {
      setLoading(false);
      setAuthError(
        "An unexpected error occurred during authentication. Please try again later."
      );
    }
  };

  return (
    <>
      <div className="login-card-header">
        <span className="login-eyebrow">Enterprise Access</span>
        <h1 className="login-heading">Welcome back to SentinelCore</h1>
        <p className="login-subheading">
          Sign in to continue your security investigations.
        </p>
      </div>

      {sessionExpired && (
        <div className="login-alert login-alert-warning" role="alert">
          <InfoIcon className="login-alert-icon" />
          <span>Your session has expired. Please sign in again to continue.</span>
        </div>
      )}

      {authError && (
        <div className="login-alert login-alert-error" role="alert">
          <ErrorOutlinedIcon className="login-alert-icon" />
          <span>{authError}</span>
        </div>
      )}

      <form className="login-form" onSubmit={handleSubmit} noValidate>
        {/* Work Email Field */}
        <div className="login-field">
          <label htmlFor="work-email" className="login-label">
            Work email
          </label>
          <div className="login-input-wrapper">
            <span className="login-input-icon" aria-hidden="true">
              <EmailIcon />
            </span>
            <input
              ref={emailInputRef}
              id="work-email"
              name="email"
              type="email"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                if (authError) setAuthError("");
              }}
              onBlur={() => setEmailTouched(true)}
              placeholder="Enter your work email"
              autoComplete="email"
              required
              disabled={loading}
              aria-required="true"
              aria-invalid={Boolean(emailError)}
              aria-describedby={emailError ? "email-error" : undefined}
              className={`login-input ${emailError ? "has-error" : ""}`}
            />
          </div>
          {emailError && (
            <span id="email-error" className="login-field-error" role="alert">
              <ErrorOutlinedIcon sx={{ fontSize: 15 }} aria-hidden="true" />
              <span>{emailError}</span>
            </span>
          )}
        </div>

        {/* Password Field */}
        <PasswordField
          id="password"
          name="password"
          label="Password"
          value={password}
          onChange={(e) => {
            setPassword(e.target.value);
            if (authError) setAuthError("");
          }}
          onBlur={() => setPasswordTouched(true)}
          error={passwordError}
          placeholder="Enter your password"
          disabled={loading}
          required
        />

        {/* Controls Row: Remember device & Forgot password */}
        <div className="login-controls-row">
          <label
            className="login-checkbox-group"
            title="Managed by organizational security policy (standard 24-hour token duration active)"
          >
            <input
              type="checkbox"
              id="remember-device"
              disabled
              checked={false}
              aria-disabled="true"
              aria-label="Remember this device (disabled by policy)"
            />
            <span>Remember this device</span>
            <span className="login-policy-tag" aria-hidden="true">
              24h Policy
            </span>
          </label>

          <button
            type="button"
            className="login-link-btn"
            onClick={() => setActiveModal("forgot")}
            aria-label="Forgot password? Open recovery guidance"
          >
            Forgot password?
          </button>
        </div>

        {/* Primary Sign In Button */}
        <button
          type="submit"
          className="login-submit-btn"
          disabled={loading}
          aria-busy={loading}
        >
          {loading ? (
            <>
              <span className="login-btn-spinner" aria-hidden="true" />
              <span>Signing In...</span>
            </>
          ) : (
            <>
              <span>Sign In Securely</span>
              <span className="login-btn-arrow" aria-hidden="true">
                →
              </span>
            </>
          )}
        </button>

        {/* Access Help Message */}
        <p className="login-access-help">
          Need access?{" "}
          <button
            type="button"
            className="login-link-inline"
            onClick={() => setActiveModal("access")}
          >
            Contact your workspace administrator.
          </button>
        </p>

        {/* Subtle Security Indicator */}
        <div className="login-security-indicator" role="status">
          <SecurityIcon className="login-security-icon" aria-hidden="true" />
          <span>
            Protected by SentinelCore Identity Gateway • 256-bit encrypted session
          </span>
        </div>

        {/* Legal Links */}
        <div className="login-legal-links" aria-label="Legal and privacy links">
          <button
            type="button"
            onClick={() => setActiveModal("privacy")}
            aria-label="View Privacy policy"
          >
            Privacy
          </button>
          <span className="login-legal-divider" aria-hidden="true">
            •
          </span>
          <button
            type="button"
            onClick={() => setActiveModal("terms")}
            aria-label="View Terms of service"
          >
            Terms
          </button>
        </div>
      </form>

      {/* Accessible Modals for Forgot Password, Access Help, Privacy, Terms */}
      <LoginModal
        type={activeModal}
        onClose={() => setActiveModal(null)}
      />
    </>
  );
}

export default LoginForm;

import { useState } from "react";
import LockIcon from "@mui/icons-material/Lock";
import ErrorOutlinedIcon from "@mui/icons-material/ErrorOutlined";

function PasswordField({
  id = "password",
  name = "password",
  label = "Password",
  value = "",
  onChange,
  onBlur,
  error = "",
  placeholder = "Enter your password",
  disabled = false,
  required = true,
  autoComplete = "current-password",
}) {
  const [showPassword, setShowPassword] = useState(false);

  const toggleVisibility = () => {
    setShowPassword((prev) => !prev);
  };

  return (
    <div className="login-field">
      <label htmlFor={id} className="login-label">
        {label}
      </label>

      <div className="login-input-wrapper">
        <span className="login-input-icon" aria-hidden="true">
          <LockIcon />
        </span>

        <input
          id={id}
          name={name}
          type={showPassword ? "text" : "password"}
          value={value}
          onChange={onChange}
          onBlur={onBlur}
          placeholder={placeholder}
          disabled={disabled}
          required={required}
          autoComplete={autoComplete}
          aria-required={required}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? `${id}-error` : undefined}
          className={`login-input login-input-password ${error ? "has-error" : ""}`}
        />

        <button
          type="button"
          onClick={toggleVisibility}
          className="login-password-toggle"
          aria-label={showPassword ? "Hide password" : "Show password"}
          tabIndex={0}
        >
          {showPassword ? "Hide" : "Show"}
        </button>
      </div>

      {error && (
        <span id={`${id}-error`} className="login-field-error" role="alert">
          <ErrorOutlinedIcon sx={{ fontSize: 15 }} aria-hidden="true" />
          <span>{error}</span>
        </span>
      )}
    </div>
  );
}

export default PasswordField;

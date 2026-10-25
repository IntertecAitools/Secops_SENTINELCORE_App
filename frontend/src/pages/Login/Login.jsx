import { useState, useEffect } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import SecurityIcon from "@mui/icons-material/Security";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";

import FibreArc from "../../components/originkit/ui/hero-49/fibre-arc";
import LoginForm from "./components/LoginForm";
import "./login.css";

function Login() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const sessionExpired = searchParams.get("session_expired") === "true";

  // Reduced motion preference detection
  const [reducedMotion, setReducedMotion] = useState(() => {
    if (typeof window !== "undefined" && window.matchMedia) {
      return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    }
    return false;
  });

  useEffect(() => {
    if (typeof window === "undefined" || !window.matchMedia) return;
    const mediaQuery = window.matchMedia("(prefers-reduced-motion: reduce)");
    const handleChange = (e) => setReducedMotion(e.matches);

    mediaQuery.addEventListener("change", handleChange);
    return () => mediaQuery.removeEventListener("change", handleChange);
  }, []);

  const handleLoginSuccess = (role) => {
    // Navigate strictly to authorized destinations matching role permissions
    const redirectUrl =
      {
        SUPER_ADMIN: "/dashboard",
        ADMIN: "/dashboard",
        SECURITY_ANALYST: "/alerts",
        INCIDENT_MANAGER: "/incidents",
        VULNERABILITY_MANAGER: "/vulnerabilities",
        ASSET_MANAGER: "/assets",
        AUDITOR: "/reports",
        USER: "/profile",
      }[role] || "/dashboard";

    navigate(redirectUrl, { replace: true });
  };

  return (
    <div className="login-page">
      {/* Shared Background — exact same WebGL FibreArc component, gradients, and navy tone */}
      <div className="login-bg" aria-hidden="true">
        <FibreArc
          style={{ width: "100%", height: "100%" }}
          speed={reducedMotion ? 0 : 100}
          hover={reducedMotion ? 0 : 200}
        />
      </div>

      <div className="login-content">
        {/* Navigation Header with return link to the landing page */}
        <header className="login-header">
          <div className="login-header-inner">
            <Link to="/" className="login-brand" aria-label="SentinelCore home">
              <SecurityIcon className="login-brand-icon" />
              <span className="login-brand-name">SentinelCore</span>
            </Link>

            <Link
              to="/"
              className="login-back-link"
              aria-label="Return to SentinelCore home"
            >
              <ArrowBackIcon sx={{ fontSize: 17 }} />
              <span>Back to Home</span>
            </Link>
          </div>
        </header>

        {/* Main Content with Split Composition on Desktop */}
        <main className="login-main" id="main-content">
          <div className="login-container">
            {/* Login Panel Card (left/center-left, background light trails visible to the right) */}
            <div className="login-card page-fade-in">
              <LoginForm
                onLoginSuccess={handleLoginSuccess}
                sessionExpired={sessionExpired}
              />
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}

export default Login;
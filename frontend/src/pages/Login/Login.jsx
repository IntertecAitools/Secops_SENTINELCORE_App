import { useState } from "react";
import {
  Container,
  Paper,
  Typography,
  TextField,
  Button,
  Stack,
  CircularProgress,
} from "@mui/material";
import SecurityIcon from "@mui/icons-material/Security";
import { useNavigate } from "react-router-dom";

import { login } from "../../services/authService";
import AppSnackbar from "../../components/common/AppSnackbar";
import RocketLaunchOverlay from "../../components/common/RocketLaunchOverlay";

function Login() {
  const navigate = useNavigate();

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  // Validation States
  const [usernameTouched, setUsernameTouched] = useState(false);
  const [passwordTouched, setPasswordTouched] = useState(false);

  const [loading, setLoading] = useState(false);

  const [launchOverlay, setLaunchOverlay] = useState({
    open: false,
    redirectUrl: "",
    userFullName: "",
  });

  const [snackbar, setSnackbar] = useState({
    open: false,
    message: "",
    severity: "error",
  });

  // Validation helper calculations
  const getUsernameError = () => {
    if (!usernameTouched) return "";
    if (!username.trim()) return "Username is required";
    if (username.trim().length < 3) return "Username must be at least 3 characters";
    return "";
  };

  const getPasswordError = () => {
    if (!passwordTouched) return "";
    if (!password) return "Password is required";
    if (password.length < 4) return "Password must be at least 4 characters";
    return "";
  };

  const isFormInvalid = () => {
    return (
      !username.trim() ||
      username.trim().length < 3 ||
      !password ||
      password.length < 4
    );
  };

  const handleLogin = async () => {
    // Touch all fields to trigger validation display
    setUsernameTouched(true);
    setPasswordTouched(true);

    if (isFormInvalid()) {
      setSnackbar({
        open: true,
        message: "Please fix form errors before logging in",
        severity: "warning",
      });
      return;
    }

    setLoading(true);

    const result = await login(username, password);

    setLoading(false);

    if (result.success) {
      setSnackbar({
        open: true,
        message: "Login Successful - Initializing Launch...",
        severity: "success",
      });

      const redirectUrl = {
        SUPER_ADMIN: "/dashboard",
        ADMIN: "/dashboard",
        SECURITY_ANALYST: "/alerts",
        INCIDENT_MANAGER: "/incidents",
        VULNERABILITY_MANAGER: "/vulnerabilities",
        ASSET_MANAGER: "/assets",
        AUDITOR: "/reports",
        USER: "/profile",
      }[result.role] || "/dashboard";

      // Trigger 3D Rocket Launch Countdown Overlay
      setLaunchOverlay({
        open: true,
        redirectUrl,
        userFullName: result.user?.fullName || username,
      });
    } else {
      setSnackbar({
        open: true,
        message: result.message,
        severity: "error",
      });
    }
  };

  return (
    <Container
      maxWidth="sm"
      sx={{
        display: "flex",
        minHeight: "100vh",
        alignItems: "center",
      }}
    >
      <Paper
        sx={{
          width: "100%",
          p: 5,
          borderRadius: 5,
          border: "1px solid #30363d",
          bgcolor: "#161B22",
        }}
      >
        <Stack spacing={3}>
          <SecurityIcon
            sx={{
              fontSize: 60,
              color: "primary.main",
              mx: "auto",
            }}
          />

          <Typography
            variant="h4"
            align="center"
            fontWeight="bold"
          >
            SentinelCore
          </Typography>

          <Typography
            align="center"
            color="text.secondary"
          >
            Security Operations Platform
          </Typography>

          <TextField
            label="Username or Email"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            onBlur={() => setUsernameTouched(true)}
            error={!!getUsernameError()}
            helperText={getUsernameError()}
            fullWidth
            required
          />

          <TextField
            label="Password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            onBlur={() => setPasswordTouched(true)}
            error={!!getPasswordError()}
            helperText={getPasswordError()}
            fullWidth
            required
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                handleLogin();
              }
            }}
          />

          <Button
            variant="contained"
            size="large"
            onClick={handleLogin}
            disabled={loading}
            sx={{
              fontWeight: 750,
              py: 1.5,
              fontSize: "1.05rem",
            }}
          >
            {loading ? (
              <CircularProgress
                size={24}
                color="inherit"
              />
            ) : (
              "Login"
            )}
          </Button>
        </Stack>
      </Paper>

      <AppSnackbar
        open={snackbar.open}
        message={snackbar.message}
        severity={snackbar.severity}
        handleClose={() =>
          setSnackbar({
            ...snackbar,
            open: false,
          })
        }
      />

      <RocketLaunchOverlay
        open={launchOverlay.open}
        userFullName={launchOverlay.userFullName}
        onComplete={() => {
          navigate(launchOverlay.redirectUrl);
        }}
      />
    </Container>
  );
}

export default Login;
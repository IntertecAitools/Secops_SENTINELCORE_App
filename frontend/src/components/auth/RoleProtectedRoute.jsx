import { Navigate } from "react-router-dom";
import { Box, Typography, Button, Container } from "@mui/material";
import LockPatternIcon from "@mui/icons-material/GppBad";
import { Link } from "react-router-dom";
import { isAuthenticated, getUserRole } from "../../services/authService";

function RoleProtectedRoute({ children, allowedRoles = [] }) {
  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }

  const rawRole = getUserRole();
  const userRole = rawRole ? rawRole.replace("ROLE_", "").trim().toUpperCase() : "";

  // Super Admin always bypasses page level role restrictions
  if (userRole === "SUPER_ADMIN") {
    return children;
  }

  const cleanAllowedRoles = allowedRoles.map((r) =>
    r.replace("ROLE_", "").trim().toUpperCase()
  );

  if (cleanAllowedRoles.length > 0 && !cleanAllowedRoles.includes(userRole)) {
    return (
      <Container
        maxWidth="md"
        sx={{
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
          minHeight: "80vh",
          textAlign: "center",
        }}
      >
        <LockPatternIcon sx={{ fontSize: 80, color: "error.main", mb: 3 }} />
        <Typography variant="h3" fontWeight={800} gutterBottom>
          403 - Access Denied
        </Typography>
        <Typography variant="h6" color="text.secondary" paragraph sx={{ maxWidth: 500 }}>
          Your security role <b>({rawRole || "NONE"})</b> does not possess the permissions required to view this module.
        </Typography>
        <Button variant="contained" component={Link} to="/" sx={{ mt: 2 }}>
          Return to Portal
        </Button>
      </Container>
    );
  }

  return children;
}

export default RoleProtectedRoute;

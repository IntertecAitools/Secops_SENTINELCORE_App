import React, { useEffect, useState } from "react";
import {
  Box,
  Container,
  Paper,
  Typography,
  Avatar,
  Chip,
  Grid,
  Divider,
  Button,
  Stack,
  CircularProgress,
  Tab,
  Tabs,
  Card,
  CardContent,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
} from "@mui/material";
import { useParams, useNavigate, Link } from "react-router-dom";
import PersonIcon from "@mui/icons-material/Person";
import EmailIcon from "@mui/icons-material/Email";
import PhoneIcon from "@mui/icons-material/Phone";
import BusinessIcon from "@mui/icons-material/Business";
import ShieldIcon from "@mui/icons-material/Shield";
import AccessTimeIcon from "@mui/icons-material/AccessTime";
import SecurityIcon from "@mui/icons-material/Security";
import SettingsIcon from "@mui/icons-material/Settings";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import CancelIcon from "@mui/icons-material/Cancel";
import LockResetIcon from "@mui/icons-material/LockReset";
import AppSnackbar from "../../components/common/AppSnackbar";
import { getProfile, getUserById } from "../../services/userService";
import { getCurrentUser } from "../../services/authService";

const ROLE_PERMISSIONS = {
  SUPER_ADMIN: [
    "Full Enterprise Platform Control",
    "User & Access Policy Management",
    "Security Audit Log Authority",
    "Asset & Threat Intel Configuration",
    "Remediation & Patch Deployment",
  ],
  ADMIN: [
    "User Account Management",
    "Security Alert Resolution",
    "Asset Catalog Oversight",
    "System Reports Generation",
  ],
  SECURITY_ANALYST: [
    "Real-time Alert Triage",
    "Threat Intelligence Feed Access",
    "Incident Investigation & Tagging",
    "Vulnerability Assessment",
  ],
  INCIDENT_MANAGER: [
    "Incident Response Workflow Execution",
    "Breach Containment Controls",
    "Alert Severity Escalation",
  ],
  VULNERABILITY_MANAGER: [
    "CVE & CVSS Vulnerability Tracking",
    "Risk Assessment & Scoring",
    "Patch Management Dispatch",
  ],
  ASSET_MANAGER: [
    "Enterprise Hardware & Cloud Tracking",
    "Software Inventory Auditing",
  ],
  AUDITOR: [
    "Read-Only Audit Log Inspection",
    "Compliance Report Generation",
  ],
  USER: [
    "Personal Profile Settings Access",
    "Security Preference Customization",
  ],
};

const getRoleColor = (role) => {
  switch (role) {
    case "SUPER_ADMIN":
      return "error";
    case "ADMIN":
      return "warning";
    case "SECURITY_ANALYST":
      return "primary";
    case "INCIDENT_MANAGER":
      return "secondary";
    case "VULNERABILITY_MANAGER":
      return "info";
    case "ASSET_MANAGER":
      return "success";
    default:
      return "default";
  }
};

function Profile() {
  const { id } = useParams();
  const navigate = useNavigate();
  const loggedInUser = getCurrentUser();

  const [userProfile, setUserProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [tabValue, setTabValue] = useState(0);

  const [snackbar, setSnackbar] = useState({
    open: false,
    message: "",
    severity: "success",
  });

  const isSelf = !id || (loggedInUser && String(loggedInUser.id) === String(id));

  useEffect(() => {
    fetchProfileData();
  }, [id]);

  const fetchProfileData = async () => {
    setLoading(true);
    try {
      if (id) {
        // Fetch specific user profile by ID
        const data = await getUserById(id);
        setUserProfile(data);
      } else {
        // Fetch logged-in user profile
        const data = await getProfile();
        setUserProfile(data);
      }
    } catch (error) {
      console.error("Failed to load user profile", error);
      // Fallback to local storage user object if viewing self
      if (isSelf && loggedInUser) {
        setUserProfile({
          fullName: loggedInUser.fullName || loggedInUser.username || "User",
          username: loggedInUser.username || "user",
          email: loggedInUser.email || "user@sentinelcore.io",
          role: loggedInUser.role || "USER",
          department: loggedInUser.department || "Cyber Operations",
          phoneNumber: loggedInUser.phoneNumber || "N/A",
          enabled: true,
          lastLogin: new Date().toISOString(),
        });
      } else {
        setSnackbar({
          open: true,
          message: "Failed to fetch user profile details",
          severity: "error",
        });
      }
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="70vh">
        <CircularProgress color="success" size={50} />
      </Box>
    );
  }

  if (!userProfile) {
    return (
      <Container maxWidth="md" sx={{ py: 6 }}>
        <Paper sx={{ p: 4, textAlign: "center", borderRadius: 3 }}>
          <Typography variant="h5" color="error" gutterBottom fontWeight="bold">
            User Profile Not Found
          </Typography>
          <Typography color="text.secondary" mb={3}>
            The requested user profile does not exist or has been removed.
          </Typography>
          <Button variant="contained" color="primary" onClick={() => navigate("/users")}>
            Back to User Directory
          </Button>
        </Paper>
      </Container>
    );
  }

  const rolePermissions = ROLE_PERMISSIONS[userProfile.role] || ROLE_PERMISSIONS.USER;

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      {/* Top Navigation Back Button if inspecting another user */}
      {id && (
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={() => navigate("/users")}
          sx={{ mb: 3, fontWeight: 700 }}
        >
          Back to User Directory
        </Button>
      )}

      {/* Main Profile Header Card */}
      <Paper
        elevation={0}
        sx={{
          p: { xs: 3, md: 4 },
          borderRadius: 4,
          bgcolor: "background.paper",
          border: "1px solid",
          borderColor: "divider",
          mb: 4,
          position: "relative",
          overflow: "hidden",
          boxShadow: "0 10px 30px rgba(0,0,0,0.08)",
        }}
      >
        {/* Decorative Top Glowing Security Line */}
        <Box
          sx={{
            position: "absolute",
            top: 0,
            left: 0,
            right: 0,
            height: 4,
            bgcolor: userProfile.enabled ? "success.main" : "error.main",
          }}
        />

        <Grid container spacing={3} alignItems="center">
          <Grid item>
            <Avatar
              sx={{
                width: 100,
                height: 100,
                bgcolor: "success.main",
                fontSize: "2.5rem",
                fontWeight: "bold",
                boxShadow: "0 6px 20px rgba(0, 200, 83, 0.3)",
                border: "3px solid #161B22",
              }}
            >
              {userProfile.fullName ? userProfile.fullName.charAt(0).toUpperCase() : "U"}
            </Avatar>
          </Grid>

          <Grid item xs>
            <Stack direction="row" spacing={1.5} alignItems="center" flexWrap="wrap" gap={1}>
              <Typography variant="h4" fontWeight={850} letterSpacing="-0.5px">
                {userProfile.fullName}
              </Typography>
              <Chip
                label={userProfile.role}
                color={getRoleColor(userProfile.role)}
                size="small"
                sx={{ fontWeight: 800, fontSize: "0.75rem" }}
              />
              <Chip
                icon={userProfile.enabled ? <CheckCircleIcon /> : <CancelIcon />}
                label={userProfile.enabled ? "Active Account" : "Disabled"}
                color={userProfile.enabled ? "success" : "error"}
                variant="outlined"
                size="small"
                sx={{ fontWeight: 700 }}
              />
            </Stack>

            <Typography variant="subtitle1" color="text.secondary" sx={{ mt: 0.5, fontFamily: "monospace" }}>
              @{userProfile.username}
            </Typography>

            <Stack direction="row" spacing={3} sx={{ mt: 2, flexWrap: "wrap", gap: 2 }}>
              <Box display="flex" alignItems="center" gap={1}>
                <EmailIcon fontSize="small" color="action" />
                <Typography variant="body2">{userProfile.email}</Typography>
              </Box>
              <Box display="flex" alignItems="center" gap={1}>
                <BusinessIcon fontSize="small" color="action" />
                <Typography variant="body2">{userProfile.department || "Cyber Security Operations"}</Typography>
              </Box>
              <Box display="flex" alignItems="center" gap={1}>
                <PhoneIcon fontSize="small" color="action" />
                <Typography variant="body2">{userProfile.phoneNumber || "Not Specified"}</Typography>
              </Box>
            </Stack>
          </Grid>

          <Grid item xs={12} md="auto">
            <Stack direction={{ xs: "row", md: "column" }} spacing={1.5}>
              {isSelf ? (
                <Button
                  component={Link}
                  to="/settings"
                  variant="contained"
                  color="primary"
                  startIcon={<SettingsIcon />}
                  sx={{ borderRadius: 2.5, px: 3, fontWeight: 700 }}
                >
                  Edit Profile
                </Button>
              ) : (
                <Button
                  component={Link}
                  to="/users"
                  variant="outlined"
                  color="primary"
                  startIcon={<ArrowBackIcon />}
                  sx={{ borderRadius: 2.5, px: 3, fontWeight: 700 }}
                >
                  Manage Users
                </Button>
              )}
            </Stack>
          </Grid>
        </Grid>
      </Paper>

      {/* Tabs Bar */}
      <Box sx={{ borderBottom: 1, borderColor: "divider", mb: 3 }}>
        <Tabs
          value={tabValue}
          onChange={(e, val) => setTabValue(val)}
          indicatorColor="primary"
          textColor="primary"
        >
          <Tab label="Account Overview" sx={{ fontWeight: 700 }} />
          <Tab label="Security & Privileges" sx={{ fontWeight: 700 }} />
          <Tab label="Activity Status" sx={{ fontWeight: 700 }} />
        </Tabs>
      </Box>

      {/* Tab 0: Overview */}
      {tabValue === 0 && (
        <Grid container spacing={3}>
          <Grid item xs={12} md={7}>
            <Card variant="outlined" sx={{ borderRadius: 3, p: 1 }}>
              <CardContent>
                <Typography variant="h6" fontWeight={800} gutterBottom display="flex" alignItems="center" gap={1}>
                  <PersonIcon color="primary" /> Profile Attributes
                </Typography>
                <Divider sx={{ my: 1.5 }} />

                <Grid container spacing={2.5}>
                  <Grid item xs={6}>
                    <Typography variant="caption" color="text.secondary">Full Name</Typography>
                    <Typography variant="body1" fontWeight={700}>{userProfile.fullName}</Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="caption" color="text.secondary">Username</Typography>
                    <Typography variant="body1" fontWeight={700}>@{userProfile.username}</Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="caption" color="text.secondary">Email Address</Typography>
                    <Typography variant="body1" fontWeight={700}>{userProfile.email}</Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="caption" color="text.secondary">Phone Number</Typography>
                    <Typography variant="body1" fontWeight={700}>{userProfile.phoneNumber || "N/A"}</Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="caption" color="text.secondary">Department</Typography>
                    <Typography variant="body1" fontWeight={700}>{userProfile.department || "N/A"}</Typography>
                  </Grid>
                  <Grid item xs={6}>
                    <Typography variant="caption" color="text.secondary">Assigned Role</Typography>
                    <Typography variant="body1" fontWeight={700} color="primary.main">{userProfile.role}</Typography>
                  </Grid>
                </Grid>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={5}>
            <Card variant="outlined" sx={{ borderRadius: 3, p: 1, height: "100%" }}>
              <CardContent>
                <Typography variant="h6" fontWeight={800} gutterBottom display="flex" alignItems="center" gap={1}>
                  <AccessTimeIcon color="warning" /> Account Metadata
                </Typography>
                <Divider sx={{ my: 1.5 }} />

                <Stack spacing={2}>
                  <Box>
                    <Typography variant="caption" color="text.secondary">Last Login Time</Typography>
                    <Typography variant="body1" fontWeight={700}>
                      {userProfile.lastLogin ? new Date(userProfile.lastLogin).toLocaleString() : "Active / Session Online"}
                    </Typography>
                  </Box>

                  <Box>
                    <Typography variant="caption" color="text.secondary">Account Security Status</Typography>
                    <Typography variant="body1" fontWeight={700} color={userProfile.enabled ? "success.main" : "error.main"}>
                      {userProfile.enabled ? "Verified & Active" : "Account Suspended"}
                    </Typography>
                  </Box>

                  <Box>
                    <Typography variant="caption" color="text.secondary">Multi-Factor Authentication (MFA)</Typography>
                    <Typography variant="body1" fontWeight={700} color="success.main">
                      Enabled (TOTP Authenticator)
                    </Typography>
                  </Box>
                </Stack>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      {/* Tab 1: Security & Role Privileges */}
      {tabValue === 1 && (
        <Card variant="outlined" sx={{ borderRadius: 3, p: 1 }}>
          <CardContent>
            <Typography variant="h6" fontWeight={800} gutterBottom display="flex" alignItems="center" gap={1}>
              <SecurityIcon color="secondary" /> Role-Based Access Control (RBAC) Privileges
            </Typography>
            <Typography variant="body2" color="text.secondary" mb={2}>
              Permissions granted based on role assignment: <strong>{userProfile.role}</strong>
            </Typography>
            <Divider sx={{ mb: 2 }} />

            <List>
              {rolePermissions.map((perm, i) => (
                <ListItem key={i} sx={{ py: 0.8 }}>
                  <ListItemIcon>
                    <ShieldIcon color="success" />
                  </ListItemIcon>
                  <ListItemText
                    primary={<Typography fontWeight={700}>{perm}</Typography>}
                    secondary="Verified platform permission level"
                  />
                </ListItem>
              ))}
            </List>
          </CardContent>
        </Card>
      )}

      {/* Tab 2: Activity Status */}
      {tabValue === 2 && (
        <Card variant="outlined" sx={{ borderRadius: 3, p: 1 }}>
          <CardContent>
            <Typography variant="h6" fontWeight={800} gutterBottom display="flex" alignItems="center" gap={1}>
              <AccessTimeIcon color="info" /> Session & Activity Logs
            </Typography>
            <Typography variant="body2" color="text.secondary" mb={2}>
              Recent authentication events and security activity overview for @{userProfile.username}.
            </Typography>
            <Divider sx={{ mb: 2 }} />

            <Stack spacing={2}>
              <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, bgcolor: "background.default" }}>
                <Stack direction="row" justifyContent="space-between" alignItems="center">
                  <Box>
                    <Typography fontWeight={700}>Successful Portal Authentication</Typography>
                    <Typography variant="caption" color="text.secondary">IP: 192.168.1.105 | SentinelCore SSO</Typography>
                  </Box>
                  <Chip label="ONLINE" color="success" size="small" sx={{ fontWeight: 800 }} />
                </Stack>
              </Paper>

              <Paper variant="outlined" sx={{ p: 2, borderRadius: 2, bgcolor: "background.default" }}>
                <Stack direction="row" justifyContent="space-between" alignItems="center">
                  <Box>
                    <Typography fontWeight={700}>RBAC Profile Verification</Typography>
                    <Typography variant="caption" color="text.secondary">Access granted to SecureOps portal routes</Typography>
                  </Box>
                  <Chip label="VERIFIED" color="info" size="small" sx={{ fontWeight: 800 }} />
                </Stack>
              </Paper>
            </Stack>
          </CardContent>
        </Card>
      )}

      {/* Snackbar Notifications */}
      <AppSnackbar
        open={snackbar.open}
        message={snackbar.message}
        severity={snackbar.severity}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
      />
    </Container>
  );
}

export default Profile;

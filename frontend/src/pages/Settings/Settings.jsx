import { useState, useEffect } from "react";
import {
  Box,
  Paper,
  Typography,
  TextField,
  Button,
  Stack,
  Switch,
  FormControlLabel,
  Grid,
  Avatar,
  IconButton,
  CircularProgress,
} from "@mui/material";

import { getProfile, updateProfile, changeOwnPassword } from "../../services/userService";
import { useThemeContext } from "../../context/ThemeContext";
import AppSnackbar from "../../components/common/AppSnackbar";

import LockIcon from "@mui/icons-material/Lock";
import PersonIcon from "@mui/icons-material/Person";
import SettingsIcon from "@mui/icons-material/Settings";
import PhotoCameraIcon from "@mui/icons-material/PhotoCamera";

function Settings() {
  const { mode, toggleTheme } = useThemeContext();

  const [formValues, setFormValues] = useState({
    fullName: "",
    username: "",
    email: "",
    role: "",
    department: "",
    phoneNumber: "",
    profilePicture: "",
  });

  const [loading, setLoading] = useState(true);

  // Password change state
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  // Notification toggle states
  const [alertNotify, setAlertNotify] = useState(true);
  const [incidentNotify, setIncidentNotify] = useState(true);

  const [snackbar, setSnackbar] = useState({
    open: false,
    message: "",
    severity: "success",
  });

  useEffect(() => {
    loadProfile();
  }, []);

  const loadProfile = async () => {
    try {
      const data = await getProfile();
      if (data) {
        setFormValues({
          fullName: data.fullName || "",
          username: data.username || "",
          email: data.email || "",
          role: data.role || "",
          department: data.department || "",
          phoneNumber: data.phoneNumber || "",
          profilePicture: data.profilePicture || "",
        });
      }
    } catch (error) {
      console.error(error);
      showSnackbar("Failed to load profile details", "error");
    } finally {
      setLoading(false);
    }
  };

  const showSnackbar = (message, severity = "success") => {
    setSnackbar({ open: true, message, severity });
  };

  const handleUpdateProfile = async (e) => {
    e.preventDefault();
    if (!formValues.fullName || !formValues.email) {
      showSnackbar("Full Name and Email are required", "warning");
      return;
    }

    try {
      await updateProfile({
        fullName: formValues.fullName,
        email: formValues.email,
        phoneNumber: formValues.phoneNumber,
        department: formValues.department,
        profilePicture: formValues.profilePicture,
      });
      showSnackbar("Profile updated successfully!");
    } catch (error) {
      console.error(error);
      showSnackbar("Failed to update profile", "error");
    }
  };

  const handleChangePassword = async (e) => {
    e.preventDefault();
    if (!currentPassword || !newPassword) {
      showSnackbar("All password fields are required", "warning");
      return;
    }
    if (newPassword !== confirmPassword) {
      showSnackbar("New passwords do not match", "warning");
      return;
    }
    if (newPassword.length < 4) {
      showSnackbar("Password must be at least 4 characters", "warning");
      return;
    }

    try {
      await changeOwnPassword(currentPassword, newPassword);
      showSnackbar("Password updated successfully!");
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
    } catch (error) {
      console.error(error);
      showSnackbar(error.response?.data?.message || "Failed to update password", "error");
    }
  };

  const handleProfilePictureUpload = (e) => {
    const file = e.target.files[0];
    if (file) {
      if (file.size > 2 * 1024 * 1024) {
        showSnackbar("Image file size must be less than 2MB", "warning");
        return;
      }

      const reader = new FileReader();
      reader.onloadend = () => {
        setFormValues({ ...formValues, profilePicture: reader.result });
      };
      reader.readAsDataURL(file);
    }
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" py={8}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box p={4}>
      <Typography variant="h4" fontWeight={800} mb={4}>
        System & User Profile Settings
      </Typography>

      <Grid container spacing={4}>
        {/* Profile Card */}
        <Grid size={{ xs: 12, md: 6 }}>
          <Paper sx={{ p: 4, borderRadius: 4, height: "100%" }} elevation={0}>
            <Stack direction="row" spacing={1.5} alignItems="center" mb={3}>
              <PersonIcon color="primary" />
              <Typography variant="h6" fontWeight={700}>
                User Profile Information
              </Typography>
            </Stack>

            <Box component="form" onSubmit={handleUpdateProfile}>
              <Stack spacing={3} alignItems="center" mb={3}>
                <Box position="relative">
                  <Avatar
                    src={formValues.profilePicture || ""}
                    sx={{ width: 100, height: 100, border: "2px solid #00C853" }}
                  >
                    {formValues.fullName?.charAt(0) || "U"}
                  </Avatar>
                  <IconButton
                    component="label"
                    sx={{
                      position: "absolute",
                      bottom: 0,
                      right: 0,
                      bgcolor: "primary.main",
                      color: "white",
                      "&:hover": { bgcolor: "primary.dark" },
                      width: 32,
                      height: 32,
                    }}
                  >
                    <input
                      type="file"
                      accept="image/*"
                      hidden
                      onChange={handleProfilePictureUpload}
                    />
                    <PhotoCameraIcon sx={{ fontSize: 16 }} />
                  </IconButton>
                </Box>
              </Stack>

              <Stack spacing={3}>
                <TextField
                  label="Full Name"
                  value={formValues.fullName}
                  onChange={(e) => setFormValues({ ...formValues, fullName: e.target.value })}
                  fullWidth
                  required
                />
                <TextField
                  label="Username"
                  value={formValues.username}
                  disabled
                  fullWidth
                  helperText="Usernames cannot be changed."
                />
                <TextField
                  label="Email Address"
                  value={formValues.email}
                  onChange={(e) => setFormValues({ ...formValues, email: e.target.value })}
                  fullWidth
                  required
                />
                <TextField
                  label="Department"
                  value={formValues.department}
                  onChange={(e) => setFormValues({ ...formValues, department: e.target.value })}
                  fullWidth
                />
                <TextField
                  label="Phone Number"
                  value={formValues.phoneNumber}
                  onChange={(e) => setFormValues({ ...formValues, phoneNumber: e.target.value })}
                  fullWidth
                />
                <TextField
                  label="Assigned System Role"
                  value={formValues.role}
                  disabled
                  fullWidth
                />
                <Button type="submit" variant="contained" color="primary" sx={{ py: 1.2 }}>
                  Save Profile Changes
                </Button>
              </Stack>
            </Box>
          </Paper>
        </Grid>

        {/* Password Card */}
        <Grid size={{ xs: 12, md: 6 }}>
          <Paper sx={{ p: 4, borderRadius: 4, height: "100%" }} elevation={0}>
            <Stack direction="row" spacing={1.5} alignItems="center" mb={3}>
              <LockIcon color="primary" />
              <Typography variant="h6" fontWeight={700}>
                Change Account Password
              </Typography>
            </Stack>

            <Box component="form" onSubmit={handleChangePassword}>
              <Stack spacing={3}>
                <TextField
                  label="Current Password"
                  type="password"
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                  fullWidth
                  required
                />
                <TextField
                  label="New Password"
                  type="password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  fullWidth
                  required
                />
                <TextField
                  label="Confirm New Password"
                  type="password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  fullWidth
                  required
                />
                <Button type="submit" variant="contained" color="primary" sx={{ py: 1.2 }}>
                  Update Password
                </Button>
              </Stack>
            </Box>
          </Paper>
        </Grid>

        {/* Global Settings */}
        <Grid size={12}>
          <Paper sx={{ p: 4, borderRadius: 4 }} elevation={0}>
            <Stack direction="row" spacing={1.5} alignItems="center" mb={3}>
              <SettingsIcon color="primary" />
              <Typography variant="h6" fontWeight={700}>
                Global Application Settings
              </Typography>
            </Stack>

            <Grid container spacing={3}>
              <Grid size={{ xs: 12, md: 4 }}>
                <Typography variant="subtitle1" fontWeight={700} mb={1}>
                  Theme Preferences
                </Typography>
                <FormControlLabel
                  control={<Switch checked={mode === "dark"} onChange={toggleTheme} />}
                  label={mode === "dark" ? "Dark Theme Mode Enabled" : "Light Theme Mode Enabled"}
                />
              </Grid>

              <Grid size={{ xs: 12, md: 8 }}>
                <Typography variant="subtitle1" fontWeight={700} mb={1}>
                  Email Notifications
                </Typography>
                <Stack spacing={1}>
                  <FormControlLabel
                    control={<Switch checked={alertNotify} onChange={(e) => setAlertNotify(e.target.checked)} />}
                    label="Send email alerts on Critical/High severity threats"
                  />
                  <FormControlLabel
                    control={<Switch checked={incidentNotify} onChange={(e) => setIncidentNotify(e.target.checked)} />}
                    label="Send email updates on ticket assignment modifications"
                  />
                </Stack>
              </Grid>
            </Grid>
          </Paper>
        </Grid>
      </Grid>

      <AppSnackbar
        open={snackbar.open}
        message={snackbar.message}
        severity={snackbar.severity}
        handleClose={() => setSnackbar({ ...snackbar, open: false })}
      />
    </Box>
  );
}

export default Settings;
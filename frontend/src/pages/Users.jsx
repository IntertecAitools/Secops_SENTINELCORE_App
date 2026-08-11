import { useEffect, useState } from "react";
import {
  Box,
  Typography,
  Paper,
  Table,
  TableHead,
  TableBody,
  TableRow,
  TableCell,
  CircularProgress,
  Chip,
  IconButton,
  Tooltip,
  Button,
  TextField,
  MenuItem,
  Stack,
  TablePagination,
  Switch,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  InputAdornment,
} from "@mui/material";

import {
  getUsers,
  createUser,
  updateUser,
  deleteUser,
  toggleUserStatus,
  resetUserPassword,
} from "../services/userService";

import { useNavigate } from "react-router-dom";
import DeleteIcon from "@mui/icons-material/Delete";
import EditIcon from "@mui/icons-material/Edit";
import AddIcon from "@mui/icons-material/Add";
import LockResetIcon from "@mui/icons-material/LockReset";
import SearchIcon from "@mui/icons-material/Search";
import VisibilityIcon from "@mui/icons-material/Visibility";
import AppSnackbar from "../components/common/AppSnackbar";

const ROLES = [
  "SUPER_ADMIN",
  "ADMIN",
  "SECURITY_ANALYST",
  "INCIDENT_MANAGER",
  "VULNERABILITY_MANAGER",
  "ASSET_MANAGER",
  "AUDITOR",
  "USER"
];

function Users() {
  const navigate = useNavigate();
  const [users, setUsers] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);

  // Filter & Search states
  const [search, setSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");

  // Pagination states
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);

  // Dialog states
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [resetPassUser, setResetPassUser] = useState(null);
  const [newPassword, setNewPassword] = useState("");

  // Form states
  const [formValues, setFormValues] = useState({
    fullName: "",
    username: "",
    email: "",
    password: "",
    role: "USER",
    department: "",
    phoneNumber: "",
    enabled: true,
  });

  const [snackbar, setSnackbar] = useState({
    open: false,
    message: "",
    severity: "success",
  });

  useEffect(() => {
    loadUsers();
  }, [search, roleFilter, statusFilter, page, rowsPerPage]);

  const loadUsers = async () => {
    setLoading(true);
    try {
      const data = await getUsers(
        search,
        roleFilter,
        statusFilter,
        page,
        rowsPerPage,
        "username",
        "asc"
      );
      setUsers(data.content || []);
      setTotalElements(data.totalElements || 0);
    } catch (error) {
      console.error(error);
      showSnackbar("Failed to fetch users", "error");
    } finally {
      setLoading(false);
    }
  };

  const showSnackbar = (message, severity = "success") => {
    setSnackbar({ open: true, message, severity });
  };

  const handleOpenCreate = () => {
    setEditingUser(null);
    setFormValues({
      fullName: "",
      username: "",
      email: "",
      password: "",
      role: "USER",
      department: "",
      phoneNumber: "",
      enabled: true,
    });
    setDialogOpen(true);
  };

  const handleOpenEdit = (user) => {
    setEditingUser(user);
    setFormValues({
      fullName: user.fullName || "",
      username: user.username || "",
      email: user.email || "",
      password: "",
      role: user.role || "USER",
      department: user.department || "",
      phoneNumber: user.phoneNumber || "",
      enabled: user.enabled ?? true,
    });
    setDialogOpen(true);
  };

  const handleSaveUser = async () => {
    if (!formValues.fullName || !formValues.email || !formValues.username) {
      showSnackbar("Full Name, Username, and Email are required", "warning");
      return;
    }

    try {
      if (editingUser) {
        // Edit flow
        await updateUser(editingUser.id, {
          fullName: formValues.fullName,
          email: formValues.email,
          role: formValues.role,
          department: formValues.department,
          phoneNumber: formValues.phoneNumber,
          enabled: formValues.enabled,
        });
        showSnackbar("User updated successfully");
      } else {
        // Create flow
        if (!formValues.password) {
          showSnackbar("Password is required for new users", "warning");
          return;
        }
        await createUser(formValues);
        showSnackbar("User created successfully");
      }
      setDialogOpen(false);
      loadUsers();
    } catch (error) {
      console.error(error);
      showSnackbar(error.response?.data?.message || "Operation failed", "error");
    }
  };

  const handleDeleteUser = async (id) => {
    if (!window.confirm("Are you sure you want to delete this user?")) return;
    try {
      await deleteUser(id);
      showSnackbar("User deleted successfully");
      loadUsers();
    } catch (error) {
      console.error(error);
      showSnackbar("Failed to delete user", "error");
    }
  };

  const handleToggleStatus = async (user) => {
    try {
      await toggleUserStatus(user.id);
      showSnackbar(`User status toggled successfully`);
      loadUsers();
    } catch (error) {
      console.error(error);
      showSnackbar("Failed to toggle status", "error");
    }
  };

  const handleOpenResetPassword = (user) => {
    setResetPassUser(user);
    setNewPassword("");
  };

  const handleSaveResetPassword = async () => {
    if (!newPassword || newPassword.trim().length < 4) {
      showSnackbar("Password must be at least 4 characters", "warning");
      return;
    }
    try {
      await resetUserPassword(resetPassUser.id, newPassword);
      showSnackbar("Password reset successfully");
      setResetPassUser(null);
    } catch (error) {
      console.error(error);
      showSnackbar("Failed to reset password", "error");
    }
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

  return (
    <Box p={4}>
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={4}>
        <Box>
          <Typography variant="h4" fontWeight={800} gutterBottom>
            User Management Panel
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Manage corporate security accounts, assign roles, reset credentials, and audit access.
          </Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={handleOpenCreate}
          sx={{ py: 1.2, px: 3, fontWeight: "bold" }}
        >
          Add New User
        </Button>
      </Stack>

      {/* FILTER PANEL */}
      <Paper sx={{ p: 3, mb: 4, borderRadius: 3 }}>
        <Stack direction={{ xs: "column", md: "row" }} spacing={2}>
          <TextField
            label="Search Users"
            variant="outlined"
            size="small"
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
            fullWidth
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon fontSize="small" />
                </InputAdornment>
              ),
            }}
          />
          <TextField
            select
            label="Filter Role"
            value={roleFilter}
            onChange={(e) => {
              setRoleFilter(e.target.value);
              setPage(0);
            }}
            size="small"
            sx={{ minWidth: 200 }}
          >
            <MenuItem value="">All Roles</MenuItem>
            {ROLES.map((r) => (
              <MenuItem key={r} value={r}>
                {r}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            select
            label="Filter Status"
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setPage(0);
            }}
            size="small"
            sx={{ minWidth: 160 }}
          >
            <MenuItem value="">All Statuses</MenuItem>
            <MenuItem value="active">Active</MenuItem>
            <MenuItem value="disabled">Disabled</MenuItem>
          </TextField>
        </Stack>
      </Paper>

      {/* DATA TABLE */}
      <Paper sx={{ borderRadius: 3, overflow: "hidden" }}>
        {loading ? (
          <Box display="flex" justifyContent="center" py={8}>
            <CircularProgress />
          </Box>
        ) : (
          <>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell><b>Full Name</b></TableCell>
                  <TableCell><b>Username</b></TableCell>
                  <TableCell><b>Email</b></TableCell>
                  <TableCell><b>Role</b></TableCell>
                  <TableCell><b>Department</b></TableCell>
                  <TableCell><b>Last Login</b></TableCell>
                  <TableCell><b>Status</b></TableCell>
                  <TableCell align="center"><b>Actions</b></TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {users.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={8} align="center" sx={{ py: 6 }}>
                      No users found.
                    </TableCell>
                  </TableRow>
                ) : (
                  users.map((user) => (
                    <TableRow key={user.id} hover>
                      <TableCell
                        onClick={() => navigate(`/profile/${user.id}`)}
                        sx={{
                          cursor: "pointer",
                          fontWeight: 700,
                          color: "primary.main",
                          "&:hover": { textDecoration: "underline" },
                        }}
                      >
                        {user.fullName}
                      </TableCell>
                      <TableCell>{user.username}</TableCell>
                      <TableCell>{user.email}</TableCell>
                      <TableCell>
                        <Chip
                          label={user.role}
                          color={getRoleColor(user.role)}
                          size="small"
                          sx={{ fontWeight: "bold" }}
                        />
                      </TableCell>
                      <TableCell>{user.department || "N/A"}</TableCell>
                      <TableCell>
                        {user.lastLogin
                          ? new Date(user.lastLogin).toLocaleString()
                          : "Never"}
                      </TableCell>
                      <TableCell>
                        <Switch
                          checked={user.enabled ?? true}
                          onChange={() => handleToggleStatus(user)}
                          color="success"
                          size="small"
                        />
                      </TableCell>
                      <TableCell align="center">
                        <Stack direction="row" spacing={1} justifyContent="center">
                          <Tooltip title="View User Profile">
                            <IconButton color="info" onClick={() => navigate(`/profile/${user.id}`)}>
                              <VisibilityIcon size="small" />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Edit Profile Details">
                            <IconButton color="primary" onClick={() => handleOpenEdit(user)}>
                              <EditIcon size="small" />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Reset Password">
                            <IconButton color="warning" onClick={() => handleOpenResetPassword(user)}>
                              <LockResetIcon size="small" />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Delete Account">
                            <IconButton
                              color="error"
                              onClick={() => handleDeleteUser(user.id)}
                              disabled={user.role === "SUPER_ADMIN"}
                            >
                              <DeleteIcon size="small" />
                            </IconButton>
                          </Tooltip>
                        </Stack>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
            <TablePagination
              component="div"
              count={totalElements}
              page={page}
              onPageChange={(e, newPage) => setPage(newPage)}
              rowsPerPage={rowsPerPage}
              onRowsPerPageChange={(e) => {
                setRowsPerPage(parseInt(e.target.value, 10));
                setPage(0);
              }}
            />
          </>
        )}
      </Paper>

      {/* CREATE & EDIT DIALOG */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{editingUser ? "Edit User Account" : "Register Security Account"}</DialogTitle>
        <DialogContent dividers>
          <Stack spacing={3} mt={1}>
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
              onChange={(e) => setFormValues({ ...formValues, username: e.target.value })}
              fullWidth
              required
              disabled={!!editingUser}
            />
            <TextField
              label="Email Address"
              type="email"
              value={formValues.email}
              onChange={(e) => setFormValues({ ...formValues, email: e.target.value })}
              fullWidth
              required
            />
            {!editingUser && (
              <TextField
                label="Initial Password"
                type="password"
                value={formValues.password}
                onChange={(e) => setFormValues({ ...formValues, password: e.target.value })}
                fullWidth
                required
              />
            )}
            <TextField
              select
              label="System Access Role"
              value={formValues.role}
              onChange={(e) => setFormValues({ ...formValues, role: e.target.value })}
              fullWidth
            >
              {ROLES.map((r) => (
                <MenuItem key={r} value={r}>
                  {r}
                </MenuItem>
              ))}
            </TextField>
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
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleSaveUser} variant="contained">
            Save
          </Button>
        </DialogActions>
      </Dialog>

      {/* RESET PASSWORD DIALOG */}
      <Dialog open={!!resetPassUser} onClose={() => setResetPassUser(null)}>
        <DialogTitle>Reset Password</DialogTitle>
        <DialogContent>
          <Typography variant="body2" mb={2}>
            Set a new credentials sequence for user: <b>{resetPassUser?.username}</b>
          </Typography>
          <TextField
            label="New Password"
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            fullWidth
            sx={{ mt: 1 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setResetPassUser(null)}>Cancel</Button>
          <Button onClick={handleSaveResetPassword} variant="contained" color="warning">
            Change Password
          </Button>
        </DialogActions>
      </Dialog>

      <AppSnackbar
        open={snackbar.open}
        message={snackbar.message}
        severity={snackbar.severity}
        handleClose={() => setSnackbar({ ...snackbar, open: false })}
      />
    </Box>
  );
}

export default Users;
import { useEffect, useState } from "react";
import {
  Box,
  Typography,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Chip,
  IconButton,
  Tooltip,
  TextField,
  MenuItem,
  Stack,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
} from "@mui/material";

import { getIncidents, updateIncident, deleteIncident } from "../../services/incidentService";
import { getAllUsers } from "../../services/userService";

import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";
import AssignmentIndIcon from "@mui/icons-material/AssignmentInd";

function Incidents() {
  const [incidents, setIncidents] = useState([]);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [severity, setSeverity] = useState("");
  const [status, setStatus] = useState("");

  // Edit State
  const [selectedIncident, setSelectedIncident] = useState(null);
  const [editOpen, setEditOpen] = useState(false);
  const [newStatus, setNewStatus] = useState("");
  const [newAssignee, setNewAssignee] = useState("");
  const [resolutionNotes, setResolutionNotes] = useState("");

  useEffect(() => {
    loadData();
  }, [search, severity, status]);

  const loadData = async () => {
    setLoading(true);
    try {
      const incData = await getIncidents(search, severity, status, 0, 100);
      setIncidents(incData.content || []);
      const usersData = await getAllUsers();
      setUsers(usersData || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleEditOpen = (incident) => {
    setSelectedIncident(incident);
    setNewStatus(incident.status);
    setNewAssignee(incident.assignedToId || "");
    setResolutionNotes(incident.resolutionNotes || "");
    setEditOpen(true);
  };

  const handleEditSave = async () => {
    if (!selectedIncident) return;
    try {
      const updated = {
        ...selectedIncident,
        status: newStatus,
        assignedToId: newAssignee || null,
        resolutionNotes,
      };
      await updateIncident(selectedIncident.id, updated);
      setEditOpen(false);
      loadData();
    } catch (err) {
      console.error(err);
      alert("Failed to save changes.");
    }
  };

  const handleDelete = async (id) => {
    const confirm = window.confirm("Are you sure you want to delete this incident?");
    if (!confirm) return;
    try {
      await deleteIncident(id);
      loadData();
    } catch (err) {
      console.error(err);
      alert("Failed to delete incident.");
    }
  };

  const getSeverityColor = (sev) => {
    switch (sev?.toUpperCase()) {
      case "CRITICAL": return "error";
      case "HIGH": return "warning";
      case "MEDIUM": return "primary";
      default: return "success";
    }
  };

  const getStatusColor = (st) => {
    switch (st?.toUpperCase()) {
      case "RESOLVED": return "success";
      case "MITIGATED": return "info";
      case "INVESTIGATING": return "warning";
      default: return "default";
    }
  };

  return (
    <Box p={3}>
      <Typography variant="h4" fontWeight={700} mb={3}>
        Security Incidents Queue
      </Typography>

      <Paper sx={{ p: 3, mb: 3, borderRadius: 4 }}>
        <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
          <TextField
            label="Search Incidents"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            fullWidth
            size="small"
          />
          <TextField
            select
            label="Severity"
            value={severity}
            onChange={(e) => setSeverity(e.target.value)}
            sx={{ minWidth: 150 }}
            size="small"
          >
            <MenuItem value="">All Severities</MenuItem>
            <MenuItem value="CRITICAL">Critical</MenuItem>
            <MenuItem value="HIGH">High</MenuItem>
            <MenuItem value="MEDIUM">Medium</MenuItem>
            <MenuItem value="LOW">Low</MenuItem>
          </TextField>
          <TextField
            select
            label="Status"
            value={status}
            onChange={(e) => setStatus(e.target.value)}
            sx={{ minWidth: 150 }}
            size="small"
          >
            <MenuItem value="">All Statuses</MenuItem>
            <MenuItem value="OPEN">Open</MenuItem>
            <MenuItem value="INVESTIGATING">Investigating</MenuItem>
            <MenuItem value="MITIGATED">Mitigated</MenuItem>
            <MenuItem value="RESOLVED">Resolved</MenuItem>
          </TextField>
        </Stack>
      </Paper>

      {loading ? (
        <Box sx={{ display: "flex", justifyContent: "center", mt: 8 }}>
          <CircularProgress />
        </Box>
      ) : (
        <Paper sx={{ overflow: "hidden", borderRadius: 4 }}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell><b>ID</b></TableCell>
                <TableCell><b>Title / Description</b></TableCell>
                <TableCell><b>Severity</b></TableCell>
                <TableCell><b>Status</b></TableCell>
                <TableCell><b>Assigned To</b></TableCell>
                <TableCell><b>Associated Alert</b></TableCell>
                <TableCell align="center"><b>Actions</b></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {incidents.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} align="center">
                    No active incidents.
                  </TableCell>
                </TableRow>
              ) : (
                incidents.map((inc) => (
                  <TableRow key={inc.id} hover>
                    <TableCell>{inc.id}</TableCell>
                    <TableCell>
                      <Typography fontWeight={700}>{inc.title}</Typography>
                      <Typography variant="body2" color="text.secondary">
                        {inc.description}
                      </Typography>
                      {inc.resolutionNotes && (
                        <Typography variant="caption" color="success.main" display="block">
                          Notes: {inc.resolutionNotes}
                        </Typography>
                      )}
                    </TableCell>
                    <TableCell>
                      <Chip label={inc.severity} color={getSeverityColor(inc.severity)} size="small" />
                    </TableCell>
                    <TableCell>
                      <Chip label={inc.status} color={getStatusColor(inc.status)} size="small" />
                    </TableCell>
                    <TableCell>
                      <Stack direction="row" spacing={1} alignItems="center">
                        <AssignmentIndIcon fontSize="small" color="action" />
                        <Typography>{inc.assignedToName || "Unassigned"}</Typography>
                      </Stack>
                    </TableCell>
                    <TableCell>{inc.alertTitle || "N/A"}</TableCell>
                    <TableCell align="center">
                      <Stack direction="row" spacing={1} justifyContent="center">
                        <Tooltip title="Investigate / Update">
                          <IconButton color="primary" onClick={() => handleEditOpen(inc)}>
                            <EditIcon />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Delete">
                          <IconButton color="error" onClick={() => handleDelete(inc.id)}>
                            <DeleteIcon />
                          </IconButton>
                        </Tooltip>
                      </Stack>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </Paper>
      )}

      {/* Update Dialog */}
      <Dialog open={editOpen} onClose={() => setEditOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Investigate Incident #{selectedIncident?.id}</DialogTitle>
        <DialogContent>
          <Stack spacing={3} mt={2}>
            <TextField
              select
              label="Update Status"
              value={newStatus}
              onChange={(e) => setNewStatus(e.target.value)}
              fullWidth
            >
              <MenuItem value="OPEN">Open</MenuItem>
              <MenuItem value="INVESTIGATING">Investigating</MenuItem>
              <MenuItem value="MITIGATED">Mitigated</MenuItem>
              <MenuItem value="RESOLVED">Resolved</MenuItem>
            </TextField>

            <TextField
              select
              label="Assign Specialist"
              value={newAssignee}
              onChange={(e) => setNewAssignee(e.target.value)}
              fullWidth
            >
              <MenuItem value="">Unassign</MenuItem>
              {users.map((u) => (
                <MenuItem key={u.id} value={u.id}>
                  {u.fullName} ({u.role})
                </MenuItem>
              ))}
            </TextField>

            <TextField
              label="Resolution & Investigation Notes"
              value={resolutionNotes}
              onChange={(e) => setResolutionNotes(e.target.value)}
              multiline
              rows={4}
              fullWidth
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditOpen(false)}>Cancel</Button>
          <Button onClick={handleEditSave} variant="contained" color="primary">
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

export default Incidents;

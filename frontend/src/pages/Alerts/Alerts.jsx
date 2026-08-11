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
  TextField,
  MenuItem,
  Stack,
  Button,
  Pagination,
} from "@mui/material";

import {
  getAlerts,
  updateAlert,
  deleteAlert,
} from "../../services/alertService";

import DeleteIcon from "@mui/icons-material/Delete";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import BlockIcon from "@mui/icons-material/Block";

function Alerts() {
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [severity, setSeverity] = useState("");
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);

  useEffect(() => {
    loadAlerts();
  }, [search, severity, status, page]);

  const loadAlerts = async () => {
    setLoading(true);
    try {
      const data = await getAlerts(search, severity, status, page - 1, 10);
      setAlerts(data.content || []);
      setTotalPages(data.totalPages || 1);
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateStatus = async (id, alertObj, newStatus) => {
    try {
      const updated = { ...alertObj, status: newStatus };
      await updateAlert(id, updated);
      loadAlerts();
    } catch (error) {
      console.error(error);
      alert("Failed to update status.");
    }
  };

  const handleDelete = async (id) => {
    const confirm = window.confirm("Are you sure you want to delete this alert?");
    if (!confirm) return;

    try {
      await deleteAlert(id);
      loadAlerts();
    } catch (error) {
      console.error(error);
      alert("Failed to delete alert.");
    }
  };

  const getSeverityColor = (sev) => {
    switch (sev?.toUpperCase()) {
      case "CRITICAL":
        return "error";
      case "HIGH":
        return "warning";
      case "MEDIUM":
        return "primary";
      default:
        return "success";
    }
  };

  return (
    <Box p={3}>
      <Typography variant="h4" fontWeight={700} mb={3}>
        Security Alerts Management
      </Typography>

      {/* Filter Bar */}
      <Paper sx={{ p: 3, mb: 3, borderRadius: 4 }}>
        <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
          <TextField
            label="Search Alerts"
            variant="outlined"
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
            <MenuItem value="RESOLVED">Resolved</MenuItem>
            <MenuItem value="DISMISSED">Dismissed</MenuItem>
          </TextField>
        </Stack>
      </Paper>

      {loading ? (
        <Box sx={{ display: "flex", justifyContent: "center", mt: 8 }}>
          <CircularProgress />
        </Box>
      ) : (
        <>
          <Paper sx={{ overflow: "hidden", borderRadius: 4, mb: 2 }}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell><b>ID</b></TableCell>
                  <TableCell><b>Title</b></TableCell>
                  <TableCell><b>Category</b></TableCell>
                  <TableCell><b>Source IP</b></TableCell>
                  <TableCell><b>Severity</b></TableCell>
                  <TableCell><b>Status</b></TableCell>
                  <TableCell><b>Asset</b></TableCell>
                  <TableCell align="center"><b>Actions</b></TableCell>
                </TableRow>
              </TableHead>

              <TableBody>
                {alerts.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={8} align="center">
                      No security alerts found.
                    </TableCell>
                  </TableRow>
                ) : (
                  alerts.map((alertItem) => (
                    <TableRow key={alertItem.id} hover>
                      <TableCell>{alertItem.id}</TableCell>
                      <TableCell>
                        <Typography fontWeight={600}>{alertItem.title}</Typography>
                        <Typography variant="caption" color="text.secondary">
                          {alertItem.description}
                        </Typography>
                      </TableCell>
                      <TableCell>{alertItem.category || "General"}</TableCell>
                      <TableCell>{alertItem.sourceIp}</TableCell>
                      <TableCell>
                        <Chip
                          label={alertItem.severity}
                          color={getSeverityColor(alertItem.severity)}
                          size="small"
                          sx={{ fontWeight: "bold" }}
                        />
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={alertItem.status}
                          variant="outlined"
                          color={alertItem.status === "RESOLVED" ? "success" : "default"}
                          size="small"
                        />
                      </TableCell>
                      <TableCell>{alertItem.assetName || "N/A"}</TableCell>
                      <TableCell align="center">
                        <Stack direction="row" spacing={1} justifyContent="center">
                          {alertItem.status === "OPEN" && (
                            <>
                              <Tooltip title="Resolve Alert">
                                <IconButton
                                  color="success"
                                  onClick={() => handleUpdateStatus(alertItem.id, alertItem, "RESOLVED")}
                                >
                                  <CheckCircleIcon />
                                </IconButton>
                              </Tooltip>

                              <Tooltip title="Dismiss Alert">
                                <IconButton
                                  color="warning"
                                  onClick={() => handleUpdateStatus(alertItem.id, alertItem, "DISMISSED")}
                                >
                                  <BlockIcon />
                                </IconButton>
                              </Tooltip>
                            </>
                          )}

                          <Tooltip title="Delete Alert">
                            <IconButton color="error" onClick={() => handleDelete(alertItem.id)}>
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

          <Box display="flex" justifyContent="center" mt={3}>
            <Pagination
              count={totalPages}
              page={page}
              onChange={(e, val) => setPage(val)}
              color="primary"
            />
          </Box>
        </>
      )}
    </Box>
  );
}

export default Alerts;
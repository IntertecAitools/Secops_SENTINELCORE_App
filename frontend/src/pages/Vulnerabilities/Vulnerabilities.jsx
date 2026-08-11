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
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
} from "@mui/material";

import {
  getVulnerabilities,
  createVulnerability,
  updateVulnerability,
  deleteVulnerability,
} from "../../services/vulnerabilityService";
import { getAllAssets } from "../../services/assetService";

import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";
import AddIcon from "@mui/icons-material/Add";

function Vulnerabilities() {
  const [vulns, setVulns] = useState([]);
  const [assets, setAssets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [severity, setSeverity] = useState("");
  const [status, setStatus] = useState("");

  // Edit / Create Modal State
  const [dialogOpen, setDialogOpen] = useState(false);
  const [selectedVuln, setSelectedVuln] = useState(null);
  const [formTitle, setFormTitle] = useState("");
  const [formDesc, setFormDesc] = useState("");
  const [formSeverity, setFormSeverity] = useState("MEDIUM");
  const [formCveId, setFormCveId] = useState("");
  const [formCvss, setFormCvss] = useState(5.0);
  const [formRemediation, setFormRemediation] = useState("");
  const [formStatus, setFormStatus] = useState("OPEN");
  const [formAssetId, setFormAssetId] = useState("");

  useEffect(() => {
    loadData();
  }, [search, severity, status]);

  const loadData = async () => {
    setLoading(true);
    try {
      const vData = await getVulnerabilities(search, severity, status, 0, 100);
      setVulns(vData.content || []);
      const aData = await getAllAssets();
      setAssets(aData || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenEdit = (v) => {
    setSelectedVuln(v);
    setFormTitle(v.title);
    setFormDesc(v.description || "");
    setFormSeverity(v.severity);
    setFormCveId(v.cveId || "");
    setFormCvss(v.cvssScore || 5.0);
    setFormRemediation(v.remediation || "");
    setFormStatus(v.status);
    setFormAssetId(v.assetId || "");
    setDialogOpen(true);
  };

  const handleOpenCreate = () => {
    setSelectedVuln(null);
    setFormTitle("");
    setFormDesc("");
    setFormSeverity("MEDIUM");
    setFormCveId("");
    setFormCvss(5.0);
    setFormRemediation("");
    setFormStatus("OPEN");
    setFormAssetId("");
    setDialogOpen(true);
  };

  const handleSave = async () => {
    if (!formTitle || !formAssetId) {
      alert("Title and Target Asset are required.");
      return;
    }

    const payload = {
      title: formTitle,
      description: formDesc,
      severity: formSeverity,
      cveId: formCveId,
      cvssScore: parseFloat(formCvss),
      remediation: formRemediation,
      status: formStatus,
      assetId: formAssetId,
    };

    try {
      if (selectedVuln) {
        await updateVulnerability(selectedVuln.id, payload);
      } else {
        await createVulnerability(payload);
      }
      setDialogOpen(false);
      loadData();
    } catch (err) {
      console.error(err);
      alert("Failed to save vulnerability record.");
    }
  };

  const handleDelete = async (id) => {
    const confirm = window.confirm("Are you sure you want to delete this vulnerability record?");
    if (!confirm) return;

    try {
      await deleteVulnerability(id);
      loadData();
    } catch (err) {
      console.error(err);
      alert("Failed to delete vulnerability.");
    }
  };

  const getCvssColor = (score) => {
    if (score >= 9.0) return "#d32f2f"; // Critical
    if (score >= 7.0) return "#f57c00"; // High
    if (score >= 4.0) return "#1976d2"; // Medium
    return "#388e3c"; // Low
  };

  return (
    <Box p={3}>
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4" fontWeight={700}>
          Vulnerability Management (CVE)
        </Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={handleOpenCreate}>
          Report Vulnerability
        </Button>
      </Stack>

      {/* Filter Bar */}
      <Paper sx={{ p: 3, mb: 3, borderRadius: 4 }}>
        <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
          <TextField
            label="Search CVE / Title"
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
            <MenuItem value="PATCHED">Patched</MenuItem>
            <MenuItem value="IGNORED">Ignored</MenuItem>
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
                <TableCell><b>CVE ID</b></TableCell>
                <TableCell><b>Vulnerability Title</b></TableCell>
                <TableCell><b>Target Asset</b></TableCell>
                <TableCell><b>CVSS Score</b></TableCell>
                <TableCell><b>Severity</b></TableCell>
                <TableCell><b>Status</b></TableCell>
                <TableCell align="center"><b>Actions</b></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {vulns.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} align="center">
                    No vulnerabilities recorded.
                  </TableCell>
                </TableRow>
              ) : (
                vulns.map((v) => (
                  <TableRow key={v.id} hover>
                    <TableCell>
                      <Chip label={v.cveId || "NO-CVE"} color="secondary" size="small" sx={{ fontWeight: "bold" }} />
                    </TableCell>
                    <TableCell>
                      <Typography fontWeight={700}>{v.title}</Typography>
                      <Typography variant="body2" color="text.secondary">
                        {v.description}
                      </Typography>
                      {v.remediation && (
                        <Typography variant="caption" color="primary.main" display="block">
                          Remediation: {v.remediation}
                        </Typography>
                      )}
                    </TableCell>
                    <TableCell>{v.assetName || "N/A"}</TableCell>
                    <TableCell>
                      <Box
                        sx={{
                          display: "inline-block",
                          px: 1.5,
                          py: 0.5,
                          borderRadius: 2,
                          color: "white",
                          fontWeight: "bold",
                          bgcolor: getCvssColor(v.cvssScore),
                        }}
                      >
                        {v.cvssScore?.toFixed(1) || "0.0"}
                      </Box>
                    </TableCell>
                    <TableCell>
                      <Chip label={v.severity} color={v.severity === "CRITICAL" || v.severity === "HIGH" ? "error" : "primary"} size="small" />
                    </TableCell>
                    <TableCell>
                      <Chip label={v.status} variant="outlined" color={v.status === "PATCHED" ? "success" : "default"} size="small" />
                    </TableCell>
                    <TableCell align="center">
                      <Stack direction="row" spacing={1} justifyContent="center">
                        <Tooltip title="Edit Record">
                          <IconButton color="primary" onClick={() => handleOpenEdit(v)}>
                            <EditIcon />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Delete">
                          <IconButton color="error" onClick={() => handleDelete(v.id)}>
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

      {/* Editor Modal */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>{selectedVuln ? "Edit Vulnerability Details" : "Report New Vulnerability"}</DialogTitle>
        <DialogContent>
          <Stack spacing={3} mt={2}>
            <TextField
              label="Vulnerability Title"
              value={formTitle}
              onChange={(e) => setFormTitle(e.target.value)}
              fullWidth
              required
            />
            <TextField
              label="CVE ID (e.g. CVE-2026-1002)"
              value={formCveId}
              onChange={(e) => setFormCveId(e.target.value)}
              fullWidth
            />
            <TextField
              select
              label="Target Asset"
              value={formAssetId}
              onChange={(e) => setFormAssetId(e.target.value)}
              fullWidth
              required
            >
              {assets.map((a) => (
                <MenuItem key={a.id} value={a.id}>
                  {a.assetName} ({a.ipAddress})
                </MenuItem>
              ))}
            </TextField>
            <Stack direction="row" spacing={2}>
              <TextField
                select
                label="Severity"
                value={formSeverity}
                onChange={(e) => setFormSeverity(e.target.value)}
                fullWidth
              >
                <MenuItem value="LOW">Low</MenuItem>
                <MenuItem value="MEDIUM">Medium</MenuItem>
                <MenuItem value="HIGH">High</MenuItem>
                <MenuItem value="CRITICAL">Critical</MenuItem>
              </TextField>
              <TextField
                label="CVSS Score (0.0 - 10.0)"
                type="number"
                inputProps={{ step: 0.1, min: 0.0, max: 10.0 }}
                value={formCvss}
                onChange={(e) => setFormCvss(e.target.value)}
                fullWidth
              />
            </Stack>
            <TextField
              select
              label="Status"
              value={formStatus}
              onChange={(e) => setFormStatus(e.target.value)}
              fullWidth
            >
              <MenuItem value="OPEN">Open</MenuItem>
              <MenuItem value="PATCHED">Patched</MenuItem>
              <MenuItem value="IGNORED">Ignored</MenuItem>
            </TextField>
            <TextField
              label="Description"
              value={formDesc}
              onChange={(e) => setFormDesc(e.target.value)}
              multiline
              rows={3}
              fullWidth
            />
            <TextField
              label="Remediation Plan"
              value={formRemediation}
              onChange={(e) => setFormRemediation(e.target.value)}
              multiline
              rows={3}
              fullWidth
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleSave} variant="contained" color="primary">
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

export default Vulnerabilities;

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
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from "@mui/material";

import {
  getPatches,
  createPatch,
  applyPatch,
  deletePatch,
} from "../../services/patchService";
import { getAllAssets } from "../../services/assetService";

import FlashOnIcon from "@mui/icons-material/FlashOn";
import DeleteIcon from "@mui/icons-material/Delete";
import AddIcon from "@mui/icons-material/Add";

function PatchManagement() {
  const [patches, setPatches] = useState([]);
  const [assets, setAssets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("");

  // Modal State
  const [dialogOpen, setDialogOpen] = useState(false);
  const [formTitle, setFormTitle] = useState("");
  const [formDesc, setFormDesc] = useState("");
  const [formVersion, setFormVersion] = useState("");
  const [formStatus, setFormStatus] = useState("PENDING");
  const [formOs, setFormOs] = useState("");
  const [formAssetId, setFormAssetId] = useState("");

  useEffect(() => {
    loadData();
  }, [search, status]);

  const loadData = async () => {
    setLoading(true);
    try {
      const pData = await getPatches(search, status, 0, 100);
      setPatches(pData.content || []);
      const aData = await getAllAssets();
      setAssets(aData || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleApply = async (id) => {
    const confirm = window.confirm("Are you sure you want to deploy this patch to the target asset?");
    if (!confirm) return;
    try {
      await applyPatch(id);
      loadData();
    } catch (err) {
      console.error(err);
      alert("Failed to apply patch.");
    }
  };

  const handleDelete = async (id) => {
    const confirm = window.confirm("Delete this patch record?");
    if (!confirm) return;
    try {
      await deletePatch(id);
      loadData();
    } catch (err) {
      console.error(err);
      alert("Failed to delete patch.");
    }
  };

  const handleSave = async () => {
    if (!formTitle || !formVersion || !formAssetId) {
      alert("Title, Version, and Target Asset are required.");
      return;
    }
    const payload = {
      title: formTitle,
      description: formDesc,
      version: formVersion,
      status: formStatus,
      targetOs: formOs,
      assetId: formAssetId,
    };
    try {
      await createPatch(payload);
      setDialogOpen(false);
      setFormTitle("");
      setFormDesc("");
      setFormVersion("");
      setFormOs("");
      setFormAssetId("");
      loadData();
    } catch (err) {
      console.error(err);
      alert("Failed to log patch.");
    }
  };

  return (
    <Box p={3}>
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4" fontWeight={700}>
          Patch Management & Deployment
        </Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialogOpen(true)}>
          Register Patch
        </Button>
      </Stack>

      {/* Filter Bar */}
      <Paper sx={{ p: 3, mb: 3, borderRadius: 4 }}>
        <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
          <TextField
            label="Search Patches"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            fullWidth
            size="small"
          />
          <TextField
            select
            label="Status"
            value={status}
            onChange={(e) => setStatus(e.target.value)}
            sx={{ minWidth: 200 }}
            size="small"
          >
            <MenuItem value="">All Statuses</MenuItem>
            <MenuItem value="PENDING">Pending</MenuItem>
            <MenuItem value="APPROVED">Approved</MenuItem>
            <MenuItem value="APPLIED">Applied</MenuItem>
            <MenuItem value="FAILED">Failed</MenuItem>
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
                <TableCell><b>Patch Title</b></TableCell>
                <TableCell><b>Version</b></TableCell>
                <TableCell><b>Target OS</b></TableCell>
                <TableCell><b>Target Asset</b></TableCell>
                <TableCell><b>Status</b></TableCell>
                <TableCell><b>Released On</b></TableCell>
                <TableCell><b>Applied On</b></TableCell>
                <TableCell align="center"><b>Actions</b></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {patches.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={8} align="center">
                    No patches scheduled.
                  </TableCell>
                </TableRow>
              ) : (
                patches.map((p) => (
                  <TableRow key={p.id} hover>
                    <TableCell>
                      <Typography fontWeight={700}>{p.title}</Typography>
                      <Typography variant="body2" color="text.secondary">
                        {p.description}
                      </Typography>
                    </TableCell>
                    <TableCell>{p.version}</TableCell>
                    <TableCell>
                      <Chip label={p.targetOs || "Any"} variant="outlined" size="small" />
                    </TableCell>
                    <TableCell>{p.assetName || "N/A"}</TableCell>
                    <TableCell>
                      <Chip
                        label={p.status}
                        color={p.status === "APPLIED" ? "success" : p.status === "FAILED" ? "error" : "primary"}
                        size="small"
                      />
                    </TableCell>
                    <TableCell>
                      {p.releaseDate ? new Date(p.releaseDate).toLocaleDateString() : "N/A"}
                    </TableCell>
                    <TableCell>
                      {p.appliedAt ? new Date(p.appliedAt).toLocaleString() : "Not applied"}
                    </TableCell>
                    <TableCell align="center">
                      <Stack direction="row" spacing={1} justifyContent="center">
                        {p.status !== "APPLIED" && (
                          <Tooltip title="Deploy Patch">
                            <IconButton color="success" onClick={() => handleApply(p.id)}>
                              <FlashOnIcon />
                            </IconButton>
                          </Tooltip>
                        )}
                        <Tooltip title="Delete">
                          <IconButton color="error" onClick={() => handleDelete(p.id)}>
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
        <DialogTitle>Register System Patch</DialogTitle>
        <DialogContent>
          <Stack spacing={3} mt={2}>
            <TextField
              label="Patch Name"
              value={formTitle}
              onChange={(e) => setFormTitle(e.target.value)}
              fullWidth
              required
            />
            <Stack direction="row" spacing={2}>
              <TextField
                label="Version (e.g. v2.4.1)"
                value={formVersion}
                onChange={(e) => setFormVersion(e.target.value)}
                fullWidth
                required
              />
              <TextField
                label="Target OS (e.g. Ubuntu 22.04 / Windows)"
                value={formOs}
                onChange={(e) => setFormOs(e.target.value)}
                fullWidth
              />
            </Stack>
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
            <TextField
              select
              label="Status"
              value={formStatus}
              onChange={(e) => setFormStatus(e.target.value)}
              fullWidth
            >
              <MenuItem value="PENDING">Pending Approval</MenuItem>
              <MenuItem value="APPROVED">Approved</MenuItem>
            </TextField>
            <TextField
              label="Description"
              value={formDesc}
              onChange={(e) => setFormDesc(e.target.value)}
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

export default PatchManagement;

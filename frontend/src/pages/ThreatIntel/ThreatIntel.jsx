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
  getThreatFeeds,
  createThreatFeed,
  updateThreatFeed,
  deleteThreatFeed,
} from "../../services/threatIntelService";

import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";
import AddIcon from "@mui/icons-material/Add";
import GavelIcon from "@mui/icons-material/Gavel";

function ThreatIntel() {
  const [feeds, setFeeds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [indicatorType, setIndicatorType] = useState("");
  const [confidenceLevel, setConfidenceLevel] = useState("");

  // Editor Modal State
  const [dialogOpen, setDialogOpen] = useState(false);
  const [selectedFeed, setSelectedFeed] = useState(null);
  const [formType, setFormType] = useState("IP");
  const [formValue, setFormValue] = useState("");
  const [formThreatType, setFormThreatType] = useState("MALWARE");
  const [formConfidence, setFormConfidence] = useState("HIGH");
  const [formDesc, setFormDesc] = useState("");
  const [formSource, setFormSource] = useState("");

  useEffect(() => {
    loadData();
  }, [search, indicatorType, confidenceLevel]);

  const loadData = async () => {
    setLoading(true);
    try {
      const data = await getThreatFeeds(search, indicatorType, confidenceLevel, 0, 100);
      setFeeds(data.content || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenEdit = (f) => {
    setSelectedFeed(f);
    setFormType(f.indicatorType);
    setFormValue(f.value);
    setFormThreatType(f.threatType);
    setFormConfidence(f.confidenceLevel);
    setFormDesc(f.description || "");
    setFormSource(f.source || "");
    setDialogOpen(true);
  };

  const handleOpenCreate = () => {
    setSelectedFeed(null);
    setFormType("IP");
    setFormValue("");
    setFormThreatType("MALWARE");
    setFormConfidence("HIGH");
    setFormDesc("");
    setFormSource("OSINT");
    setDialogOpen(true);
  };

  const handleSave = async () => {
    if (!formValue) {
      alert("Indicator Value is required.");
      return;
    }

    const payload = {
      indicatorType: formType,
      value: formValue,
      threatType: formThreatType,
      confidenceLevel: formConfidence,
      description: formDesc,
      source: formSource,
    };

    try {
      if (selectedFeed) {
        await updateThreatFeed(selectedFeed.id, payload);
      } else {
        await createThreatFeed(payload);
      }
      setDialogOpen(false);
      loadData();
    } catch (err) {
      console.error(err);
      alert("Failed to save threat indicator.");
    }
  };

  const handleDelete = async (id) => {
    const confirm = window.confirm("Are you sure you want to delete this threat indicator?");
    if (!confirm) return;

    try {
      await deleteThreatFeed(id);
      loadData();
    } catch (err) {
      console.error(err);
      alert("Failed to delete indicator.");
    }
  };

  const getConfidenceColor = (lvl) => {
    switch (lvl?.toUpperCase()) {
      case "HIGH":
        return "error";
      case "MEDIUM":
        return "warning";
      default:
        return "primary";
    }
  };

  return (
    <Box p={3}>
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4" fontWeight={700}>
          Threat Intelligence Feeds (IOCs)
        </Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={handleOpenCreate}>
          Add Indicator (IOC)
        </Button>
      </Stack>

      <Paper sx={{ p: 3, mb: 3, borderRadius: 4 }}>
        <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
          <TextField
            label="Search Indicator / Threat Type"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            fullWidth
            size="small"
          />
          <TextField
            select
            label="Indicator Type"
            value={indicatorType}
            onChange={(e) => setIndicatorType(e.target.value)}
            sx={{ minWidth: 160 }}
            size="small"
          >
            <MenuItem value="">All Types</MenuItem>
            <MenuItem value="IP">IP Address</MenuItem>
            <MenuItem value="DOMAIN">Domain Name</MenuItem>
            <MenuItem value="FILE_HASH">File Hash (MD5/SHA)</MenuItem>
            <MenuItem value="URL">URL Link</MenuItem>
          </TextField>
          <TextField
            select
            label="Confidence"
            value={confidenceLevel}
            onChange={(e) => setConfidenceLevel(e.target.value)}
            sx={{ minWidth: 160 }}
            size="small"
          >
            <MenuItem value="">All Levels</MenuItem>
            <MenuItem value="HIGH">High</MenuItem>
            <MenuItem value="MEDIUM">Medium</MenuItem>
            <MenuItem value="LOW">Low</MenuItem>
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
                <TableCell><b>Type</b></TableCell>
                <TableCell><b>Indicator Value (IOC)</b></TableCell>
                <TableCell><b>Threat Type</b></TableCell>
                <TableCell><b>Confidence</b></TableCell>
                <TableCell><b>Source</b></TableCell>
                <TableCell><b>Description</b></TableCell>
                <TableCell align="center"><b>Actions</b></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {feeds.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} align="center">
                    No threat indicators logged.
                  </TableCell>
                </TableRow>
              ) : (
                feeds.map((f) => (
                  <TableRow key={f.id} hover>
                    <TableCell>
                      <Chip label={f.indicatorType} color="primary" size="small" />
                    </TableCell>
                    <TableCell>
                      <Typography fontWeight={700} sx={{ fontFamily: "monospace" }}>
                        {f.value}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Chip label={f.threatType} variant="outlined" size="small" />
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={f.confidenceLevel + " CONFIDENCE"}
                        color={getConfidenceColor(f.confidenceLevel)}
                        size="small"
                        sx={{ fontWeight: "bold" }}
                      />
                    </TableCell>
                    <TableCell>{f.source || "OSINT"}</TableCell>
                    <TableCell>{f.description}</TableCell>
                    <TableCell align="center">
                      <Stack direction="row" spacing={1} justifyContent="center">
                        <Tooltip title="Edit Indicator">
                          <IconButton color="primary" onClick={() => handleOpenEdit(f)}>
                            <EditIcon />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Delete">
                          <IconButton color="error" onClick={() => handleDelete(f.id)}>
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
        <DialogTitle>{selectedFeed ? "Edit Threat Indicator" : "Add Threat Indicator (IOC)"}</DialogTitle>
        <DialogContent>
          <Stack spacing={3} mt={2}>
            <Stack direction="row" spacing={2}>
              <TextField
                select
                label="Indicator Type"
                value={formType}
                onChange={(e) => setFormType(e.target.value)}
                fullWidth
              >
                <MenuItem value="IP">IP Address</MenuItem>
                <MenuItem value="DOMAIN">Domain Name</MenuItem>
                <MenuItem value="FILE_HASH">File Hash</MenuItem>
                <MenuItem value="URL">URL Link</MenuItem>
              </TextField>
              <TextField
                select
                label="Confidence Level"
                value={formConfidence}
                onChange={(e) => setFormConfidence(e.target.value)}
                fullWidth
              >
                <MenuItem value="LOW">Low</MenuItem>
                <MenuItem value="MEDIUM">Medium</MenuItem>
                <MenuItem value="HIGH">High</MenuItem>
              </TextField>
            </Stack>
            <TextField
              label="Indicator Value (e.g. 10.99.1.5 or baddomain.com)"
              value={formValue}
              onChange={(e) => setFormValue(e.target.value)}
              fullWidth
              required
            />
            <Stack direction="row" spacing={2}>
              <TextField
                select
                label="Threat Type"
                value={formThreatType}
                onChange={(e) => setFormThreatType(e.target.value)}
                fullWidth
              >
                <MenuItem value="MALWARE">Malware</MenuItem>
                <MenuItem value="PHISHING">Phishing</MenuItem>
                <MenuItem value="BOTNET">Botnet C2</MenuItem>
                <MenuItem value="APT">Advanced Threat</MenuItem>
              </TextField>
              <TextField
                label="Intel Source"
                value={formSource}
                onChange={(e) => setFormSource(e.target.value)}
                fullWidth
              />
            </Stack>
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

export default ThreatIntel;

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
  Grid,
} from "@mui/material";

import {
  getRiskAssessments,
  createRiskAssessment,
  deleteRiskAssessment,
} from "../../services/riskAssessmentService";
import { getAllAssets } from "../../services/assetService";

import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";
import AddIcon from "@mui/icons-material/Add";

function RiskAssessment() {
  const [assessments, setAssessments] = useState([]);
  const [assets, setAssets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");

  // Create Modal State
  const [dialogOpen, setDialogOpen] = useState(false);
  const [assetId, setAssetId] = useState("");
  const [likelihood, setLikelihood] = useState("MEDIUM");
  const [impact, setImpact] = useState("MEDIUM");
  const [desc, setDesc] = useState("");

  useEffect(() => {
    loadData();
  }, [search]);

  const loadData = async () => {
    setLoading(true);
    try {
      const data = await getRiskAssessments(search, 0, 100);
      setAssessments(data.content || []);
      const aData = await getAllAssets();
      setAssets(aData || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async () => {
    if (!assetId) {
      alert("Target Asset is required.");
      return;
    }
    const payload = {
      assetId,
      likelihood,
      impact,
      description: desc,
    };
    try {
      await createRiskAssessment(payload);
      setDialogOpen(false);
      setAssetId("");
      setDesc("");
      loadData();
    } catch (err) {
      console.error(err);
      alert("Failed to submit risk assessment.");
    }
  };

  const handleDelete = async (id) => {
    const confirm = window.confirm("Delete this risk assessment?");
    if (!confirm) return;
    try {
      await deleteRiskAssessment(id);
      loadData();
    } catch (err) {
      console.error(err);
      alert("Failed to delete.");
    }
  };

  const getScoreColor = (score) => {
    if (score >= 60) return "error";
    if (score >= 35) return "warning";
    return "success";
  };

  // Matrix Configuration
  const levels = ["LOW", "MEDIUM", "HIGH"];
  const matrixColor = (l, i) => {
    if (l === "HIGH" && i === "HIGH") return "#d32f2f"; // Critical
    if (l === "LOW" && i === "LOW") return "#2e7d32"; // Low
    if ((l === "HIGH" && i === "MEDIUM") || (l === "MEDIUM" && i === "HIGH")) return "#ed6c02"; // High
    return "#e2b13c"; // Medium
  };

  return (
    <Box p={3}>
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4" fontWeight={700}>
          Infrastructure Risk Assessment
        </Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setDialogOpen(true)}>
          New Assessment
        </Button>
      </Stack>

      <Grid container spacing={4} mb={4}>
        {/* Interactive Matrix */}
        <Grid size={{ xs: 12, md: 5 }}>
          <Paper sx={{ p: 3, borderRadius: 4, height: "100%" }}>
            <Typography variant="h6" fontWeight={700} mb={2} align="center">
              Risk Severity Matrix (3x3)
            </Typography>
            <Box
              sx={{
                display: "grid",
                gridTemplateColumns: "80px repeat(3, 1fr)",
                gap: 1.5,
                textAlign: "center",
              }}
            >
              {/* Top Row: Empty corner followed by Impact labels */}
              <Box />
              <Box sx={{ fontWeight: "bold" }}>Impact: Low</Box>
              <Box sx={{ fontWeight: "bold" }}>Impact: Med</Box>
              <Box sx={{ fontWeight: "bold" }}>Impact: High</Box>

              {/* Rows */}
              {levels.slice().reverse().map((l) => (
                <>
                  <Box sx={{ display: "flex", alignItems: "center", fontWeight: "bold" }}>
                    Like: {l}
                  </Box>
                  {levels.map((i) => (
                    <Box
                      sx={{
                        bgcolor: matrixColor(l, i),
                        height: 55,
                        borderRadius: 2,
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        color: "white",
                        fontWeight: "bold",
                        boxShadow: 2,
                        fontSize: "0.9rem",
                      }}
                    >
                      {l === "HIGH" && i === "HIGH" ? "Critical" : l === "LOW" && i === "LOW" ? "Low" : (l === "HIGH" && i === "MEDIUM") || (l === "MEDIUM" && i === "HIGH") ? "High" : "Medium"}
                    </Box>
                  ))}
                </>
              ))}
            </Box>
          </Paper>
        </Grid>

        {/* Info Box */}
        <Grid size={{ xs: 12, md: 7 }}>
          <Paper sx={{ p: 4, borderRadius: 4, height: "100%", bgcolor: "background.paper" }}>
            <Typography variant="h6" fontWeight={700} gutterBottom>
              Methodology
            </Typography>
            <Typography color="text.secondary" paragraph>
              The platform calculates the risk level based on the mathematical product of the **Likelihood** of a threat event occurring and the operational **Impact** to the targeted system asset.
            </Typography>
            <Typography color="text.secondary" paragraph>
              Risk score ranges are converted to scale values and integrated directly into the infrastructure monitoring alerts. Recommended actions are automatically assigned based on calculated scores.
            </Typography>
          </Paper>
        </Grid>
      </Grid>

      {/* Filter Bar */}
      <Paper sx={{ p: 3, mb: 3, borderRadius: 4 }}>
        <TextField
          label="Search Risk Assessments by asset..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          fullWidth
          size="small"
        />
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
                <TableCell><b>Asset Name</b></TableCell>
                <TableCell><b>Likelihood</b></TableCell>
                <TableCell><b>Impact</b></TableCell>
                <TableCell><b>Calculated Risk Score</b></TableCell>
                <TableCell><b>Description</b></TableCell>
                <TableCell><b>Assessed Date</b></TableCell>
                <TableCell align="center"><b>Actions</b></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {assessments.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={8} align="center">
                    No risk assessments registered.
                  </TableCell>
                </TableRow>
              ) : (
                assessments.map((ra) => (
                  <TableRow key={ra.id} hover>
                    <TableCell>{ra.id}</TableCell>
                    <TableCell>
                      <Typography fontWeight={700}>{ra.assetName}</Typography>
                    </TableCell>
                    <TableCell>
                      <Chip label={ra.likelihood} variant="outlined" size="small" />
                    </TableCell>
                    <TableCell>
                      <Chip label={ra.impact} variant="outlined" size="small" />
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={ra.riskScore + "%"}
                        color={getScoreColor(ra.riskScore)}
                        sx={{ fontWeight: "bold" }}
                      />
                    </TableCell>
                    <TableCell>{ra.description}</TableCell>
                    <TableCell>
                      {ra.assessmentDate ? new Date(ra.assessmentDate).toLocaleDateString() : "N/A"}
                    </TableCell>
                    <TableCell align="center">
                      <Tooltip title="Delete">
                        <IconButton color="error" onClick={() => handleDelete(ra.id)}>
                          <DeleteIcon />
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </Paper>
      )}

      {/* Creator Modal */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Log New Risk Assessment</DialogTitle>
        <DialogContent>
          <Stack spacing={3} mt={2}>
            <TextField
              select
              label="Target Asset"
              value={assetId}
              onChange={(e) => setAssetId(e.target.value)}
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
                label="Likelihood"
                value={likelihood}
                onChange={(e) => setLikelihood(e.target.value)}
                fullWidth
              >
                <MenuItem value="LOW">Low</MenuItem>
                <MenuItem value="MEDIUM">Medium</MenuItem>
                <MenuItem value="HIGH">High</MenuItem>
              </TextField>

              <TextField
                select
                label="Impact"
                value={impact}
                onChange={(e) => setImpact(e.target.value)}
                fullWidth
              >
                <MenuItem value="LOW">Low</MenuItem>
                <MenuItem value="MEDIUM">Medium</MenuItem>
                <MenuItem value="HIGH">High</MenuItem>
              </TextField>
            </Stack>

            <TextField
              label="Risk Details / Notes"
              value={desc}
              onChange={(e) => setDesc(e.target.value)}
              multiline
              rows={3}
              fullWidth
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleCreate} variant="contained" color="primary">
            Submit
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

export default RiskAssessment;

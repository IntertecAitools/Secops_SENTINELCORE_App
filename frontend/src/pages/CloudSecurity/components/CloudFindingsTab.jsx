import { useCallback, useEffect, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from "@mui/material";

import {
  createRemediation,
  getCloudAccounts,
  getCloudFindings,
  newIdempotencyKey,
  recheckCloudFinding,
} from "../../../services/cloudService";
import { apiErrorMessage } from "../../../services/api";
import { hasPermission } from "../../../services/authService";
import { formatDateTime, REMEDIATION_LABELS, severityColor } from "../cloudUi";

function CloudFindingsTab({ notify }) {
  const canRequest = hasPermission("REMEDIATION_REQUEST");
  const canRecheck = hasPermission("CLOUD_SCAN_RUN");

  const [accounts, setAccounts] = useState([]);
  const [findings, setFindings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [filters, setFilters] = useState({ accountId: "", status: "OPEN", severity: "", search: "" });

  const [selected, setSelected] = useState(null);
  const [rechecking, setRechecking] = useState(false);
  const [requestOpen, setRequestOpen] = useState(false);
  const [justification, setJustification] = useState("");
  const [idempotencyKey, setIdempotencyKey] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    getCloudAccounts().then((a) => setAccounts(a || [])).catch(() => setAccounts([]));
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const page = await getCloudFindings({ ...filters, page: 0, size: 100 });
      setFindings(page.content || []);
    } catch (err) {
      setLoadError(apiErrorMessage(err, "Failed to load findings"));
    } finally {
      setLoading(false);
    }
  }, [filters]);

  useEffect(() => {
    load();
  }, [load]);

  const openRequest = () => {
    setJustification("");
    // Generated once per dialog so that retrying the same submission cannot create duplicates
    setIdempotencyKey(newIdempotencyKey());
    setRequestOpen(true);
  };

  const submitRequest = async () => {
    setSubmitting(true);
    try {
      await createRemediation(
        { findingId: selected.id, actionType: selected.supportedRemediation, justification },
        idempotencyKey
      );
      notify("Remediation requested. It now needs approval by another user.");
      setRequestOpen(false);
      setSelected(null);
    } catch (err) {
      notify(apiErrorMessage(err, "Failed to request remediation"), "error");
    } finally {
      setSubmitting(false);
    }
  };

  const recheck = async () => {
    setRechecking(true);
    try {
      const updated = await recheckCloudFinding(selected.id);
      setSelected(updated);
      notify(updated.status === "RESOLVED" ? "Condition no longer observed; finding resolved" : "Condition still present");
      load();
    } catch (err) {
      notify(apiErrorMessage(err, "Re-check failed"), "error");
    } finally {
      setRechecking(false);
    }
  };

  return (
    <>
      <Paper sx={{ p: 3, mb: 3, borderRadius: 4 }}>
        <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
          <TextField
            label="Search title / resource"
            value={filters.search}
            onChange={(e) => setFilters({ ...filters, search: e.target.value })}
            fullWidth
            size="small"
          />
          <TextField
            select
            label="Account"
            value={filters.accountId}
            onChange={(e) => setFilters({ ...filters, accountId: e.target.value })}
            sx={{ minWidth: 180 }}
            size="small"
          >
            <MenuItem value="">All Accounts</MenuItem>
            {accounts.map((a) => (
              <MenuItem key={a.id} value={a.id}>{a.name}</MenuItem>
            ))}
          </TextField>
          <TextField
            select
            label="Severity"
            value={filters.severity}
            onChange={(e) => setFilters({ ...filters, severity: e.target.value })}
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
            value={filters.status}
            onChange={(e) => setFilters({ ...filters, status: e.target.value })}
            sx={{ minWidth: 150 }}
            size="small"
          >
            <MenuItem value="">All Statuses</MenuItem>
            <MenuItem value="OPEN">Open</MenuItem>
            <MenuItem value="RESOLVED">Resolved</MenuItem>
          </TextField>
        </Stack>
      </Paper>

      {loading ? (
        <Box sx={{ display: "flex", justifyContent: "center", mt: 8 }}>
          <CircularProgress />
        </Box>
      ) : loadError ? (
        <Alert severity="error" sx={{ borderRadius: 3 }}>{loadError}</Alert>
      ) : (
        <Paper sx={{ overflow: "hidden", borderRadius: 4 }}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell><b>Finding</b></TableCell>
                <TableCell><b>Account</b></TableCell>
                <TableCell><b>Region</b></TableCell>
                <TableCell><b>Severity</b></TableCell>
                <TableCell><b>Status</b></TableCell>
                <TableCell><b>Last Observed</b></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {findings.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} align="center">
                    No findings match the current filters.
                  </TableCell>
                </TableRow>
              ) : (
                findings.map((f) => (
                  <TableRow key={f.id} hover onClick={() => setSelected(f)} sx={{ cursor: "pointer" }}>
                    <TableCell>
                      <Typography fontWeight={700}>{f.title}</Typography>
                      <Typography variant="caption" color="text.secondary" sx={{ wordBreak: "break-all" }}>
                        {f.findingType} · {f.resourceId}
                      </Typography>
                    </TableCell>
                    <TableCell>{f.cloudAccountName}</TableCell>
                    <TableCell>{f.region || "global"}</TableCell>
                    <TableCell>
                      <Chip label={f.severity} color={severityColor(f.severity)} size="small" />
                    </TableCell>
                    <TableCell>
                      <Chip label={f.status} variant="outlined" color={f.status === "RESOLVED" ? "success" : "default"} size="small" />
                    </TableCell>
                    <TableCell>{formatDateTime(f.lastObservedAt)}</TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </Paper>
      )}

      {/* Finding details */}
      <Dialog open={!!selected && !requestOpen} onClose={() => setSelected(null)} fullWidth maxWidth="md">
        {selected && (
          <>
            <DialogTitle>
              <Stack direction="row" spacing={1} alignItems="center">
                <Chip label={selected.severity} color={severityColor(selected.severity)} size="small" />
                <span>{selected.title}</span>
              </Stack>
            </DialogTitle>
            <DialogContent>
              <Stack spacing={2} mt={1}>
                <Typography variant="body2" color="text.secondary" sx={{ wordBreak: "break-all" }}>
                  {selected.cloudAccountName} · {selected.resourceType} · {selected.resourceId} · {selected.region || "global"}
                </Typography>
                <Box>
                  <Typography variant="subtitle2" fontWeight={700}>Observed (from provider API)</Typography>
                  <Typography variant="body2">{selected.observedFact}</Typography>
                  <Typography variant="caption" color="text.secondary">
                    Source: {selected.source} · First detected {formatDateTime(selected.firstDetectedAt)} · Last observed{" "}
                    {formatDateTime(selected.lastObservedAt)}
                  </Typography>
                </Box>
                <Box>
                  <Typography variant="subtitle2" fontWeight={700}>Assessed risk (platform interpretation)</Typography>
                  <Typography variant="body2">{selected.inferredRisk}</Typography>
                </Box>
                {selected.evidence && Object.keys(selected.evidence).length > 0 && (
                  <Box>
                    <Typography variant="subtitle2" fontWeight={700} gutterBottom>Evidence</Typography>
                    <Table size="small">
                      <TableBody>
                        {Object.entries(selected.evidence).map(([k, v]) => (
                          <TableRow key={k}>
                            <TableCell sx={{ width: 220 }}><code>{k}</code></TableCell>
                            <TableCell><code>{v}</code></TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </Box>
                )}
                {selected.remediationGuidance && (
                  <Typography variant="body2" color="primary.main">
                    Guidance: {selected.remediationGuidance}
                  </Typography>
                )}
                {selected.status === "RESOLVED" && (
                  <Alert severity="success">
                    Resolved {formatDateTime(selected.resolvedAt)}: {selected.resolutionReason}
                  </Alert>
                )}
                {selected.status === "OPEN" && !selected.supportedRemediation && (
                  <Typography variant="body2" color="text.secondary">
                    No automated remediation is available for this finding type. Apply the guidance manually.
                  </Typography>
                )}
              </Stack>
            </DialogContent>
            <DialogActions>
              {canRecheck && (
                <Button onClick={recheck} disabled={rechecking}>
                  {rechecking ? "Re-checking..." : "Re-check Resource"}
                </Button>
              )}
              {canRequest && selected.status === "OPEN" && selected.supportedRemediation && (
                <Button variant="contained" onClick={openRequest}>
                  Request Remediation
                </Button>
              )}
              <Button onClick={() => setSelected(null)}>Close</Button>
            </DialogActions>
          </>
        )}
      </Dialog>

      {/* Request remediation */}
      <Dialog open={requestOpen} onClose={() => setRequestOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Request Remediation</DialogTitle>
        <DialogContent>
          {selected && (
            <Stack spacing={2} mt={1}>
              <Typography>
                <b>{REMEDIATION_LABELS[selected.supportedRemediation] || selected.supportedRemediation}</b> on{" "}
                <code>{selected.resourceId}</code>
              </Typography>
              <Alert severity="warning">
                This is a high-impact change. It must be approved by a different user before anyone can execute it,
                and it will stop any intentional public access to this bucket.
              </Alert>
              <TextField
                label="Justification"
                value={justification}
                onChange={(e) => setJustification(e.target.value)}
                multiline
                rows={3}
                fullWidth
                inputProps={{ maxLength: 1000 }}
              />
            </Stack>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRequestOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={submitRequest} disabled={submitting}>
            {submitting ? "Submitting..." : "Submit for Approval"}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}

export default CloudFindingsTab;

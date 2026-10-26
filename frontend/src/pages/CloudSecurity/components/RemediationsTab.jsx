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
  Step,
  StepLabel,
  Stepper,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from "@mui/material";

import {
  approveRemediation,
  cancelRemediation,
  executeRemediation,
  getRemediations,
  previewRemediation,
  rejectRemediation,
  verifyRemediation,
} from "../../../services/cloudService";
import { apiErrorMessage } from "../../../services/api";
import { getCurrentUser, hasPermission } from "../../../services/authService";
import {
  bucketName,
  formatDateTime,
  REMEDIATION_LABELS,
  REMEDIATION_LIFECYCLE,
  remediationColor,
} from "../cloudUi";

const STATUSES = [
  "PENDING_APPROVAL", "APPROVED", "EXECUTING", "SUCCEEDED", "FAILED", "OUTCOME_UNKNOWN",
  "VERIFIED", "VERIFICATION_FAILED", "REJECTED", "CANCELLED",
];

function RemediationsTab({ capabilities, notify }) {
  const canApprove = hasPermission("REMEDIATION_APPROVE");
  const canExecute = hasPermission("REMEDIATION_EXECUTE");
  const canRequest = hasPermission("REMEDIATION_REQUEST");
  const currentUsername = getCurrentUser()?.username;
  const executionEnabled = !!capabilities?.remediationExecutionEnabled;

  const [items, setItems] = useState([]);
  const [statusFilter, setStatusFilter] = useState("");
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);

  const [selected, setSelected] = useState(null);
  const [preview, setPreview] = useState(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [comment, setComment] = useState("");
  const [working, setWorking] = useState(false);

  const [confirmOpen, setConfirmOpen] = useState(false);
  const [confirmText, setConfirmText] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const page = await getRemediations({ status: statusFilter, page: 0, size: 100 });
      setItems(page.content || []);
    } catch (err) {
      setLoadError(apiErrorMessage(err, "Failed to load remediation requests"));
    } finally {
      setLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => {
    load();
  }, [load]);

  const open = (item) => {
    setSelected(item);
    setPreview(null);
    setComment("");
  };

  const run = async (fn, successMessage) => {
    setWorking(true);
    try {
      const updated = await fn();
      setSelected(updated);
      notify(successMessage(updated));
      load();
    } catch (err) {
      notify(apiErrorMessage(err, "Action failed"), "error");
    } finally {
      setWorking(false);
    }
  };

  const loadPreview = async () => {
    setPreviewLoading(true);
    try {
      setPreview(await previewRemediation(selected.id));
    } catch (err) {
      notify(apiErrorMessage(err, "Preview failed"), "error");
    } finally {
      setPreviewLoading(false);
    }
  };

  const isOwnRequest = selected && selected.requestedBy === currentUsername;
  const canDecide = selected && canApprove && selected.status === "PENDING_APPROVAL" && !isOwnRequest;
  const canRun = selected && canExecute && ["APPROVED", "FAILED"].includes(selected.status);
  const canVerify = selected && (canExecute || canApprove)
    && ["SUCCEEDED", "OUTCOME_UNKNOWN", "VERIFICATION_FAILED"].includes(selected.status);
  const canCancel = selected
    && ["PENDING_APPROVAL", "APPROVED"].includes(selected.status)
    && ((canRequest && isOwnRequest) || canApprove);
  const expectedConfirmation = selected ? bucketName(selected.resourceId) : "";
  const activeStep = selected ? REMEDIATION_LIFECYCLE.indexOf(selected.status) : -1;

  return (
    <>
      <Paper sx={{ p: 3, mb: 3, borderRadius: 4 }}>
        <TextField
          select
          label="Status"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          sx={{ minWidth: 220 }}
          size="small"
        >
          <MenuItem value="">All Statuses</MenuItem>
          {STATUSES.map((s) => (
            <MenuItem key={s} value={s}>{s.replaceAll("_", " ")}</MenuItem>
          ))}
        </TextField>
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
                <TableCell><b>Action</b></TableCell>
                <TableCell><b>Target</b></TableCell>
                <TableCell><b>Requested</b></TableCell>
                <TableCell><b>Decision</b></TableCell>
                <TableCell><b>Status</b></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {items.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={5} align="center">
                    No remediation requests.
                  </TableCell>
                </TableRow>
              ) : (
                items.map((r) => (
                  <TableRow key={r.id} hover onClick={() => open(r)} sx={{ cursor: "pointer" }}>
                    <TableCell>
                      <Typography fontWeight={700}>{REMEDIATION_LABELS[r.actionType] || r.actionType}</Typography>
                      <Typography variant="caption" color="text.secondary">#{r.id} · {r.findingTitle}</Typography>
                    </TableCell>
                    <TableCell sx={{ wordBreak: "break-all" }}>
                      {r.resourceId}
                      <Typography variant="caption" color="text.secondary" display="block">{r.cloudAccountName}</Typography>
                    </TableCell>
                    <TableCell>
                      {r.requestedBy}
                      <Typography variant="caption" color="text.secondary" display="block">{formatDateTime(r.requestedAt)}</Typography>
                    </TableCell>
                    <TableCell>{r.decidedBy ? `${r.decidedBy} · ${formatDateTime(r.decidedAt)}` : "—"}</TableCell>
                    <TableCell>
                      <Chip label={r.status.replaceAll("_", " ")} color={remediationColor(r.status)} size="small" />
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </Paper>
      )}

      {/* Details & workflow actions */}
      <Dialog open={!!selected && !confirmOpen} onClose={() => setSelected(null)} fullWidth maxWidth="md">
        {selected && (
          <>
            <DialogTitle>
              Remediation #{selected.id}: {REMEDIATION_LABELS[selected.actionType] || selected.actionType}
            </DialogTitle>
            <DialogContent>
              <Stack spacing={2} mt={1}>
                <Stepper activeStep={activeStep < 0 ? 0 : activeStep} alternativeLabel>
                  {REMEDIATION_LIFECYCLE.map((s) => (
                    <Step key={s} completed={activeStep > REMEDIATION_LIFECYCLE.indexOf(s)}>
                      <StepLabel>{s.replaceAll("_", " ")}</StepLabel>
                    </Step>
                  ))}
                </Stepper>
                {activeStep < 0 && (
                  <Alert severity={remediationColor(selected.status) === "error" ? "error" : "info"}>
                    Status: {selected.status.replaceAll("_", " ")}
                  </Alert>
                )}

                <Table size="small">
                  <TableBody>
                    <TableRow><TableCell sx={{ width: 200 }}>Target</TableCell><TableCell sx={{ wordBreak: "break-all" }}>{selected.resourceId} ({selected.region || "global"})</TableCell></TableRow>
                    <TableRow><TableCell>Account</TableCell><TableCell>{selected.cloudAccountName}</TableCell></TableRow>
                    <TableRow><TableCell>Requested by</TableCell><TableCell>{selected.requestedBy} · {formatDateTime(selected.requestedAt)}</TableCell></TableRow>
                    {selected.justification && <TableRow><TableCell>Justification</TableCell><TableCell>{selected.justification}</TableCell></TableRow>}
                    <TableRow><TableCell>Validation</TableCell><TableCell>{selected.validationNotes || "—"}</TableCell></TableRow>
                    <TableRow><TableCell>Request hash</TableCell><TableCell><code style={{ wordBreak: "break-all" }}>{selected.requestHash}</code></TableCell></TableRow>
                    {selected.decidedBy && (
                      <TableRow><TableCell>Decision</TableCell><TableCell>{selected.decidedBy} · {formatDateTime(selected.decidedAt)}{selected.decisionComment ? ` · "${selected.decisionComment}"` : ""}</TableCell></TableRow>
                    )}
                    {selected.cancelledBy && (
                      <TableRow><TableCell>Cancelled</TableCell><TableCell>{selected.cancelledBy} · {formatDateTime(selected.cancelledAt)}</TableCell></TableRow>
                    )}
                    {selected.executedBy && (
                      <TableRow>
                        <TableCell>Execution</TableCell>
                        <TableCell>
                          {selected.executedBy} · attempt {selected.executionAttempts} · {formatDateTime(selected.executionStartedAt)}
                          {selected.executionMessage && <Typography variant="body2">{selected.executionMessage}</Typography>}
                          {selected.providerRequestId && <Typography variant="caption" color="text.secondary">Provider request id: {selected.providerRequestId}</Typography>}
                        </TableCell>
                      </TableRow>
                    )}
                    {selected.verificationResult && (
                      <TableRow><TableCell>Verification</TableCell><TableCell>{formatDateTime(selected.verifiedAt)} · {selected.verificationResult}</TableCell></TableRow>
                    )}
                  </TableBody>
                </Table>

                {selected.status === "OUTCOME_UNKNOWN" && (
                  <Alert severity="warning">The provider call did not return a definitive result. Verify the resource state before taking further action.</Alert>
                )}

                <Box>
                  <Button onClick={loadPreview} disabled={previewLoading} variant="outlined" size="small">
                    {previewLoading ? "Loading preview..." : "Preview Change"}
                  </Button>
                  {preview && (
                    <Paper variant="outlined" sx={{ p: 2, mt: 1.5, borderRadius: 3 }}>
                      <Chip
                        size="small"
                        label={preview.previewType === "PROVIDER_DRY_RUN" ? "Provider dry-run" : "Local validation (not a provider dry-run)"}
                        color={preview.previewType === "PROVIDER_DRY_RUN" ? "success" : "default"}
                        sx={{ mb: 1 }}
                      />
                      <Typography variant="body2"><b>Current state:</b> {preview.currentState}</Typography>
                      <Typography variant="body2"><b>Planned change:</b> {preview.plannedChange}</Typography>
                      <Typography variant="caption" color="text.secondary">{preview.notes}</Typography>
                    </Paper>
                  )}
                </Box>

                {selected.status === "PENDING_APPROVAL" && canApprove && isOwnRequest && (
                  <Alert severity="info">You requested this change, so another approver must review it.</Alert>
                )}
                {canDecide && (
                  <TextField
                    label="Decision comment"
                    value={comment}
                    onChange={(e) => setComment(e.target.value)}
                    fullWidth
                    size="small"
                    inputProps={{ maxLength: 1000 }}
                  />
                )}
                {canRun && !executionEnabled && (
                  <Alert severity="info">Execution is disabled by server configuration.</Alert>
                )}
              </Stack>
            </DialogContent>
            <DialogActions>
              {canCancel && (
                <Button color="inherit" disabled={working}
                  onClick={() => run(() => cancelRemediation(selected.id), () => "Request cancelled")}>
                  Cancel Request
                </Button>
              )}
              {canDecide && (
                <>
                  <Button color="error" disabled={working}
                    onClick={() => run(() => rejectRemediation(selected.id, selected.requestHash, comment), () => "Request rejected")}>
                    Reject
                  </Button>
                  <Button variant="contained" color="success" disabled={working}
                    onClick={() => run(() => approveRemediation(selected.id, selected.requestHash, comment), () => "Request approved")}>
                    Approve
                  </Button>
                </>
              )}
              {canRun && (
                <Button variant="contained" color="warning" disabled={working || !executionEnabled}
                  onClick={() => { setConfirmText(""); setConfirmOpen(true); }}>
                  {selected.status === "FAILED" ? "Retry Execution" : "Execute"}
                </Button>
              )}
              {canVerify && (
                <Button variant="outlined" disabled={working}
                  onClick={() => run(() => verifyRemediation(selected.id), (u) => `Verification: ${u.status.replaceAll("_", " ")}`)}>
                  Verify Now
                </Button>
              )}
              <Button onClick={() => setSelected(null)}>Close</Button>
            </DialogActions>
          </>
        )}
      </Dialog>

      {/* Typed execution confirmation */}
      <Dialog open={confirmOpen} onClose={() => setConfirmOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Confirm Execution</DialogTitle>
        <DialogContent>
          <Stack spacing={2} mt={1}>
            <Alert severity="warning">
              This changes a real cloud resource. Type <b>{expectedConfirmation}</b> to confirm.
            </Alert>
            <TextField
              label="Resource name"
              value={confirmText}
              onChange={(e) => setConfirmText(e.target.value)}
              fullWidth
              autoComplete="off"
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            color="warning"
            disabled={working || confirmText.trim() !== expectedConfirmation}
            onClick={async () => {
              setConfirmOpen(false);
              await run(
                () => executeRemediation(selected.id, confirmText.trim()),
                (u) => `Execution finished: ${u.status.replaceAll("_", " ")}`
              );
            }}
          >
            Execute Change
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}

export default RemediationsTab;

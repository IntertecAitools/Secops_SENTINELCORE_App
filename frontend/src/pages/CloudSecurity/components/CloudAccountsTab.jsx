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
  IconButton,
  MenuItem,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import EditIcon from "@mui/icons-material/Edit";
import WifiTetheringIcon from "@mui/icons-material/WifiTethering";
import RadarIcon from "@mui/icons-material/Radar";
import InventoryIcon from "@mui/icons-material/Inventory2";

import {
  createCloudAccount,
  getCloudAccounts,
  getCloudResources,
  getCloudScans,
  runCloudScan,
  updateCloudAccount,
  verifyCloudAccount,
} from "../../../services/cloudService";
import { apiErrorMessage } from "../../../services/api";
import { hasPermission } from "../../../services/authService";
import { connectionColor, formatDateTime, scanColor } from "../cloudUi";

const EMPTY_FORM = {
  name: "",
  provider: "AWS",
  externalAccountId: "",
  authMode: "ASSUME_ROLE",
  roleArn: "",
  externalId: "",
  regions: "us-east-1",
};

function CloudAccountsTab({ capabilities, notify }) {
  const canManage = hasPermission("CLOUD_ACCOUNT_MANAGE");
  const canScan = hasPermission("CLOUD_SCAN_RUN");

  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [busy, setBusy] = useState({});

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);

  const [detail, setDetail] = useState(null);
  const [detailData, setDetailData] = useState({ resources: [], scans: [], loading: false, error: null });

  const load = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      setAccounts((await getCloudAccounts()) || []);
    } catch (err) {
      setLoadError(apiErrorMessage(err, "Failed to load cloud accounts"));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const openCreate = () => {
    setEditing(null);
    setForm(EMPTY_FORM);
    setDialogOpen(true);
  };

  const openEdit = (account) => {
    setEditing(account);
    setForm({
      name: account.name,
      provider: account.provider,
      externalAccountId: account.externalAccountId,
      authMode: account.authMode,
      roleArn: account.roleArn || "",
      externalId: "", // never pre-filled: the server does not return it
      regions: (account.regions || []).join(", "),
    });
    setDialogOpen(true);
  };

  const handleSave = async () => {
    const payload = {
      name: form.name.trim(),
      provider: form.provider,
      externalAccountId: form.externalAccountId.trim(),
      authMode: form.authMode,
      roleArn: form.authMode === "ASSUME_ROLE" ? form.roleArn.trim() : null,
      // Blank on edit keeps the stored value; the value is write-only
      externalId: form.authMode === "ASSUME_ROLE" && form.externalId.trim() ? form.externalId.trim() : null,
      regions: form.regions.split(",").map((r) => r.trim()).filter(Boolean),
    };
    setSaving(true);
    try {
      if (editing) {
        await updateCloudAccount(editing.id, payload);
        notify("Cloud account updated");
      } else {
        await createCloudAccount(payload);
        notify("Cloud account registered");
      }
      setDialogOpen(false);
      setForm(EMPTY_FORM); // clear any submitted ExternalId from component state
      load();
    } catch (err) {
      notify(apiErrorMessage(err, "Failed to save cloud account"), "error");
    } finally {
      setSaving(false);
    }
  };

  const withBusy = async (key, fn) => {
    setBusy((b) => ({ ...b, [key]: true }));
    try {
      await fn();
    } finally {
      setBusy((b) => ({ ...b, [key]: false }));
    }
  };

  const handleVerify = (account) =>
    withBusy(`verify-${account.id}`, async () => {
      try {
        const updated = await verifyCloudAccount(account.id);
        notify(
          updated.connectionStatus === "CONNECTED" ? "Connection verified" : `Connection failed: ${updated.lastVerificationMessage}`,
          updated.connectionStatus === "CONNECTED" ? "success" : "warning"
        );
        load();
      } catch (err) {
        notify(apiErrorMessage(err, "Connection check failed"), "error");
      }
    });

  const handleScan = (account) =>
    withBusy(`scan-${account.id}`, async () => {
      try {
        const scan = await runCloudScan(account.id);
        const summary = `Scan ${scan.status}: ${scan.findingsObserved ?? 0} findings observed, ${scan.findingsResolved ?? 0} resolved`;
        notify(summary, scan.status === "SUCCEEDED" ? "success" : "warning");
        load();
      } catch (err) {
        notify(apiErrorMessage(err, "Scan failed"), "error");
      }
    });

  const openDetail = async (account) => {
    setDetail(account);
    setDetailData({ resources: [], scans: [], loading: true, error: null });
    try {
      const [resources, scans] = await Promise.all([getCloudResources(account.id, 0, 100), getCloudScans(account.id, 0, 10)]);
      setDetailData({ resources: resources.content || [], scans: scans.content || [], loading: false, error: null });
    } catch (err) {
      setDetailData({ resources: [], scans: [], loading: false, error: apiErrorMessage(err, "Failed to load account details") });
    }
  };

  const supportedProviders = capabilities?.supportedProviders || ["AWS"];

  return (
    <>
      <Stack direction="row" justifyContent="flex-end" mb={2}>
        {canManage && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate}>
            Connect Account
          </Button>
        )}
      </Stack>

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
                <TableCell><b>Account</b></TableCell>
                <TableCell><b>Provider / ID</b></TableCell>
                <TableCell><b>Access</b></TableCell>
                <TableCell><b>Regions</b></TableCell>
                <TableCell><b>Connection</b></TableCell>
                <TableCell><b>Last Scan</b></TableCell>
                <TableCell align="center"><b>Actions</b></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {accounts.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} align="center">
                    No cloud accounts connected.
                  </TableCell>
                </TableRow>
              ) : (
                accounts.map((a) => (
                  <TableRow key={a.id} hover>
                    <TableCell>
                      <Typography fontWeight={700}>{a.name}</Typography>
                      <Typography variant="caption" color="text.secondary">Added by {a.createdBy}</Typography>
                    </TableCell>
                    <TableCell>
                      <Chip label={a.provider} size="small" color="secondary" sx={{ fontWeight: "bold", mr: 1 }} />
                      {a.externalAccountId}
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2">{a.authMode === "ASSUME_ROLE" ? "Assume role" : "Platform identity"}</Typography>
                      {a.roleArn && (
                        <Typography variant="caption" color="text.secondary" sx={{ wordBreak: "break-all" }}>
                          {a.roleArn}
                        </Typography>
                      )}
                    </TableCell>
                    <TableCell>{(a.regions || []).join(", ") || "Default"}</TableCell>
                    <TableCell>
                      <Chip label={a.connectionStatus} size="small" color={connectionColor(a.connectionStatus)} variant="outlined" />
                      <Typography variant="caption" color="text.secondary" display="block">
                        {formatDateTime(a.lastVerifiedAt)}
                      </Typography>
                      {a.connectionStatus === "FAILED" && a.lastVerificationMessage && (
                        <Typography variant="caption" color="error.main" display="block" sx={{ maxWidth: 260 }}>
                          {a.lastVerificationMessage}
                        </Typography>
                      )}
                    </TableCell>
                    <TableCell>{formatDateTime(a.lastScanAt)}</TableCell>
                    <TableCell align="center">
                      <Stack direction="row" spacing={1} justifyContent="center">
                        <Tooltip title="Inventory & scan history">
                          <IconButton color="primary" onClick={() => openDetail(a)}>
                            <InventoryIcon />
                          </IconButton>
                        </Tooltip>
                        {canManage && (
                          <Tooltip title="Verify connection (read-only)">
                            <span>
                              <IconButton color="primary" onClick={() => handleVerify(a)} disabled={!!busy[`verify-${a.id}`]}>
                                {busy[`verify-${a.id}`] ? <CircularProgress size={20} /> : <WifiTetheringIcon />}
                              </IconButton>
                            </span>
                          </Tooltip>
                        )}
                        {canScan && (
                          <Tooltip title="Run read-only scan">
                            <span>
                              <IconButton color="primary" onClick={() => handleScan(a)} disabled={!!busy[`scan-${a.id}`]}>
                                {busy[`scan-${a.id}`] ? <CircularProgress size={20} /> : <RadarIcon />}
                              </IconButton>
                            </span>
                          </Tooltip>
                        )}
                        {canManage && (
                          <Tooltip title="Edit">
                            <IconButton color="primary" onClick={() => openEdit(a)}>
                              <EditIcon />
                            </IconButton>
                          </Tooltip>
                        )}
                      </Stack>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </Paper>
      )}

      {/* Connect / edit account */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>{editing ? "Edit Cloud Account" : "Connect Cloud Account"}</DialogTitle>
        <DialogContent>
          <Alert severity="info" sx={{ mt: 1, borderRadius: 3 }}>
            Never enter access keys or root credentials. SentinelCore uses its own identity and, optionally,
            assumes a read-only IAM role in your account.
          </Alert>
          <Stack spacing={3} mt={2}>
            <TextField label="Display Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} fullWidth required />
            <Stack direction="row" spacing={2}>
              <TextField
                select
                label="Provider"
                value={form.provider}
                onChange={(e) => setForm({ ...form, provider: e.target.value })}
                sx={{ minWidth: 140 }}
                disabled={!!editing}
              >
                {supportedProviders.map((p) => (
                  <MenuItem key={p} value={p}>{p}</MenuItem>
                ))}
              </TextField>
              <TextField
                label="AWS Account ID (12 digits)"
                value={form.externalAccountId}
                onChange={(e) => setForm({ ...form, externalAccountId: e.target.value })}
                fullWidth
                required
                disabled={!!editing}
              />
            </Stack>
            <TextField
              select
              label="Access Method"
              value={form.authMode}
              onChange={(e) => setForm({ ...form, authMode: e.target.value })}
              fullWidth
            >
              <MenuItem value="ASSUME_ROLE">Assume IAM role (recommended)</MenuItem>
              <MenuItem value="PLATFORM_DEFAULT">Platform identity (same account)</MenuItem>
            </TextField>
            {form.authMode === "ASSUME_ROLE" && (
              <>
                <TextField
                  label="Role ARN"
                  placeholder="arn:aws:iam::123456789012:role/SentinelCoreReadOnly"
                  value={form.roleArn}
                  onChange={(e) => setForm({ ...form, roleArn: e.target.value })}
                  fullWidth
                  required
                />
                <TextField
                  label="External ID (optional)"
                  type="password"
                  autoComplete="off"
                  helperText={
                    editing?.externalIdConfigured
                      ? "An External ID is configured. Leave blank to keep it."
                      : "Shared value required by the role's trust policy, if any."
                  }
                  value={form.externalId}
                  onChange={(e) => setForm({ ...form, externalId: e.target.value })}
                  fullWidth
                />
              </>
            )}
            <TextField
              label="Regions (comma-separated)"
              helperText="Used for regional services such as EC2 security groups."
              value={form.regions}
              onChange={(e) => setForm({ ...form, regions: e.target.value })}
              fullWidth
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleSave} variant="contained" disabled={saving}>
            {saving ? "Saving..." : "Save"}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Inventory & scans */}
      <Dialog open={!!detail} onClose={() => setDetail(null)} fullWidth maxWidth="md">
        <DialogTitle>{detail?.name}: Inventory & Scan History</DialogTitle>
        <DialogContent>
          {detailData.loading ? (
            <Box sx={{ display: "flex", justifyContent: "center", my: 4 }}>
              <CircularProgress />
            </Box>
          ) : detailData.error ? (
            <Alert severity="error">{detailData.error}</Alert>
          ) : (
            <Stack spacing={3} mt={1}>
              <Box>
                <Typography variant="h6" fontWeight={700} gutterBottom>Recent Scans</Typography>
                {detailData.scans.length === 0 ? (
                  <Typography color="text.secondary">No scans yet.</Typography>
                ) : (
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell><b>Started</b></TableCell>
                        <TableCell><b>Status</b></TableCell>
                        <TableCell><b>Resources</b></TableCell>
                        <TableCell><b>Findings</b></TableCell>
                        <TableCell><b>By</b></TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {detailData.scans.map((s) => (
                        <TableRow key={s.id}>
                          <TableCell>{formatDateTime(s.startedAt)}</TableCell>
                          <TableCell>
                            <Chip label={s.status} size="small" color={scanColor(s.status)} />
                            {s.errorSummary && (
                              <Typography variant="caption" color="text.secondary" display="block" sx={{ whiteSpace: "pre-line", maxWidth: 320 }}>
                                {s.errorSummary}
                              </Typography>
                            )}
                          </TableCell>
                          <TableCell>{s.resourcesObserved ?? "—"}</TableCell>
                          <TableCell>
                            {s.findingsObserved ?? "—"} observed / {s.findingsResolved ?? 0} resolved
                          </TableCell>
                          <TableCell>{s.triggeredBy}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                )}
              </Box>
              <Box>
                <Typography variant="h6" fontWeight={700} gutterBottom>Resources</Typography>
                {detailData.resources.length === 0 ? (
                  <Typography color="text.secondary">No resources recorded. Run a scan to build the inventory.</Typography>
                ) : (
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell><b>Type</b></TableCell>
                        <TableCell><b>Name</b></TableCell>
                        <TableCell><b>Region</b></TableCell>
                        <TableCell><b>Last Seen</b></TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {detailData.resources.map((r) => (
                        <TableRow key={r.id}>
                          <TableCell>{r.resourceType}</TableCell>
                          <TableCell>
                            {r.name}
                            <Typography variant="caption" color="text.secondary" display="block" sx={{ wordBreak: "break-all" }}>
                              {r.resourceId}
                            </Typography>
                          </TableCell>
                          <TableCell>{r.region}</TableCell>
                          <TableCell>{formatDateTime(r.lastSeenAt)}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                )}
              </Box>
            </Stack>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDetail(null)}>Close</Button>
        </DialogActions>
      </Dialog>
    </>
  );
}

export default CloudAccountsTab;

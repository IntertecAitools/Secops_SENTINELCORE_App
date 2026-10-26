import { useCallback, useEffect, useState } from "react";
import { Alert, Box, CircularProgress, Paper, Stack, Tab, Tabs, Typography } from "@mui/material";

import CloudAccountsTab from "./components/CloudAccountsTab";
import CloudFindingsTab from "./components/CloudFindingsTab";
import RemediationsTab from "./components/RemediationsTab";
import AppSnackbar from "../../components/common/AppSnackbar";
import { getCloudCapabilities } from "../../services/cloudService";
import { apiErrorMessage } from "../../services/api";

function CloudSecurity() {
  const [tab, setTab] = useState(0);
  const [capabilities, setCapabilities] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [snackbar, setSnackbar] = useState({ open: false, message: "", severity: "success" });

  const notify = useCallback((message, severity = "success") => {
    setSnackbar({ open: true, message, severity });
  }, []);

  useEffect(() => {
    const load = async () => {
      try {
        setCapabilities(await getCloudCapabilities());
      } catch (err) {
        setError(err?.response?.status === 403
          ? "Your role does not have access to cloud security data."
          : apiErrorMessage(err, "Cloud security service is unavailable."));
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  return (
    <Box p={3}>
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4" fontWeight={700}>
          Cloud Security
        </Typography>
      </Stack>

      {loading && (
        <Box sx={{ display: "flex", justifyContent: "center", mt: 8 }}>
          <CircularProgress />
        </Box>
      )}

      {!loading && error && (
        <Alert severity="error" sx={{ borderRadius: 3 }}>
          {error}
        </Alert>
      )}

      {!loading && capabilities && (
        <>
          <Stack spacing={1.5} mb={3}>
            {!capabilities.awsIntegrationEnabled && (
              <Alert severity="info" sx={{ borderRadius: 3 }}>
                AWS integration is disabled on this server. Accounts can be registered, but connection checks,
                scans and previews of live state are unavailable until an administrator enables it.
              </Alert>
            )}
            {!capabilities.remediationExecutionEnabled && (
              <Alert severity="info" sx={{ borderRadius: 3 }}>
                Remediation execution is disabled by configuration. Requests can be proposed and approved, but no
                change will be made to cloud resources.
              </Alert>
            )}
            {capabilities.knownButUnsupportedProviders?.length > 0 && (
              <Typography variant="body2" color="text.secondary">
                Supported providers: {capabilities.supportedProviders.join(", ")}. Not yet supported:{" "}
                {capabilities.knownButUnsupportedProviders.join(", ")}.
              </Typography>
            )}
          </Stack>

          <Paper sx={{ borderRadius: 4, mb: 3 }}>
            <Tabs value={tab} onChange={(e, v) => setTab(v)} sx={{ px: 2 }}>
              <Tab label="Accounts" />
              <Tab label="Findings" />
              <Tab label="Remediations" />
            </Tabs>
          </Paper>

          {tab === 0 && <CloudAccountsTab capabilities={capabilities} notify={notify} />}
          {tab === 1 && <CloudFindingsTab notify={notify} />}
          {tab === 2 && <RemediationsTab capabilities={capabilities} notify={notify} />}
        </>
      )}

      <AppSnackbar
        open={snackbar.open}
        message={snackbar.message}
        severity={snackbar.severity}
        handleClose={() => setSnackbar((s) => ({ ...s, open: false }))}
      />
    </Box>
  );
}

export default CloudSecurity;

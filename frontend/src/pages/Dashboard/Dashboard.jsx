import { useEffect, useState } from "react";
import { Grid, Box, Typography, Paper, Stack, List, ListItem, ListItemIcon, ListItemText } from "@mui/material";

import {
  Computer,
  Warning,
  NotificationsActive,
  BugReport,
  TrendingUp,
  History,
} from "@mui/icons-material";

import WelcomeBanner from "../../components/dashboard/WelcomeBanner";
import StatCard from "../../components/cards/StatCard";
import OverviewChart from "../../components/charts/OverviewChart";
import AssetDistributionChart from "../../components/dashboard/AssetDistributionChart";
import RecentAlerts from "../../components/cards/RecentAlerts";
import RecentAssets from "../../components/dashboard/RecentAssets";

import { getDashboardStats } from "../../services/dashboardService";
import { getUserRole } from "../../services/authService";

function Dashboard() {
  const role = getUserRole();
  const [stats, setStats] = useState({
    totalAssets: 0,
    healthyAssets: 0,
    runningAssets: 0,
    warningAssets: 0,
    criticalAssets: 0,
    totalAlerts: 0,
    criticalAlerts: 0,
    totalIncidents: 0,
    openIncidents: 0,
    totalVulnerabilities: 0,
    criticalVulnerabilities: 0,
    recentAlerts: [],
    recentActivities: [],
  });

  useEffect(() => {
    loadDashboard();
  }, []);

  const loadDashboard = async () => {
    try {
      const data = await getDashboardStats();
      if (data) {
        setStats(data);
      }
    } catch (error) {
      console.error("Dashboard loading failed:", error);
    }
  };

  // Determine section visibility based on role
  const showAssetsCard = ["SUPER_ADMIN", "ADMIN", "ASSET_MANAGER", "AUDITOR"].includes(role);
  const showAlertsCard = ["SUPER_ADMIN", "ADMIN", "SECURITY_ANALYST", "INCIDENT_MANAGER", "AUDITOR"].includes(role);
  const showIncidentsCard = ["SUPER_ADMIN", "ADMIN", "SECURITY_ANALYST", "INCIDENT_MANAGER", "AUDITOR"].includes(role);
  const showVulnerabilitiesCard = ["SUPER_ADMIN", "ADMIN", "SECURITY_ANALYST", "VULNERABILITY_MANAGER", "AUDITOR"].includes(role);

  const showOverviewChart = ["SUPER_ADMIN", "ADMIN", "SECURITY_ANALYST", "AUDITOR"].includes(role);
  const showRecentAlerts = ["SUPER_ADMIN", "ADMIN", "SECURITY_ANALYST", "INCIDENT_MANAGER", "AUDITOR"].includes(role);
  const showAssetDistribution = ["SUPER_ADMIN", "ADMIN", "ASSET_MANAGER", "AUDITOR"].includes(role);
  const showActivityLog = ["SUPER_ADMIN", "AUDITOR"].includes(role);
  const showRecentAssetsList = ["SUPER_ADMIN", "ADMIN", "ASSET_MANAGER", "AUDITOR"].includes(role);

  return (
    <Box sx={{ p: 1 }}>
      <WelcomeBanner />

      {/* Role-specific notification message for USER role */}
      {role === "USER" && (
        <Paper sx={{ p: 4, mt: 3, borderRadius: 3, bgcolor: "background.paper" }}>
          <Typography variant="h5" fontWeight={700} gutterBottom>
            Welcome to the Employee Portal
          </Typography>
          <Typography variant="body1" color="text.secondary" paragraph>
            You are currently logged in with general **USER** access credentials. You can view and manage your profile settings, configure theme preferences, and toggle email notifications.
          </Typography>
          <Typography variant="body2" color="primary.main" fontWeight="bold">
            Please use the navigation bar to access the profile settings.
          </Typography>
        </Paper>
      )}

      {/* Stats Counter Cards Grid */}
      <Grid container spacing={3} sx={{ mt: 3 }}>
        {showAssetsCard && (
          <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
            <StatCard
              title="Infrastructure Assets"
              value={stats.totalAssets}
              icon={<Computer />}
              color="#1976D2"
            />
          </Grid>
        )}

        {showAlertsCard && (
          <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
            <StatCard
              title="Alerts Tracked"
              value={stats.totalAlerts}
              icon={<Warning />}
              color="#D32F2F"
            />
          </Grid>
        )}

        {showIncidentsCard && (
          <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
            <StatCard
              title="Open Incidents"
              value={stats.openIncidents}
              icon={<NotificationsActive />}
              color="#FB8C00"
            />
          </Grid>
        )}

        {showVulnerabilitiesCard && (
          <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
            <StatCard
              title="Vulnerabilities Found"
              value={stats.totalVulnerabilities}
              icon={<BugReport />}
              color="#E65100"
            />
          </Grid>
        )}
      </Grid>

      {/* Main Charts & Feeds Section */}
      <Grid container spacing={3} sx={{ mt: 3 }}>
        {showOverviewChart && (
          <Grid size={{ xs: 12, lg: showRecentAlerts ? 8 : 12 }}>
            <OverviewChart />
          </Grid>
        )}

        {showRecentAlerts && (
          <Grid size={{ xs: 12, lg: showOverviewChart ? 4 : 12 }}>
            <RecentAlerts alerts={stats.recentAlerts} />
          </Grid>
        )}
      </Grid>

      {/* Secondary Distribution & Activity Section */}
      <Grid container spacing={3} sx={{ mt: 3 }}>
        {showAssetDistribution && (
          <Grid size={{ xs: 12, lg: showActivityLog ? 4 : 12 }}>
            <AssetDistributionChart />
          </Grid>
        )}

        {showActivityLog && (
          <Grid size={{ xs: 12, lg: showAssetDistribution ? 8 : 12 }}>
            <Paper
              elevation={0}
              sx={{
                p: 3,
                borderRadius: 5,
                border: "1px solid",
                borderColor: "divider",
                height: "100%",
              }}
            >
              <Stack direction="row" spacing={1} alignItems="center" mb={2}>
                <History color="primary" />
                <Typography variant="h6" fontWeight={700}>
                  Live Platform Activity Log
                </Typography>
              </Stack>

              {stats.recentActivities?.length === 0 ? (
                <Typography color="text.secondary" p={4} align="center">
                  No administrative actions logged yet.
                </Typography>
              ) : (
                <List>
                  {stats.recentActivities?.map((activity) => (
                    <ListItem key={activity.id} sx={{ px: 0, py: 1.2 }}>
                      <ListItemIcon>
                        <TrendingUp color="primary" />
                      </ListItemIcon>
                      <ListItemText
                        primary={
                          <Typography fontWeight={600}>
                            {activity.action}
                          </Typography>
                        }
                        secondary={
                          <Typography variant="body2" color="text.secondary">
                            {activity.username} at {activity.ipAddress} - {activity.details}
                          </Typography>
                        }
                      />
                    </ListItem>
                  ))}
                </List>
              )}
            </Paper>
          </Grid>
        )}
      </Grid>

      {/* Live Assets List Grid */}
      {showRecentAssetsList && (
        <Grid container spacing={3} sx={{ mt: 3 }}>
          <Grid size={12}>
            <RecentAssets />
          </Grid>
        </Grid>
      )}
    </Box>
  );
}

export default Dashboard;
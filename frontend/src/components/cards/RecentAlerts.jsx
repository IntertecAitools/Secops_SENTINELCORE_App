import {
  Paper,
  Typography,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Avatar,
  Chip,
  Divider,
  Box,
} from "@mui/material";

import {
  BugReport,
  WarningAmber,
  Security,
  SystemUpdate,
} from "@mui/icons-material";

const getIcon = (category) => {
  switch (category?.toLowerCase()) {
    case "malware":
      return <Security />;
    case "vulnerability":
    case "cve":
      return <BugReport />;
    case "unauthorized":
    case "auth":
      return <WarningAmber />;
    default:
      return <SystemUpdate />;
  }
};

const getColor = (severity) => {
  switch (severity?.toUpperCase()) {
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

function RecentAlerts({ alerts = [] }) {
  return (
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
      <Typography
        variant="h6"
        fontWeight={700}
        mb={2}
      >
        Recent Security Alerts
      </Typography>

      {alerts.length === 0 ? (
        <Typography color="text.secondary" p={2} align="center">
          No security alerts recorded.
        </Typography>
      ) : (
        <List disablePadding>
          {alerts.map((alert, index) => (
            <Box key={alert.id || index}>
              <ListItem
                sx={{ py: 1.5 }}
                secondaryAction={
                  <Chip
                    label={alert.severity}
                    color={getColor(alert.severity)}
                    size="small"
                  />
                }
              >
                <ListItemAvatar>
                  <Avatar
                    sx={{
                      bgcolor: `${getColor(alert.severity)}.main`,
                    }}
                  >
                    {getIcon(alert.category)}
                  </Avatar>
                </ListItemAvatar>

                <ListItemText
                  primary={
                    <Typography fontWeight={600} noWrap>
                      {alert.title}
                    </Typography>
                  }
                  secondary={alert.category || "General"}
                />
              </ListItem>

              {index !== alerts.length - 1 && <Divider />}
            </Box>
          ))}
        </List>
      )}
    </Paper>
  );
}

export default RecentAlerts;
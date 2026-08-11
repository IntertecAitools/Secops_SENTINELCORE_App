import { Box, Paper, Typography, Grid, Button, Stack, Card, CardContent } from "@mui/material";
import AssessmentIcon from "@mui/icons-material/Assessment";
import DownloadIcon from "@mui/icons-material/Download";
import TableChartIcon from "@mui/icons-material/TableChart";
import ArticleIcon from "@mui/icons-material/Article";

import {
  downloadAssetsCSV,
  downloadAlertsCSV,
  downloadAssetsExcel,
  downloadIncidentsExcel,
  downloadIncidentsPDF,
} from "../../services/reportService";

function Reports() {

  const handleDownload = async (actionFn, title) => {
    try {
      await actionFn();
    } catch (err) {
      console.error(err);
      alert(`Failed to download ${title}`);
    }
  };

  const reportCards = [
    {
      title: "Assets Inventory Sheet",
      desc: "Export complete asset list containing host IP address records, locations, and operating systems.",
      actions: [
        { label: "Download CSV", fn: downloadAssetsCSV, color: "secondary" },
        { label: "Download Excel (XLSX)", fn: downloadAssetsExcel, color: "primary" },
      ],
      icon: <TableChartIcon fontSize="large" color="primary" />
    },
    {
      title: "Security Alerts Audit",
      desc: "Export logged alerts with severity flags, descriptions, categories, and target assets.",
      actions: [
        { label: "Download CSV", fn: downloadAlertsCSV, color: "secondary" },
      ],
      icon: <AssessmentIcon fontSize="large" color="warning" />
    },
    {
      title: "Incidents Resolution log",
      desc: "Export investigations logs, specialists assignments, and resolutions notes.",
      actions: [
        { label: "Download PDF Brief", fn: downloadIncidentsPDF, color: "error" },
        { label: "Download Excel", fn: downloadIncidentsExcel, color: "primary" },
      ],
      icon: <ArticleIcon fontSize="large" color="error" />
    }
  ];

  return (
    <Box p={3}>
      <Typography variant="h4" fontWeight={700} mb={3}>
        Reports & Export Center
      </Typography>

      <Grid container spacing={3}>
        {reportCards.map((card, i) => (
          <Grid size={{ xs: 12, md: 4 }} key={i}>
            <Card sx={{ height: "100%", display: "flex", flexDirection: "column", justifyContent: "space-between", border: "1px solid", borderColor: "divider" }} elevation={0}>
              <CardContent>
                <Stack direction="row" spacing={2} alignItems="center" mb={2}>
                  {card.icon}
                  <Typography variant="h6" fontWeight={700}>
                    {card.title}
                  </Typography>
                </Stack>
                <Typography color="text.secondary" variant="body2" mb={3}>
                  {card.desc}
                </Typography>
              </CardContent>
              <Box p={3} pt={0}>
                <Stack spacing={1.5}>
                  {card.actions.map((act, idx) => (
                    <Button
                      key={idx}
                      variant="contained"
                      color={act.color}
                      startIcon={<DownloadIcon />}
                      onClick={() => handleDownload(act.fn, card.title)}
                      fullWidth
                    >
                      {act.label}
                    </Button>
                  ))}
                </Stack>
              </Box>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Box>
  );
}

export default Reports;
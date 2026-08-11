import { useState, useEffect } from "react";
import { Box, Typography, Button, Container, Paper, Stack, CircularProgress } from "@mui/material";
import SecurityIcon from "@mui/icons-material/Security";
import ShieldIcon from "@mui/icons-material/Shield";
import StorageIcon from "@mui/icons-material/Storage";
import ReportProblemIcon from "@mui/icons-material/ReportProblem";
import GppGoodIcon from "@mui/icons-material/GppGood";
import { Link } from "react-router-dom";
import SecuritySimulation3D from "../../components/common/SecuritySimulation3D";
import CursorEffect from "../../components/common/CursorEffect";

function LandingPage() {
  const [loadingProgress, setLoadingProgress] = useState(0);
  const [loadingCompleted, setLoadingCompleted] = useState(false);

  useEffect(() => {
    const timer = setInterval(() => {
      setLoadingProgress((prev) => {
        if (prev >= 100) {
          clearInterval(timer);
          setTimeout(() => {
            setLoadingCompleted(true);
          }, 300);
          return 100;
        }
        const step = Math.random() * 25 + 5;
        return Math.min(prev + step, 100);
      });
    }, 120);

    return () => clearInterval(timer);
  }, []);

  const features = [
    {
      title: "Asset Management",
      desc: "Live visibility and operating system tracking across all enterprise hardware and cloud assets.",
      icon: <StorageIcon fontSize="large" color="primary" />,
    },
    {
      title: "Real-time Security Alerts",
      desc: "Automatic SQL Injection, malware detection, and unauthorized attempt alerts tracking.",
      icon: <ReportProblemIcon fontSize="large" color="warning" />,
    },
    {
      title: "Incident Tracker",
      desc: "Investigate and resolve severe platform breaches using role-assigned workflow queues.",
      icon: <ShieldIcon fontSize="large" color="secondary" />,
    },
    {
      title: "Vulnerability Management",
      desc: "Track CVE indexes and dynamic CVSS scores to deploy critical remediating systems.",
      icon: <GppGoodIcon fontSize="large" color="success" />,
    },
  ];

  if (!loadingCompleted) {
    return (
      <Box
        sx={{
          bgcolor: "#0D1117",
          color: "#00C853",
          height: "100vh",
          display: "flex",
          flexDirection: "column",
          justifyContent: "center",
          alignItems: "center",
          fontFamily: "'Courier New', Courier, monospace",
          px: 3,
        }}
      >
        <Stack spacing={4} alignItems="center" sx={{ textAlign: "center" }}>
          <SecurityIcon sx={{ fontSize: 70, color: "#00C853", animation: "pulse 2s infinite" }} />
          <Typography variant="h5" fontWeight="bold" letterSpacing={2}>
            INITIALIZING SECUREOPS CORE...
          </Typography>
          <Box sx={{ width: 280, bgcolor: "#161B22", height: 8, borderRadius: 4, overflow: "hidden", border: "1px solid #30363d" }}>
            <Box sx={{ width: `${loadingProgress}%`, bgcolor: "#00C853", height: "100%", transition: "width 0.15s linear" }} />
          </Box>
          <Typography variant="body1" sx={{ color: "text.secondary" }}>
            SYS_LOAD: {Math.round(loadingProgress)}%
          </Typography>
        </Stack>
      </Box>
    );
  }

  return (
    <Box
      sx={{
        bgcolor: "#0D1117",
        color: "#F8FAFC",
        minHeight: "100vh",
        display: "flex",
        flexDirection: "column",
        justifyContent: "center",
        py: 6,
      }}
    >
      <CursorEffect />
      <Container maxWidth="lg">
        {/* Main Grid: Info Section vs Graphics Preview */}
        <Box
          sx={{
            display: "grid",
            gridTemplateColumns: { xs: "1fr", md: "1.2fr 1fr" },
            gap: 6,
            alignItems: "center",
            mb: 8,
          }}
        >
          {/* Info Section */}
          <Stack spacing={4}>
            <Stack direction="row" spacing={1.5} alignItems="center">
              <SecurityIcon color="primary" sx={{ fontSize: 44 }} />
              <Typography variant="h6" fontWeight="bold" color="primary.main" letterSpacing={1}>
                SENTINELCORE SECUREOPS
              </Typography>
            </Stack>

            <Typography variant="h2" fontWeight={850} lineHeight={1.15} sx={{ letterSpacing: "-1px" }}>
              Enterprise Security & Infrastructure Monitoring
            </Typography>

            <Typography variant="h6" color="text.secondary" fontWeight={400} sx={{ lineHeight: 1.6 }}>
              SentinelCore SecureOps protects organizational infrastructure with audit logging, threat intelligence tracking, live vulnerability management, and real-time incident responses.
            </Typography>

            <Box>
              <Button
                component={Link}
                to="/login"
                variant="contained"
                color="primary"
                size="large"
                sx={{
                  px: 5,
                  py: 1.8,
                  fontSize: "1.15rem",
                  borderRadius: 3,
                  fontWeight: 700,
                  boxShadow: "0 4px 20px rgba(0, 200, 83, 0.3)",
                  "&:hover": {
                    boxShadow: "0 6px 24px rgba(0, 200, 83, 0.4)",
                  },
                }}
              >
                Enter Portal
              </Button>
            </Box>
          </Stack>

          {/* Graphics Section - 3D Simulation */}
          <Box sx={{ width: "100%" }}>
            <SecuritySimulation3D />
          </Box>
        </Box>

        {/* Feature Cards Grid (Two beside, and next two beside below) */}
        <Typography variant="h5" fontWeight={800} mb={4} sx={{ borderLeft: "4px solid #00C853", pl: 2 }}>
          Core Protection Modules
        </Typography>

        <Box
          sx={{
            display: "grid",
            gridTemplateColumns: { xs: "1fr", sm: "1fr 1fr" },
            gap: 3,
          }}
        >
          {features.map((feature, i) => (
            <Paper
              key={i}
              sx={{
                p: 4,
                borderRadius: 4,
                bgcolor: "#161B22",
                border: "1px solid #30363d",
                minHeight: 180,
                display: "flex",
                flexDirection: "column",
                justifyContent: "space-between",
                transition: "transform 0.2s, box-shadow 0.2s, border-color 0.2s",
                "&:hover": {
                  transform: "translateY(-4px)",
                  borderColor: "#00C853",
                  boxShadow: "0 6px 20px rgba(0,200,83,0.08)",
                },
              }}
            >
              <Box>
                {feature.icon}
                <Typography variant="h6" fontWeight="bold" mt={2} gutterBottom>
                  {feature.title}
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.5 }}>
                  {feature.desc}
                </Typography>
              </Box>
            </Paper>
          ))}
        </Box>
      </Container>
    </Box>
  );
}

export default LandingPage;

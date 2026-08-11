import { Box, Typography } from "@mui/material";
import SecurityIcon from "@mui/icons-material/Security";
import BugReportIcon from "@mui/icons-material/BugReport";
import WarningIcon from "@mui/icons-material/Warning";
import BarChartIcon from "@mui/icons-material/BarChart";
import SettingsSuggestIcon from "@mui/icons-material/SettingsSuggest";

function SecuritySimulation3D() {
  return (
    <Box
      sx={{
        position: "relative",
        width: "100%",
        height: "100%",
        minHeight: 400,
        bgcolor: "#0A0E17",
        borderRadius: 5,
        border: "1px solid #1E293B",
        overflow: "hidden",
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        boxShadow: "inset 0 0 50px rgba(0, 200, 83, 0.05), 0 10px 40px rgba(0,0,0,0.6)",
      }}
    >
      {/* Dynamic Scan Grid Background */}
      <Box
        sx={{
          position: "absolute",
          top: 0,
          left: 0,
          width: "100%",
          height: "100%",
          backgroundImage:
            "linear-gradient(rgba(30, 41, 59, 0.15) 1px, transparent 1px), linear-gradient(90deg, rgba(30, 41, 59, 0.15) 1px, transparent 1px)",
          backgroundSize: "20px 20px",
          opacity: 0.8,
        }}
      />

      {/* Orbit Rings (3D Tilted Ellipses) */}
      <Box
        sx={{
          position: "absolute",
          width: 320,
          height: 140,
          border: "1.5px dashed rgba(59, 130, 246, 0.25)",
          borderRadius: "50%",
          transform: "rotateX(65deg) rotateY(-15deg)",
          pointerEvents: "none",
        }}
      />
      <Box
        sx={{
          position: "absolute",
          width: 440,
          height: 180,
          border: "1.5px dashed rgba(245, 158, 11, 0.2)",
          borderRadius: "50%",
          transform: "rotateX(65deg) rotateY(15deg)",
          pointerEvents: "none",
        }}
      />
      <Box
        sx={{
          position: "absolute",
          width: 540,
          height: 220,
          border: "1.5px dashed rgba(239, 68, 68, 0.15)",
          borderRadius: "50%",
          transform: "rotateX(65deg) rotateY(-5deg)",
          pointerEvents: "none",
        }}
      />

      {/* Central Server Rack & Computer Graphic */}
      <Box
        sx={{
          position: "relative",
          zIndex: 5,
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          animation: "floatCenter 4s ease-in-out infinite",
        }}
      >
        <svg width="120" height="130" viewBox="0 0 120 130" fill="none">
          {/* Base Platform */}
          <path d="M10 95 L60 115 L110 95 L60 75 Z" fill="#1E293B" stroke="#334155" strokeWidth="2" />
          <path d="M10 95 L10 102 L60 122 L60 115 Z" fill="#0F172A" />
          <path d="M110 95 L110 102 L60 122 L60 115 Z" fill="#1E293B" />

          {/* Server Column */}
          <rect x="35" y="20" width="50" height="70" rx="3" fill="#1E293B" stroke="#00C853" strokeWidth="2" />
          
          {/* Glowing Console Lines */}
          <line x1="42" y1="32" x2="78" y2="32" stroke="#334155" strokeWidth="3" strokeLinecap="round" />
          <line x1="42" y1="44" x2="68" y2="44" stroke="#334155" strokeWidth="3" strokeLinecap="round" />
          <line x1="42" y1="56" x2="78" y2="56" stroke="#334155" strokeWidth="3" strokeLinecap="round" />
          <line x1="42" y1="68" x2="58" y2="68" stroke="#334155" strokeWidth="3" strokeLinecap="round" />

          {/* LED status points */}
          <circle cx="80" cy="44" r="2.5" fill="#EF4444" className="led-blink-red" />
          <circle cx="80" cy="68" r="2.5" fill="#00C853" className="led-blink-green" />
          <circle cx="68" cy="68" r="2.5" fill="#3B82F6" className="led-blink-blue" />
        </svg>
        <Typography
          variant="caption"
          sx={{
            mt: 1,
            color: "#00C853",
            fontFamily: "monospace",
            fontWeight: "bold",
            letterSpacing: 1.5,
            textShadow: "0 0 10px rgba(0,200,83,0.5)",
          }}
        >
          CORE_SERVER
        </Typography>
      </Box>

      {/* Orbiting Objects */}
      {/* Object 1: THREATS (Revolves on inner track) */}
      <Box
        className="orbiting-node"
        sx={{
          position: "absolute",
          animation: "revolveInner 9s linear infinite",
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          gap: 0.5,
          zIndex: 6,
        }}
      >
        <Box
          sx={{
            width: 38,
            height: 38,
            borderRadius: "50%",
            bgcolor: "rgba(239, 68, 68, 0.15)",
            border: "2px solid #EF4444",
            display: "flex",
            justifyContent: "center",
            alignItems: "center",
            boxShadow: "0 0 15px rgba(239, 68, 68, 0.4)",
          }}
        >
          <BugReportIcon sx={{ color: "#EF4444", fontSize: 20 }} />
        </Box>
        <Typography variant="caption" sx={{ fontSize: 9, color: "#EF4444", fontWeight: "bold", fontFamily: "monospace" }}>
          THREAT
        </Typography>
      </Box>

      {/* Object 2: ALERTS (Revolves on mid track) */}
      <Box
        className="orbiting-node"
        sx={{
          position: "absolute",
          animation: "revolveMid 12s linear infinite",
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          gap: 0.5,
          zIndex: 7,
        }}
      >
        <Box
          sx={{
            width: 38,
            height: 38,
            borderRadius: "50%",
            bgcolor: "rgba(245, 158, 11, 0.15)",
            border: "2px solid #F59E0B",
            display: "flex",
            justifyContent: "center",
            alignItems: "center",
            boxShadow: "0 0 15px rgba(245, 158, 11, 0.4)",
          }}
        >
          <WarningIcon sx={{ color: "#F59E0B", fontSize: 20 }} />
        </Box>
        <Typography variant="caption" sx={{ fontSize: 9, color: "#F59E0B", fontWeight: "bold", fontFamily: "monospace" }}>
          ALERT
        </Typography>
      </Box>

      {/* Object 3: DIAGNOSIS (Revolves on outer track) */}
      <Box
        className="orbiting-node"
        sx={{
          position: "absolute",
          animation: "revolveOuter 16s linear infinite",
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          gap: 0.5,
          zIndex: 8,
        }}
      >
        <Box
          sx={{
            width: 38,
            height: 38,
            borderRadius: "50%",
            bgcolor: "rgba(59, 130, 246, 0.15)",
            border: "2px solid #3B82F6",
            display: "flex",
            justifyContent: "center",
            alignItems: "center",
            boxShadow: "0 0 15px rgba(59, 130, 246, 0.4)",
          }}
        >
          <BarChartIcon sx={{ color: "#3B82F6", fontSize: 20 }} />
        </Box>
        <Typography variant="caption" sx={{ fontSize: 9, color: "#3B82F6", fontWeight: "bold", fontFamily: "monospace" }}>
          DIAGNOSIS
        </Typography>
      </Box>

      {/* Object 4: SOLUTIONS (Revolves opposite on outer track) */}
      <Box
        className="orbiting-node"
        sx={{
          position: "absolute",
          animation: "revolveSolutions 14s linear infinite",
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          gap: 0.5,
          zIndex: 9,
        }}
      >
        <Box
          sx={{
            width: 38,
            height: 38,
            borderRadius: "50%",
            bgcolor: "rgba(0, 200, 83, 0.15)",
            border: "2px solid #00C853",
            display: "flex",
            justifyContent: "center",
            alignItems: "center",
            boxShadow: "0 0 15px rgba(0, 200, 83, 0.4)",
          }}
        >
          <SettingsSuggestIcon sx={{ color: "#00C853", fontSize: 20 }} />
        </Box>
        <Typography variant="caption" sx={{ fontSize: 9, color: "#00C853", fontWeight: "bold", fontFamily: "monospace" }}>
          SOLUTION
        </Typography>
      </Box>

      {/* Styled Keyframes & Animations Block */}
      <style>{`
        @keyframes floatCenter {
          0%, 100% { transform: translateY(0); }
          50% { transform: translateY(-10px); }
        }
        
        .orbiting-node {
          transition: transform 0.3s ease, filter 0.3s ease;
          cursor: pointer;
        }

        .orbiting-node:hover {
          filter: brightness(1.3) drop-shadow(0 0 10px rgba(0, 200, 83, 0.6));
        }

        .led-blink-red { animation: blinkRed 1s infinite alternate; }
        .led-blink-green { animation: blinkGreen 0.7s infinite alternate; }
        .led-blink-blue { animation: blinkBlue 1.3s infinite alternate; }

        @keyframes blinkRed { 0% { opacity: 0.3; } 100% { opacity: 1; fill: #EF4444; filter: drop-shadow(0 0 2px #EF4444); } }
        @keyframes blinkGreen { 0% { opacity: 0.3; } 100% { opacity: 1; fill: #00C853; filter: drop-shadow(0 0 2px #00C853); } }
        @keyframes blinkBlue { 0% { opacity: 0.3; } 100% { opacity: 1; fill: #3B82F6; filter: drop-shadow(0 0 2px #3B82F6); } }

        /* Outer Orbit paths matching tilted coordinates */
        @keyframes revolveInner {
          0% { transform: rotate(0deg) translate(145px) rotate(0deg); }
          100% { transform: rotate(360deg) translate(145px) rotate(-360deg); }
        }
        @keyframes revolveMid {
          0% { transform: rotate(120deg) translate(200px) rotate(-120deg); }
          100% { transform: rotate(480deg) translate(200px) rotate(-480deg); }
        }
        @keyframes revolveOuter {
          0% { transform: rotate(240deg) translate(250px) rotate(-240deg); }
          100% { transform: rotate(600deg) translate(250px) rotate(-600deg); }
        }
        @keyframes revolveSolutions {
          0% { transform: rotate(360deg) translate(230px) rotate(-360deg); }
          100% { transform: rotate(0deg) translate(230px) rotate(0deg); }
        }
      `}</style>
    </Box>
  );
}

export default SecuritySimulation3D;

import { Box } from "@mui/material";
import { useLocation } from "react-router-dom";
import Navbar from "../components/layout/Navbar";
import SentinelChatbot from "../components/common/SentinelChatbot";
import FibreArc from "../components/originkit/ui/fibre-arc";

function MainLayout({ children }) {
  const location = useLocation();

  return (
    <Box
      sx={{
        minHeight: "100vh",
        position: "relative",
        overflow: "hidden",
      }}
    >
      <FibreArc />

      <Box
        sx={{
          position: "fixed",
          inset: 0,
          pointerEvents: "none",
          zIndex: 0,
        }}
      />

      <Box
        sx={{
          position: "relative",
          zIndex: 1,
          pointerEvents: "auto",
          minHeight: "100vh",
          bgcolor: "rgba(22, 27, 34, 0.85)",
          backdropFilter: "blur(8px)",
        }}
      >
        <Navbar />

        <Box
          key={location.pathname}
          className="page-fade-in"
          component="main"
          sx={{
            width: "100%",
            px: 2,
            py: 3,
          }}
        >
          {children}
        </Box>

        {/* Floating Cyber Security AI Assistant */}
        <SentinelChatbot />
      </Box>
    </Box>
  );
}

export default MainLayout;
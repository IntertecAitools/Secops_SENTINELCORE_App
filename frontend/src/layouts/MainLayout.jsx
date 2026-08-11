import { Box } from "@mui/material";
import { useLocation } from "react-router-dom";
import Navbar from "../components/layout/Navbar";

function MainLayout({ children }) {
  const location = useLocation();

  return (
    <Box
      sx={{
        minHeight: "100vh",
        bgcolor: "background.default",
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
    </Box>
  );
}

export default MainLayout;
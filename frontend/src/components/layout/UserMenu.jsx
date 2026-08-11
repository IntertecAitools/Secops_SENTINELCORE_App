import { useState } from "react";
import {
  Avatar,
  Box,
  Typography,
  Menu,
  MenuItem,
  Divider,
  ListItemIcon,
} from "@mui/material";

import PersonIcon from "@mui/icons-material/Person";
import SettingsIcon from "@mui/icons-material/Settings";
import LogoutIcon from "@mui/icons-material/Logout";

import { useNavigate } from "react-router-dom";
import {
  logout,
  getCurrentUser,
} from "../../services/authService";

function UserMenu() {
  const navigate = useNavigate();

  const user = getCurrentUser();

  const [anchorEl, setAnchorEl] = useState(null);

  const open = Boolean(anchorEl);

  const handleOpen = (event) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  const fullName = user?.fullName || "Guest";
  const role = user?.role || "USER";

  return (
    <>
      <Box
        onClick={handleOpen}
        sx={{
          display: "flex",
          alignItems: "flex-start",
          gap: 1,
          cursor: "pointer",
          px: 1.5,
          py: 0.8,
          borderRadius: 3,
          transition: ".2s",
          "&:hover": {
            bgcolor: "action.hover",
          },
        }}
      >
        <Avatar
          sx={{
            width: 42,
            height: 42,
            bgcolor: "success.main",
            fontWeight: "bold",
          }}
        >
          {fullName.charAt(0).toUpperCase()}
        </Avatar>

        <Box
          sx={{
            display: "flex",
            flexDirection: "column",
            justifyContent: "center",
            lineHeight: 1.2,
            pt: 0.3,
          }}
        >
          <Typography
            sx={{
              fontWeight: 700,
              fontSize: "0.95rem",
            }}
          >
            {fullName}
          </Typography>

          <Typography
            variant="caption"
            color="text.secondary"
            sx={{
              textAlign: "left",
              textTransform: "capitalize",
            }}
          >
            {role}
          </Typography>
        </Box>
      </Box>

      <Menu
        anchorEl={anchorEl}
        open={open}
        onClose={handleClose}
        PaperProps={{
          sx: {
            mt: 1,
            width: 220,
            borderRadius: 3,
          },
        }}
      >
        <MenuItem
          onClick={() => {
            handleClose();
            navigate("/profile");
          }}
        >
          <ListItemIcon>
            <PersonIcon fontSize="small" />
          </ListItemIcon>
          Profile
        </MenuItem>

        <MenuItem
          onClick={() => {
            handleClose();
            navigate("/settings");
          }}
        >
          <ListItemIcon>
            <SettingsIcon fontSize="small" />
          </ListItemIcon>
          Settings
        </MenuItem>

        <Divider />

        <MenuItem
          onClick={() => {
            handleClose();
            handleLogout();
          }}
        >
          <ListItemIcon>
            <LogoutIcon
              color="error"
              fontSize="small"
            />
          </ListItemIcon>
          Logout
        </MenuItem>
      </Menu>
    </>
  );
}

export default UserMenu;
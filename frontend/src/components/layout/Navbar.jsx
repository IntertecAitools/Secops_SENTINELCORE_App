import { useState } from "react";
import {
  AppBar,
  Toolbar,
  Box,
  IconButton,
  Badge,
  Button,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
} from "@mui/material";

import {
  Notifications,
  DarkMode,
  LightMode,
  Dashboard,
  Storage,
  Group,
  Warning,
  Assessment,
  Settings,
  Shield,
  KeyboardArrowDown,
  BugReport,
  Hub,
  FlashOn,
  Security,
  FormatListBulleted,
} from "@mui/icons-material";

import { Link, useLocation } from "react-router-dom";

import Logo from "./Logo";
import SearchBar from "./SearchBar";
import UserMenu from "./UserMenu";

import { useThemeContext } from "../../context/ThemeContext";
import { getUserRole } from "../../services/authService";

function Navbar() {
  const { mode, toggleTheme } = useThemeContext();
  const location = useLocation();

  // Dropdown States
  const [securityAnchorEl, setSecurityAnchorEl] = useState(null);
  const [adminAnchorEl, setAdminAnchorEl] = useState(null);

  const handleSecurityOpen = (event) => setSecurityAnchorEl(event.currentTarget);
  const handleSecurityClose = () => setSecurityAnchorEl(null);

  const handleAdminOpen = (event) => setAdminAnchorEl(event.currentTarget);
  const handleAdminClose = () => setAdminAnchorEl(null);

  const coreItems = [
    {
      text: "Dashboard",
      path: "/dashboard",
      icon: <Dashboard fontSize="small" />,
      allowedRoles: ["SUPER_ADMIN", "ADMIN", "SECURITY_ANALYST", "INCIDENT_MANAGER", "VULNERABILITY_MANAGER", "ASSET_MANAGER", "AUDITOR"],
    },
    {
      text: "Assets",
      path: "/assets",
      icon: <Storage fontSize="small" />,
      allowedRoles: ["SUPER_ADMIN", "ADMIN", "SECURITY_ANALYST", "ASSET_MANAGER", "AUDITOR"],
    },
    {
      text: "Alerts",
      path: "/alerts",
      icon: <Warning fontSize="small" />,
      allowedRoles: ["SUPER_ADMIN", "ADMIN", "SECURITY_ANALYST", "INCIDENT_MANAGER", "AUDITOR"],
    },
    {
      text: "Incidents",
      path: "/incidents",
      icon: <Shield fontSize="small" />,
      allowedRoles: ["SUPER_ADMIN", "ADMIN", "SECURITY_ANALYST", "INCIDENT_MANAGER", "AUDITOR"],
    },
  ];

  const securityItems = [
    {
      text: "Vulnerabilities",
      path: "/vulnerabilities",
      icon: <BugReport fontSize="small" color="error" />,
      allowedRoles: ["SUPER_ADMIN", "ADMIN", "SECURITY_ANALYST", "VULNERABILITY_MANAGER", "AUDITOR"],
    },
    {
      text: "Threat Intel",
      path: "/threat-intel",
      icon: <Hub fontSize="small" color="warning" />,
      allowedRoles: ["SUPER_ADMIN", "SECURITY_ANALYST", "AUDITOR"],
    },
    {
      text: "Risk Assessment",
      path: "/risk-assessment",
      icon: <Security fontSize="small" color="primary" />,
      allowedRoles: ["SUPER_ADMIN", "VULNERABILITY_MANAGER", "AUDITOR"],
    },
    {
      text: "Patch Management",
      path: "/patch-management",
      icon: <FlashOn fontSize="small" color="success" />,
      allowedRoles: ["SUPER_ADMIN", "VULNERABILITY_MANAGER"],
    },
  ];

  const adminItems = [
    {
      text: "User Management",
      path: "/users",
      icon: <Group fontSize="small" />,
      allowedRoles: ["SUPER_ADMIN", "ADMIN"],
    },
    {
      text: "Audit Logs",
      path: "/audit-logs",
      icon: <FormatListBulleted fontSize="small" />,
      allowedRoles: ["SUPER_ADMIN", "AUDITOR"],
    },
    {
      text: "Reports & Analytics",
      path: "/reports",
      icon: <Assessment fontSize="small" />,
      allowedRoles: ["SUPER_ADMIN", "ADMIN", "SECURITY_ANALYST", "INCIDENT_MANAGER", "AUDITOR"],
    },
    {
      text: "System Settings",
      path: "/settings",
      icon: <Settings fontSize="small" />,
      allowedRoles: ["SUPER_ADMIN", "ADMIN", "SECURITY_ANALYST", "INCIDENT_MANAGER", "VULNERABILITY_MANAGER", "ASSET_MANAGER", "AUDITOR", "USER"],
    },
  ];

  const userRole = getUserRole();

  const filteredCoreItems = coreItems.filter(item => !item.allowedRoles || item.allowedRoles.includes(userRole));
  const filteredSecurityItems = securityItems.filter(item => !item.allowedRoles || item.allowedRoles.includes(userRole));
  const filteredAdminItems = adminItems.filter(item => !item.allowedRoles || item.allowedRoles.includes(userRole));

  const isPathActive = (path) => location.pathname === path;
  const isGroupActive = (items) => items.some(item => location.pathname === item.path);

  return (
    <AppBar
      position="sticky"
      elevation={0}
      sx={{
        bgcolor: mode === "dark" ? "rgba(22, 27, 34, 0.85)" : "rgba(255, 255, 255, 0.85)",
        backdropFilter: "blur(12px)",
        color: "text.primary",
        borderBottom: "1px solid",
        borderColor: "divider",
        transition: "all 0.3s ease",
      }}
    >
      <Toolbar
        sx={{
          minHeight: 68,
          px: 4,
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
        }}
      >
        {/* LEFT */}
        <Box display="flex" alignItems="center" gap={4}>
          <Logo />

          <Box display="flex" alignItems="center" gap={1}>
            {filteredCoreItems.map((item) => (
              <Button
                key={item.text}
                component={Link}
                to={item.path}
                startIcon={item.icon}
                variant={isPathActive(item.path) ? "contained" : "text"}
                color={isPathActive(item.path) ? "primary" : "inherit"}
                sx={{
                  borderRadius: 3,
                  px: 2,
                  textTransform: "none",
                  fontWeight: 700,
                }}
              >
                {item.text}
              </Button>
            ))}

            {/* SECURITY OPERATIONS DROPDOWN */}
            {filteredSecurityItems.length > 0 && (
              <>
                <Button
                  endIcon={<KeyboardArrowDown />}
                  onClick={handleSecurityOpen}
                  variant={isGroupActive(filteredSecurityItems) ? "contained" : "text"}
                  color={isGroupActive(filteredSecurityItems) ? "primary" : "inherit"}
                  sx={{
                    borderRadius: 3,
                    px: 2,
                    textTransform: "none",
                    fontWeight: 700,
                  }}
                >
                  Security Ops
                </Button>
                <Menu
                  anchorEl={securityAnchorEl}
                  open={Boolean(securityAnchorEl)}
                  onClose={handleSecurityClose}
                  disableScrollLock
                  PaperProps={{
                    sx: { mt: 1, minWidth: 200, borderRadius: 3 }
                  }}
                >
                  {filteredSecurityItems.map((item) => (
                    <MenuItem
                      key={item.text}
                      component={Link}
                      to={item.path}
                      onClick={handleSecurityClose}
                      selected={isPathActive(item.path)}
                    >
                      <ListItemIcon>{item.icon}</ListItemIcon>
                      <ListItemText primary={item.text} />
                    </MenuItem>
                  ))}
                </Menu>
              </>
            )}

            {/* ADMINISTRATION DROPDOWN */}
            {filteredAdminItems.length > 0 && (
              <>
                <Button
                  endIcon={<KeyboardArrowDown />}
                  onClick={handleAdminOpen}
                  variant={isGroupActive(filteredAdminItems) ? "contained" : "text"}
                  color={isGroupActive(filteredAdminItems) ? "primary" : "inherit"}
                  sx={{
                    borderRadius: 3,
                    px: 2,
                    textTransform: "none",
                    fontWeight: 700,
                  }}
                >
                  Administration
                </Button>
                <Menu
                  anchorEl={adminAnchorEl}
                  open={Boolean(adminAnchorEl)}
                  onClose={handleAdminClose}
                  disableScrollLock
                  PaperProps={{
                    sx: { mt: 1, minWidth: 220, borderRadius: 3 }
                  }}
                >
                  {filteredAdminItems.map((item) => (
                    <MenuItem
                      key={item.text}
                      component={Link}
                      to={item.path}
                      onClick={handleAdminClose}
                      selected={isPathActive(item.path)}
                    >
                      <ListItemIcon>{item.icon}</ListItemIcon>
                      <ListItemText primary={item.text} />
                    </MenuItem>
                  ))}
                </Menu>
              </>
            )}
          </Box>
        </Box>

        {/* RIGHT */}
        <Box display="flex" alignItems="center" gap={2}>
          <SearchBar />

          <Badge badgeContent={2} color="error">
            <IconButton
              component={Link}
              to="/alerts"
              sx={{
                bgcolor: "action.hover",
                width: 42,
                height: 42,
                "&:hover": {
                  bgcolor: "action.selected",
                },
              }}
            >
              <Notifications />
            </IconButton>
          </Badge>

          <IconButton onClick={toggleTheme}>
            {mode === "dark" ? <LightMode /> : <DarkMode />}
          </IconButton>

          <UserMenu />
        </Box>
      </Toolbar>
    </AppBar>
  );
}

export default Navbar;
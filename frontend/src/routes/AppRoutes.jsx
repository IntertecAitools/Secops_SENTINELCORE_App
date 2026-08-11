import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

import MainLayout from "../layouts/MainLayout";

import LandingPage from "../pages/LandingPage/LandingPage";
import Dashboard from "../pages/Dashboard/Dashboard";
import Assets from "../pages/Assets/Assets";
import Users from "../pages/Users";
import Login from "../pages/Login/Login";
import Alerts from "../pages/Alerts/Alerts";
import Incidents from "../pages/Incidents/Incidents";
import Vulnerabilities from "../pages/Vulnerabilities/Vulnerabilities";
import ThreatIntel from "../pages/ThreatIntel/ThreatIntel";
import RiskAssessment from "../pages/RiskAssessment/RiskAssessment";
import PatchManagement from "../pages/PatchManagement/PatchManagement";
import AuditLogs from "../pages/AuditLogs/AuditLogs";
import Reports from "../pages/Reports/Reports";
import Settings from "../pages/Settings/Settings";
import Profile from "../pages/Profile/Profile";

import ProtectedRoute from "../components/auth/ProtectedRoute";
import RoleProtectedRoute from "../components/auth/RoleProtectedRoute";

function AppRoutes() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Landing Page */}
        <Route path="/" element={<LandingPage />} />

        {/* Login */}
        <Route path="/login" element={<Login />} />

        {/* Protected Routes */}
        <Route
          path="/*"
          element={
            <ProtectedRoute>
              <MainLayout>
                <Routes>
                  {/* Base redirect inside portal */}
                  <Route path="" element={<Navigate to="dashboard" replace />} />

                  <Route
                    path="dashboard"
                    element={
                      <RoleProtectedRoute allowedRoles={["SUPER_ADMIN", "ADMIN", "SECURITY_ANALYST", "INCIDENT_MANAGER", "VULNERABILITY_MANAGER", "ASSET_MANAGER", "AUDITOR"]}>
                        <Dashboard />
                      </RoleProtectedRoute>
                    }
                  />
                  <Route
                    path="assets"
                    element={
                      <RoleProtectedRoute allowedRoles={["SUPER_ADMIN", "ADMIN", "SECURITY_ANALYST", "ASSET_MANAGER", "AUDITOR"]}>
                        <Assets />
                      </RoleProtectedRoute>
                    }
                  />
                  <Route
                    path="users"
                    element={
                      <RoleProtectedRoute allowedRoles={["SUPER_ADMIN", "ADMIN"]}>
                        <Users />
                      </RoleProtectedRoute>
                    }
                  />
                  <Route
                    path="alerts"
                    element={
                      <RoleProtectedRoute allowedRoles={["SUPER_ADMIN", "ADMIN", "SECURITY_ANALYST", "INCIDENT_MANAGER", "AUDITOR"]}>
                        <Alerts />
                      </RoleProtectedRoute>
                    }
                  />
                  <Route
                    path="incidents"
                    element={
                      <RoleProtectedRoute allowedRoles={["SUPER_ADMIN", "ADMIN", "SECURITY_ANALYST", "INCIDENT_MANAGER", "AUDITOR"]}>
                        <Incidents />
                      </RoleProtectedRoute>
                    }
                  />
                  <Route
                    path="vulnerabilities"
                    element={
                      <RoleProtectedRoute allowedRoles={["SUPER_ADMIN", "ADMIN", "SECURITY_ANALYST", "VULNERABILITY_MANAGER", "AUDITOR"]}>
                        <Vulnerabilities />
                      </RoleProtectedRoute>
                    }
                  />
                  <Route
                    path="threat-intel"
                    element={
                      <RoleProtectedRoute allowedRoles={["SUPER_ADMIN", "SECURITY_ANALYST", "AUDITOR"]}>
                        <ThreatIntel />
                      </RoleProtectedRoute>
                    }
                  />
                  <Route
                    path="risk-assessment"
                    element={
                      <RoleProtectedRoute allowedRoles={["SUPER_ADMIN", "VULNERABILITY_MANAGER", "AUDITOR"]}>
                        <RiskAssessment />
                      </RoleProtectedRoute>
                    }
                  />
                  <Route
                    path="patch-management"
                    element={
                      <RoleProtectedRoute allowedRoles={["SUPER_ADMIN", "VULNERABILITY_MANAGER"]}>
                        <PatchManagement />
                      </RoleProtectedRoute>
                    }
                  />
                  <Route
                    path="audit-logs"
                    element={
                      <RoleProtectedRoute allowedRoles={["SUPER_ADMIN", "AUDITOR"]}>
                        <AuditLogs />
                      </RoleProtectedRoute>
                    }
                  />
                  <Route
                    path="reports"
                    element={
                      <RoleProtectedRoute allowedRoles={["SUPER_ADMIN", "ADMIN", "SECURITY_ANALYST", "INCIDENT_MANAGER", "AUDITOR"]}>
                        <Reports />
                      </RoleProtectedRoute>
                    }
                  />
                  <Route
                    path="settings"
                    element={
                      <RoleProtectedRoute allowedRoles={["SUPER_ADMIN", "ADMIN", "SECURITY_ANALYST", "INCIDENT_MANAGER", "VULNERABILITY_MANAGER", "ASSET_MANAGER", "AUDITOR", "USER"]}>
                        <Settings />
                      </RoleProtectedRoute>
                    }
                  />
                  <Route
                    path="profile"
                    element={
                      <RoleProtectedRoute allowedRoles={["SUPER_ADMIN", "ADMIN", "SECURITY_ANALYST", "INCIDENT_MANAGER", "VULNERABILITY_MANAGER", "ASSET_MANAGER", "AUDITOR", "USER"]}>
                        <Profile />
                      </RoleProtectedRoute>
                    }
                  />
                  <Route
                    path="profile/:id"
                    element={
                      <RoleProtectedRoute allowedRoles={["SUPER_ADMIN", "ADMIN", "SECURITY_ANALYST", "INCIDENT_MANAGER", "VULNERABILITY_MANAGER", "ASSET_MANAGER", "AUDITOR", "USER"]}>
                        <Profile />
                      </RoleProtectedRoute>
                    }
                  />
                  <Route
                    path="users/:id"
                    element={
                      <RoleProtectedRoute allowedRoles={["SUPER_ADMIN", "ADMIN", "SECURITY_ANALYST", "INCIDENT_MANAGER", "VULNERABILITY_MANAGER", "ASSET_MANAGER", "AUDITOR", "USER"]}>
                        <Profile />
                      </RoleProtectedRoute>
                    }
                  />
                  <Route
                    path="*"
                    element={
                      <h2 style={{ padding: 30 }}>
                        404 - Page Not Found
                      </h2>
                    }
                  />
                </Routes>
              </MainLayout>
            </ProtectedRoute>
          }
        />
      </Routes>
    </BrowserRouter>
  );
}

export default AppRoutes;
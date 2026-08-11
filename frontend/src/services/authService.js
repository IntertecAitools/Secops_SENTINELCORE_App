import API from "./api";

const TOKEN_KEY = "sentinelcore_token";
const USER_KEY = "sentinelcore_user";
const ROLE_KEY = "sentinelcore_role";
const PERMISSIONS_KEY = "sentinelcore_permissions";

const decodeToken = (token) => {
  try {
    const base64Url = token.split(".")[1];
    let base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    while (base64.length % 4) {
      base64 += "=";
    }
    const jsonPayload = decodeURIComponent(
      window
        .atob(base64)
        .split("")
        .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
        .join("")
    );
    return JSON.parse(jsonPayload);
  } catch (error) {
    console.error("JWT decoding failed:", error);
    return null;
  }
};

export const login = async (username, password) => {
  try {
    const response = await API.post("/auth/login", {
      username,
      password,
    });

    const data = response.data;
    localStorage.setItem(TOKEN_KEY, data.token);

    // Decode token to extract role & permissions
    const claims = decodeToken(data.token);
    const role = claims?.role || data.role;
    const authorities = claims?.authorities || [];

    localStorage.setItem(ROLE_KEY, role);
    localStorage.setItem(PERMISSIONS_KEY, JSON.stringify(authorities));

    localStorage.setItem(
      USER_KEY,
      JSON.stringify({
        username: data.username,
        fullName: data.fullName,
        role: role,
      })
    );

    return {
      success: true,
      user: data,
      role: role,
    };
  } catch (error) {
    return {
      success: false,
      message: error.response?.data?.message || "Invalid username or password",
    };
  }
};

export const register = async (user) => {
  const response = await API.post("/auth/register", user);
  return response.data;
};

export const logout = () => {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  localStorage.removeItem(ROLE_KEY);
  localStorage.removeItem(PERMISSIONS_KEY);
};

export const getToken = () => {
  return localStorage.getItem(TOKEN_KEY);
};

export const getCurrentUser = () => {
  const user = localStorage.getItem(USER_KEY);
  return user ? JSON.parse(user) : null;
};

export const getUserRole = () => {
  const role = localStorage.getItem(ROLE_KEY);
  return role ? role.replace("ROLE_", "").trim().toUpperCase() : "";
};

export const getUserPermissions = () => {
  const perms = localStorage.getItem(PERMISSIONS_KEY);
  return perms ? JSON.parse(perms) : [];
};

export const hasPermission = (permission) => {
  const perms = getUserPermissions();
  return perms.includes(permission);
};

export const hasAnyRole = (roles) => {
  const userRole = getUserRole();
  return roles.includes(userRole);
};

export const isAuthenticated = () => {
  return !!localStorage.getItem(TOKEN_KEY);
};
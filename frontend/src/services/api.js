import axios from "axios";

const API = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api",
  headers: {
    "Content-Type": "application/json",
  },
});

// Request Interceptor: Attach token dynamically
API.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("sentinelcore_token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

const isAuthEndpoint = (config) => (config?.url || "").startsWith("/auth/");

// Response Interceptor: a 401 means the session is missing or expired, so log out.
// A 403 means the user is authenticated but lacks permission for that one action: keep the
// session and let the calling page show the error.
API.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401 && !isAuthEndpoint(error.config)) {
      console.warn("Session expired or invalid. Logging out user...");
      localStorage.removeItem("sentinelcore_token");
      localStorage.removeItem("sentinelcore_user");
      localStorage.removeItem("sentinelcore_role");
      localStorage.removeItem("sentinelcore_permissions");

      // Avoid infinite redirects if already on login page
      if (!window.location.pathname.includes("/login")) {
        window.location.href = "/login?session_expired=true";
      }
    }
    return Promise.reject(error);
  }
);

/** User-safe message from an API error (backend returns { success, message, data }). */
export const apiErrorMessage = (error, fallback = "Request failed") => {
  if (error?.response?.status === 403) {
    return error.response.data?.message || "You do not have permission to perform this action";
  }
  return error?.response?.data?.message || fallback;
};

export default API;

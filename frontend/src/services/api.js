import axios from "axios";

const API = axios.create({
  baseURL: "http://localhost:8080/api",
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

// Response Interceptor: Catch 401 & 403 errors and force logout
API.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      console.warn("Unauthorized or Forbidden request. Logging out user...");
      localStorage.removeItem("sentinelcore_token");
      localStorage.removeItem("sentinelcore_user");
      
      // Avoid infinite redirects if already on login page
      if (!window.location.pathname.includes("/login")) {
        window.location.href = "/login?session_expired=true";
      }
    }
    return Promise.reject(error);
  }
);

export default API;

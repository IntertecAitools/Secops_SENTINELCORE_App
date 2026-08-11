import API from "./api";

export const getAlerts = async (search = "", severity = "", status = "", page = 0, size = 10) => {
  const response = await API.get("/alerts", {
    params: { search, severity, status, page, size }
  });
  return response.data.data;
};

export const getAlertById = async (id) => {
  const response = await API.get(`/alerts/${id}`);
  return response.data.data;
};

export const createAlert = async (alert) => {
  const response = await API.post("/alerts", alert);
  return response.data.data;
};

export const updateAlert = async (id, alert) => {
  const response = await API.put(`/alerts/${id}`, alert);
  return response.data.data;
};

export const deleteAlert = async (id) => {
  await API.delete(`/alerts/${id}`);
};

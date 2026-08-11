import API from "./api";

export const getIncidents = async (search = "", severity = "", status = "", page = 0, size = 10) => {
  const response = await API.get("/incidents", {
    params: { search, severity, status, page, size }
  });
  return response.data.data;
};

export const getIncidentById = async (id) => {
  const response = await API.get(`/incidents/${id}`);
  return response.data.data;
};

export const createIncident = async (incident) => {
  const response = await API.post("/incidents", incident);
  return response.data.data;
};

export const updateIncident = async (id, incident) => {
  const response = await API.put(`/incidents/${id}`, incident);
  return response.data.data;
};

export const deleteIncident = async (id) => {
  await API.delete(`/incidents/${id}`);
};

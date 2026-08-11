import API from "./api";

export const getPatches = async (search = "", status = "", page = 0, size = 100) => {
  const response = await API.get("/patches", {
    params: { search, status, page, size }
  });
  return response.data.data;
};

export const getPatchById = async (id) => {
  const response = await API.get(`/patches/${id}`);
  return response.data.data;
};

export const createPatch = async (patch) => {
  const response = await API.post("/patches", patch);
  return response.data.data;
};

export const updatePatch = async (id, patch) => {
  const response = await API.put(`/patches/${id}`, patch);
  return response.data.data;
};

export const applyPatch = async (id) => {
  const response = await API.post(`/patches/${id}/apply`);
  return response.data.data;
};

export const deletePatch = async (id) => {
  await API.delete(`/patches/${id}`);
};

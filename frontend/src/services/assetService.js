import API from "./api";

export const getAllAssets = async (search = "", status = "", page = 0, size = 100) => {
  const response = await API.get("/assets", {
    params: { search, status, page, size }
  });
  return response.data.data.content || response.data.data;
};

export const createAsset = async (asset) => {
  const response = await API.post("/assets", asset);
  return response.data.data;
};

export const updateAsset = async (id, asset) => {
  const response = await API.put(`/assets/${id}`, asset);
  return response.data.data;
};

export const deleteAsset = async (id) => {
  await API.delete(`/assets/${id}`);
};
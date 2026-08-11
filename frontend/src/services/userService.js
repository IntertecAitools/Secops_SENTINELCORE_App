import API from "./api";

export const getAllUsers = async () => {
  const response = await API.get("/users");
  return response.data.data.content || response.data.data;
};

export const getUsers = async (search = "", role = "", status = "", page = 0, size = 10, sortBy = "username", sortDir = "asc") => {
  const response = await API.get("/users", {
    params: {
      search,
      role,
      status,
      page,
      size,
      sortBy,
      sortDir,
    },
  });
  return response.data.data;
};

export const getUserById = async (id) => {
  const response = await API.get(`/users/${id}`);
  return response.data.data;
};

export const createUser = async (user) => {
  const response = await API.post("/users", user);
  return response.data.data;
};

export const updateUser = async (id, user) => {
  const response = await API.put(`/users/${id}`, user);
  return response.data.data;
};

export const deleteUser = async (id) => {
  await API.delete(`/users/${id}`);
};

export const toggleUserStatus = async (id) => {
  const response = await API.put(`/users/${id}/toggle-status`);
  return response.data.data;
};

export const resetUserPassword = async (id, newPassword) => {
  const response = await API.put(`/users/${id}/reset-password`, { newPassword });
  return response.data.data;
};

export const getProfile = async () => {
  const response = await API.get("/users/profile");
  return response.data.data;
};

export const updateProfile = async (profile) => {
  const response = await API.put("/users/profile", profile);
  return response.data.data;
};

export const changeOwnPassword = async (currentPassword, newPassword) => {
  const response = await API.put("/users/profile/change-password", { currentPassword, newPassword });
  return response.data;
};
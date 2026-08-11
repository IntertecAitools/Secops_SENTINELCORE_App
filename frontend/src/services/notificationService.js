import API from "./api";

export const getNotifications = async (all = false) => {
  const response = await API.get("/notifications", {
    params: { all }
  });
  return response.data.data;
};

export const markAsRead = async (id) => {
  await API.put(`/notifications/${id}/read`);
};

export const markAllAsRead = async () => {
  await API.put("/notifications/read-all");
};

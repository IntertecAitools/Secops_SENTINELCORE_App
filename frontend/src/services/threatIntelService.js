import API from "./api";

export const getThreatFeeds = async (search = "", indicatorType = "", confidenceLevel = "", page = 0, size = 10) => {
  const response = await API.get("/threat-intel", {
    params: { search, indicatorType, confidenceLevel, page, size }
  });
  return response.data.data;
};

export const getThreatFeedById = async (id) => {
  const response = await API.get(`/threat-intel/${id}`);
  return response.data.data;
};

export const createThreatFeed = async (feed) => {
  const response = await API.post("/threat-intel", feed);
  return response.data.data;
};

export const updateThreatFeed = async (id, feed) => {
  const response = await API.put(`/threat-intel/${id}`, feed);
  return response.data.data;
};

export const deleteThreatFeed = async (id) => {
  await API.delete(`/threat-intel/${id}`);
};

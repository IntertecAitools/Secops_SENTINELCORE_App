import API from "./api";

export const getAuditLogs = async (search = "", page = 0, size = 50) => {
  const response = await API.get("/audit-logs", {
    params: { search, page, size }
  });
  return response.data.data;
};

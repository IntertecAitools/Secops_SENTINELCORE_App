import API from "./api";

// Cloud security API client. Contains no credentials: cloud access is configured on the server.

export const getCloudCapabilities = async () => {
  const response = await API.get("/cloud/capabilities");
  return response.data.data;
};

export const getCloudAccounts = async () => {
  const response = await API.get("/cloud/accounts");
  return response.data.data;
};

export const createCloudAccount = async (account) => {
  const response = await API.post("/cloud/accounts", account);
  return response.data.data;
};

export const updateCloudAccount = async (id, account) => {
  const response = await API.put(`/cloud/accounts/${id}`, account);
  return response.data.data;
};

export const verifyCloudAccount = async (id) => {
  const response = await API.post(`/cloud/accounts/${id}/verify`);
  return response.data.data;
};

export const runCloudScan = async (accountId) => {
  const response = await API.post(`/cloud/accounts/${accountId}/scans`);
  return response.data.data;
};

export const getCloudScans = async (accountId, page = 0, size = 10) => {
  const response = await API.get(`/cloud/accounts/${accountId}/scans`, { params: { page, size } });
  return response.data.data;
};

export const getCloudResources = async (accountId, page = 0, size = 25) => {
  const response = await API.get(`/cloud/accounts/${accountId}/resources`, { params: { page, size } });
  return response.data.data;
};

export const getCloudFindings = async ({ accountId = "", status = "", severity = "", search = "", page = 0, size = 25 } = {}) => {
  const response = await API.get("/cloud/findings", {
    params: { accountId: accountId || undefined, status, severity, search, page, size },
  });
  return response.data.data;
};

export const recheckCloudFinding = async (id) => {
  const response = await API.post(`/cloud/findings/${id}/recheck`);
  return response.data.data;
};

export const getRemediations = async ({ status = "", page = 0, size = 25 } = {}) => {
  const response = await API.get("/cloud/remediations", { params: { status, page, size } });
  return response.data.data;
};

export const createRemediation = async ({ findingId, actionType, justification }, idempotencyKey) => {
  const response = await API.post(
    "/cloud/remediations",
    { findingId, actionType, justification },
    { headers: { "Idempotency-Key": idempotencyKey } }
  );
  return response.data.data;
};

export const previewRemediation = async (id) => {
  const response = await API.get(`/cloud/remediations/${id}/preview`);
  return response.data.data;
};

export const approveRemediation = async (id, requestHash, comment) => {
  const response = await API.post(`/cloud/remediations/${id}/approve`, { requestHash, comment });
  return response.data.data;
};

export const rejectRemediation = async (id, requestHash, comment) => {
  const response = await API.post(`/cloud/remediations/${id}/reject`, { requestHash, comment });
  return response.data.data;
};

export const cancelRemediation = async (id) => {
  const response = await API.post(`/cloud/remediations/${id}/cancel`);
  return response.data.data;
};

export const executeRemediation = async (id, confirmResourceId) => {
  const response = await API.post(`/cloud/remediations/${id}/execute`, { confirmResourceId });
  return response.data.data;
};

export const verifyRemediation = async (id) => {
  const response = await API.post(`/cloud/remediations/${id}/verify`);
  return response.data.data;
};

/** Generates a client idempotency key; reused if the same submission is retried. */
export const newIdempotencyKey = () => {
  if (typeof crypto !== "undefined" && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return `key-${Date.now()}-${Math.random().toString(36).slice(2, 12)}`;
};

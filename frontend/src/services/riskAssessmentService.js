import API from "./api";

export const getRiskAssessments = async (search = "", page = 0, size = 100) => {
  const response = await API.get("/risk-assessments", {
    params: { search, page, size }
  });
  return response.data.data;
};

export const getRiskAssessmentById = async (id) => {
  const response = await API.get(`/risk-assessments/${id}`);
  return response.data.data;
};

export const createRiskAssessment = async (assessment) => {
  const response = await API.post("/risk-assessments", assessment);
  return response.data.data;
};

export const updateRiskAssessment = async (id, assessment) => {
  const response = await API.put(`/risk-assessments/${id}`, assessment);
  return response.data.data;
};

export const deleteRiskAssessment = async (id) => {
  await API.delete(`/risk-assessments/${id}`);
};

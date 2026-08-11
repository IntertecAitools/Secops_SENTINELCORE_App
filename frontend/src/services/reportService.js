import API from "./api";

const triggerDownload = (response, filename) => {
  const url = window.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement("a");
  link.href = url;
  link.setAttribute("download", filename);
  document.body.appendChild(link);
  link.click();
  link.remove();
};

export const downloadAssetsCSV = async () => {
  const response = await API.get("/reports/assets/csv", { responseType: "blob" });
  triggerDownload(response, "assets_report.csv");
};

export const downloadAlertsCSV = async () => {
  const response = await API.get("/reports/alerts/csv", { responseType: "blob" });
  triggerDownload(response, "alerts_report.csv");
};

export const downloadAssetsExcel = async () => {
  const response = await API.get("/reports/assets/excel", { responseType: "blob" });
  triggerDownload(response, "assets_report.xlsx");
};

export const downloadIncidentsExcel = async () => {
  const response = await API.get("/reports/incidents/excel", { responseType: "blob" });
  triggerDownload(response, "incidents_report.xlsx");
};

export const downloadIncidentsPDF = async () => {
  const response = await API.get("/reports/incidents/pdf", { responseType: "blob" });
  triggerDownload(response, "incidents_brief.pdf");
};

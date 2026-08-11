package com.sentinelcore.backend.service;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sentinelcore.backend.entity.Alert;
import com.sentinelcore.backend.entity.Asset;
import com.sentinelcore.backend.entity.Incident;
import com.sentinelcore.backend.repository.AlertRepository;
import com.sentinelcore.backend.repository.AssetRepository;
import com.sentinelcore.backend.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final AssetRepository assetRepository;
    private final AlertRepository alertRepository;
    private final IncidentRepository incidentRepository;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==========================================
    // CSV EXPORTS
    // ==========================================

    public byte[] exportAssetsToCSV() {
        log.info("Generating assets CSV report");
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Asset Name,Asset Type,IP Address,Operating System,Location,Status,Created At\n");

        List<Asset> assets = assetRepository.findAll();
        for (Asset asset : assets) {
            sb.append(asset.getId()).append(",")
                    .append(escapeCSV(asset.getAssetName())).append(",")
                    .append(escapeCSV(asset.getAssetType())).append(",")
                    .append(escapeCSV(asset.getIpAddress())).append(",")
                    .append(escapeCSV(asset.getOperatingSystem())).append(",")
                    .append(escapeCSV(asset.getLocation())).append(",")
                    .append(escapeCSV(asset.getStatus())).append(",")
                    .append(asset.getCreatedAt() != null ? asset.getCreatedAt().format(formatter) : "").append("\n");
        }
        return sb.toString().getBytes();
    }

    public byte[] exportAlertsToCSV() {
        log.info("Generating alerts CSV report");
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Title,Description,Severity,Status,Category,Source IP,Asset,Created At\n");

        List<Alert> alerts = alertRepository.findAll();
        for (Alert alert : alerts) {
            sb.append(alert.getId()).append(",")
                    .append(escapeCSV(alert.getTitle())).append(",")
                    .append(escapeCSV(alert.getDescription())).append(",")
                    .append(escapeCSV(alert.getSeverity())).append(",")
                    .append(escapeCSV(alert.getStatus())).append(",")
                    .append(escapeCSV(alert.getCategory())).append(",")
                    .append(escapeCSV(alert.getSourceIp())).append(",")
                    .append(alert.getAsset() != null ? escapeCSV(alert.getAsset().getAssetName()) : "N/A").append(",")
                    .append(alert.getCreatedAt() != null ? alert.getCreatedAt().format(formatter) : "").append("\n");
        }
        return sb.toString().getBytes();
    }

    // ==========================================
    // EXCEL EXPORTS
    // ==========================================

    public byte[] exportAssetsToExcel() throws IOException {
        log.info("Generating assets Excel report");
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Assets");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            org.apache.poi.ss.usermodel.Font font = workbook.createFont();
            font.setColor(IndexedColors.WHITE.getIndex());
            font.setBold(true);
            headerStyle.setFont(font);

            // Headers
            String[] headers = {"ID", "Asset Name", "Asset Type", "IP Address", "Operating System", "Location", "Status", "Created At"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data
            List<Asset> assets = assetRepository.findAll();
            int rowIdx = 1;
            for (Asset asset : assets) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(asset.getId());
                row.createCell(1).setCellValue(asset.getAssetName());
                row.createCell(2).setCellValue(asset.getAssetType());
                row.createCell(3).setCellValue(asset.getIpAddress());
                row.createCell(4).setCellValue(asset.getOperatingSystem());
                row.createCell(5).setCellValue(asset.getLocation());
                row.createCell(6).setCellValue(asset.getStatus());
                row.createCell(7).setCellValue(asset.getCreatedAt() != null ? asset.getCreatedAt().format(formatter) : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportIncidentsToExcel() throws IOException {
        log.info("Generating incidents Excel report");
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Incidents");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.MAROON.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            org.apache.poi.ss.usermodel.Font font = workbook.createFont();
            font.setColor(IndexedColors.WHITE.getIndex());
            font.setBold(true);
            headerStyle.setFont(font);

            // Headers
            String[] headers = {"ID", "Incident Title", "Description", "Severity", "Status", "Assigned To", "Alert Title", "Created At"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data
            List<Incident> incidents = incidentRepository.findAll();
            int rowIdx = 1;
            for (Incident incident : incidents) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(incident.getId());
                row.createCell(1).setCellValue(incident.getTitle());
                row.createCell(2).setCellValue(incident.getDescription());
                row.createCell(3).setCellValue(incident.getSeverity());
                row.createCell(4).setCellValue(incident.getStatus());
                row.createCell(5).setCellValue(incident.getAssignedTo() != null ? incident.getAssignedTo().getFullName() : "Unassigned");
                row.createCell(6).setCellValue(incident.getAlert() != null ? incident.getAlert().getTitle() : "N/A");
                row.createCell(7).setCellValue(incident.getCreatedAt() != null ? incident.getCreatedAt().format(formatter) : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    // ==========================================
    // PDF EXPORTS (OpenPDF)
    // ==========================================

    public byte[] exportIncidentsToPDF() {
        log.info("Generating incidents PDF report");
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            // Styling Font
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Font.BOLD);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

            // Title
            Paragraph title = new Paragraph("SentinelCore Security Platform - Incidents Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Table setup
            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100f);
            table.setWidths(new float[]{1.0f, 3.0f, 2.0f, 2.0f, 3.0f, 3.0f, 3.0f});

            // Table headers
            String[] headers = {"ID", "Title", "Severity", "Status", "Assigned To", "Associated Alert", "Created At"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(java.awt.Color.DARK_GRAY);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(6);
                table.addCell(cell);
            }

            // Add data rows
            List<Incident> incidents = incidentRepository.findAll();
            for (Incident incident : incidents) {
                table.addCell(new Phrase(incident.getId().toString(), cellFont));
                table.addCell(new Phrase(incident.getTitle(), cellFont));
                table.addCell(new Phrase(incident.getSeverity(), cellFont));
                table.addCell(new Phrase(incident.getStatus(), cellFont));
                table.addCell(new Phrase(incident.getAssignedTo() != null ? incident.getAssignedTo().getFullName() : "Unassigned", cellFont));
                table.addCell(new Phrase(incident.getAlert() != null ? incident.getAlert().getTitle() : "N/A", cellFont));
                table.addCell(new Phrase(incident.getCreatedAt() != null ? incident.getCreatedAt().format(formatter) : "", cellFont));
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error creating PDF report", e);
            throw new RuntimeException("Error creating PDF report", e);
        }
    }

    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        String clean = value.replace("\"", "\"\"");
        if (clean.contains(",") || clean.contains("\n") || clean.contains("\"")) {
            return "\"" + clean + "\"";
        }
        return clean;
    }
}

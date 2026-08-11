package com.sentinelcore.backend.controller;

import com.sentinelcore.backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/assets/csv")
    public ResponseEntity<byte[]> downloadAssetsCSV() {
        byte[] csvData = reportService.exportAssetsToCSV();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=assets.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }

    @GetMapping("/alerts/csv")
    public ResponseEntity<byte[]> downloadAlertsCSV() {
        byte[] csvData = reportService.exportAlertsToCSV();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=alerts.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }

    @GetMapping("/assets/excel")
    public ResponseEntity<byte[]> downloadAssetsExcel() throws IOException {
        byte[] excelData = reportService.exportAssetsToExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=assets.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
    }

    @GetMapping("/incidents/excel")
    public ResponseEntity<byte[]> downloadIncidentsExcel() throws IOException {
        byte[] excelData = reportService.exportIncidentsToExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=incidents.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
    }

    @GetMapping("/incidents/pdf")
    public ResponseEntity<byte[]> downloadIncidentsPDF() {
        byte[] pdfData = reportService.exportIncidentsToPDF();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=incidents.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }
}

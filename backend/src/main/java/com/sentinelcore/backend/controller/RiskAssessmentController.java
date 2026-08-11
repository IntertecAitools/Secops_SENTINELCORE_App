package com.sentinelcore.backend.controller;

import com.sentinelcore.backend.dto.RiskAssessmentDTO;
import com.sentinelcore.backend.dto.ApiResponse;
import com.sentinelcore.backend.service.RiskAssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/risk-assessments")
@RequiredArgsConstructor
public class RiskAssessmentController {

    private final RiskAssessmentService riskAssessmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<RiskAssessmentDTO>>> getRiskAssessments(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "assessmentDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<RiskAssessmentDTO> assessments = riskAssessmentService.getRiskAssessments(search, pageable);

        return ResponseEntity.ok(ApiResponse.success("Risk Assessments retrieved successfully", assessments));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RiskAssessmentDTO>> getRiskAssessmentById(@PathVariable Long id) {
        RiskAssessmentDTO assessment = riskAssessmentService.getRiskAssessmentById(id);
        return ResponseEntity.ok(ApiResponse.success("Risk Assessment retrieved successfully", assessment));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RiskAssessmentDTO>> createRiskAssessment(@RequestBody RiskAssessmentDTO dto) {
        RiskAssessmentDTO saved = riskAssessmentService.createRiskAssessment(dto);
        return ResponseEntity.ok(ApiResponse.success("Risk Assessment created successfully", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RiskAssessmentDTO>> updateRiskAssessment(@PathVariable Long id, @RequestBody RiskAssessmentDTO dto) {
        RiskAssessmentDTO updated = riskAssessmentService.updateRiskAssessment(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Risk Assessment updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteRiskAssessment(@PathVariable Long id) {
        riskAssessmentService.deleteRiskAssessment(id);
        return ResponseEntity.ok(ApiResponse.success("Risk Assessment deleted successfully", null));
    }
}

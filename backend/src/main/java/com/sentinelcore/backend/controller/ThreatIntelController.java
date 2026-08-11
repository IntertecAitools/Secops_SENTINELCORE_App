package com.sentinelcore.backend.controller;

import com.sentinelcore.backend.dto.ThreatIntelDTO;
import com.sentinelcore.backend.dto.ApiResponse;
import com.sentinelcore.backend.service.ThreatIntelService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/threat-intel")
@RequiredArgsConstructor
public class ThreatIntelController {

    private final ThreatIntelService threatIntelService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ThreatIntelDTO>>> getThreatIntels(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String indicatorType,
            @RequestParam(required = false) String confidenceLevel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ThreatIntelDTO> threatIntels = threatIntelService.getThreatIntels(search, indicatorType, confidenceLevel, pageable);

        return ResponseEntity.ok(ApiResponse.success("Threat Intelligence feeds retrieved successfully", threatIntels));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ThreatIntelDTO>> getThreatIntelById(@PathVariable Long id) {
        ThreatIntelDTO intel = threatIntelService.getThreatIntelById(id);
        return ResponseEntity.ok(ApiResponse.success("Threat Intel retrieved successfully", intel));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ThreatIntelDTO>> createThreatIntel(@RequestBody ThreatIntelDTO dto) {
        ThreatIntelDTO saved = threatIntelService.createThreatIntel(dto);
        return ResponseEntity.ok(ApiResponse.success("Threat Intel created successfully", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ThreatIntelDTO>> updateThreatIntel(@PathVariable Long id, @RequestBody ThreatIntelDTO dto) {
        ThreatIntelDTO updated = threatIntelService.updateThreatIntel(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Threat Intel updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteThreatIntel(@PathVariable Long id) {
        threatIntelService.deleteThreatIntel(id);
        return ResponseEntity.ok(ApiResponse.success("Threat Intel deleted successfully", null));
    }
}

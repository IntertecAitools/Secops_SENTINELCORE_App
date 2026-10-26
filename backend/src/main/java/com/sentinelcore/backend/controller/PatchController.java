package com.sentinelcore.backend.controller;

import com.sentinelcore.backend.dto.PatchDTO;
import com.sentinelcore.backend.dto.ApiResponse;
import com.sentinelcore.backend.service.PatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patches")
@RequiredArgsConstructor
public class PatchController {

    private final PatchService patchService;

    @PreAuthorize("hasAnyAuthority('PATCH_MANAGE', 'VULNERABILITY_VIEW')")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PatchDTO>>> getPatches(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<PatchDTO> patches = patchService.getPatches(search, status, pageable);

        return ResponseEntity.ok(ApiResponse.success("Patches retrieved successfully", patches));
    }

    @PreAuthorize("hasAnyAuthority('PATCH_MANAGE', 'VULNERABILITY_VIEW')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PatchDTO>> getPatchById(@PathVariable Long id) {
        PatchDTO patch = patchService.getPatchById(id);
        return ResponseEntity.ok(ApiResponse.success("Patch retrieved successfully", patch));
    }

    @PreAuthorize("hasAuthority('PATCH_MANAGE')")
    @PostMapping
    public ResponseEntity<ApiResponse<PatchDTO>> createPatch(@RequestBody PatchDTO dto) {
        PatchDTO saved = patchService.createPatch(dto);
        return ResponseEntity.ok(ApiResponse.success("Patch created successfully", saved));
    }

    @PreAuthorize("hasAuthority('PATCH_MANAGE')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PatchDTO>> updatePatch(@PathVariable Long id, @RequestBody PatchDTO dto) {
        PatchDTO updated = patchService.updatePatch(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Patch updated successfully", updated));
    }

    @PreAuthorize("hasAuthority('PATCH_MANAGE')")
    @PostMapping("/{id}/apply")
    public ResponseEntity<ApiResponse<PatchDTO>> applyPatch(@PathVariable Long id) {
        PatchDTO applied = patchService.applyPatch(id);
        return ResponseEntity.ok(ApiResponse.success("Patch applied successfully", applied));
    }

    @PreAuthorize("hasAuthority('PATCH_MANAGE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deletePatch(@PathVariable Long id) {
        patchService.deletePatch(id);
        return ResponseEntity.ok(ApiResponse.success("Patch deleted successfully", null));
    }
}

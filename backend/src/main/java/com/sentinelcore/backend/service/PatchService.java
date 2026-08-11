package com.sentinelcore.backend.service;

import com.sentinelcore.backend.dto.PatchDTO;
import com.sentinelcore.backend.entity.Asset;
import com.sentinelcore.backend.entity.Patch;
import com.sentinelcore.backend.exception.ResourceNotFoundException;
import com.sentinelcore.backend.repository.AssetRepository;
import com.sentinelcore.backend.repository.PatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatchService {

    private final PatchRepository patchRepository;
    private final AssetRepository assetRepository;

    public Page<PatchDTO> getPatches(String search, String status, Pageable pageable) {
        log.info("Fetching patches with search: {}, status: {}", search, status);

        Specification<Patch> spec = Specification.where(null);

        if (StringUtils.hasText(search)) {
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("description")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("version")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("targetOs")), "%" + search.toLowerCase() + "%")
            ));
        }

        if (StringUtils.hasText(status)) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("status")), status.toUpperCase()));
        }

        return patchRepository.findAll(spec, pageable).map(this::convertToDTO);
    }

    public PatchDTO getPatchById(Long id) {
        log.info("Fetching patch by id: {}", id);
        Patch patch = patchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patch not found with id: " + id));
        return convertToDTO(patch);
    }

    public PatchDTO createPatch(PatchDTO dto) {
        log.info("Creating patch: {}", dto.getTitle());
        Patch patch = convertToEntity(dto);
        Patch saved = patchRepository.save(patch);
        return convertToDTO(saved);
    }

    public PatchDTO updatePatch(Long id, PatchDTO dto) {
        log.info("Updating patch with id: {}", id);
        Patch patch = patchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patch not found with id: " + id));

        patch.setTitle(dto.getTitle());
        patch.setDescription(dto.getDescription());
        patch.setVersion(dto.getVersion());
        patch.setStatus(dto.getStatus());
        patch.setTargetOs(dto.getTargetOs());

        if (dto.getAssetId() != null) {
            Asset asset = assetRepository.findById(dto.getAssetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + dto.getAssetId()));
            patch.setAsset(asset);
        } else {
            patch.setAsset(null);
        }

        Patch updated = patchRepository.save(patch);
        return convertToDTO(updated);
    }

    public PatchDTO applyPatch(Long id) {
        log.info("Applying patch with id: {}", id);
        Patch patch = patchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patch not found with id: " + id));

        patch.setStatus("APPLIED");
        patch.setAppliedAt(LocalDateTime.now());

        Patch updated = patchRepository.save(patch);
        return convertToDTO(updated);
    }

    public void deletePatch(Long id) {
        log.info("Deleting patch with id: {}", id);
        if (!patchRepository.existsById(id)) {
            throw new ResourceNotFoundException("Patch not found with id: " + id);
        }
        patchRepository.deleteById(id);
    }

    private PatchDTO convertToDTO(Patch patch) {
        return PatchDTO.builder()
                .id(patch.getId())
                .title(patch.getTitle())
                .description(patch.getDescription())
                .version(patch.getVersion())
                .status(patch.getStatus())
                .targetOs(patch.getTargetOs())
                .assetId(patch.getAsset() != null ? patch.getAsset().getId() : null)
                .assetName(patch.getAsset() != null ? patch.getAsset().getAssetName() : null)
                .releaseDate(patch.getReleaseDate())
                .appliedAt(patch.getAppliedAt())
                .createdAt(patch.getCreatedAt())
                .build();
    }

    private Patch convertToEntity(PatchDTO dto) {
        Patch patch = Patch.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .version(dto.getVersion())
                .status(dto.getStatus())
                .targetOs(dto.getTargetOs())
                .releaseDate(dto.getReleaseDate() != null ? dto.getReleaseDate() : LocalDateTime.now())
                .build();

        if (dto.getAssetId() != null) {
            Asset asset = assetRepository.findById(dto.getAssetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Asset not found with id: " + dto.getAssetId()));
            patch.setAsset(asset);
        }
        return patch;
    }
}

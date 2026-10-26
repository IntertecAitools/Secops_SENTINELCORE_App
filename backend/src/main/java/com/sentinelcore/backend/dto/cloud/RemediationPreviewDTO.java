package com.sentinelcore.backend.dto.cloud;

import com.sentinelcore.backend.cloud.CloudModels.PreviewType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemediationPreviewDTO {

    private Long remediationId;

    private String requestHash;

    /** LOCAL_VALIDATION means the platform computed the change; the provider did not simulate it. */
    private PreviewType previewType;

    /** False when the current state could not be read (e.g. AWS integration disabled). */
    private boolean currentStateAvailable;

    private String currentState;

    private String plannedChange;

    private String notes;
}

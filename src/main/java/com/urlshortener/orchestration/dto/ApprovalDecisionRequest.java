package com.urlshortener.orchestration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Human decision on a pending approval gate. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalDecisionRequest {
    private String approver;
    /** Required when rejecting; ignored when approving. */
    private String reason;
}

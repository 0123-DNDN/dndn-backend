package com.team0123.dndn.family.dto;

import com.team0123.dndn.family.entity.GuardianRelationship;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FamilyRelationResponse {

    private Long relationshipId;
    private Long seniorUserId;
    private Long guardianUserId;
    private String status;

    public static FamilyRelationResponse from(
            GuardianRelationship relationship
    ) {
        return FamilyRelationResponse.builder()
                .relationshipId(relationship.getRelationshipId())
                .seniorUserId(relationship.getSeniorUserId())
                .guardianUserId(relationship.getGuardianUserId())
                .status(relationship.getStatus().name())
                .build();
    }
}
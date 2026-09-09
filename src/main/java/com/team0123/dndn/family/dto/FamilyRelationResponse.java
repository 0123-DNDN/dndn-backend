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
    private String seniorName;
    private String guardianName;
    private String status;

    public static FamilyRelationResponse from(
            GuardianRelationship relationship,
            String seniorName,
            String guardianName
    ) {
        return FamilyRelationResponse.builder()
                .relationshipId(relationship.getRelationshipId())
                .seniorUserId(relationship.getSeniorUserId())
                .guardianUserId(relationship.getGuardianUserId())
                .seniorName(seniorName)
                .guardianName(guardianName)
                .status(relationship.getStatus().name())
                .build();
    }
}
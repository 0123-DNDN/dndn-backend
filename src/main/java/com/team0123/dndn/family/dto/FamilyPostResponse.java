package com.team0123.dndn.family.dto;

import com.team0123.dndn.family.entity.FamilyPost;

import java.time.LocalDate;

public record FamilyPostResponse(
        Long familyPostId,
        Long relationshipId,
        LocalDate targetDate,
        String imageUrl,
        String message
) {

    public static FamilyPostResponse from(FamilyPost post) {
        return from(post, true);
    }

    public static FamilyPostResponse from(
            FamilyPost post,
            boolean exposeContent
    ) {
        return new FamilyPostResponse(
                post.getFamilyPostId(),
                post.getRelationshipId(),
                post.getTargetDate(),
                exposeContent ? post.getImageUrl() : null,
                exposeContent ? post.getMessage() : null
        );
    }
}

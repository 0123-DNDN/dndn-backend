package com.team0123.dndn.family.dto;

import java.util.List;

public record TodayFamilyPostsResponse(
        boolean unlocked,
        List<FamilyPostResponse> posts
) {
}

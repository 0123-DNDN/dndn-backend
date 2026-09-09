package com.team0123.dndn.family.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FamilyPostTest {

    @Test
    void viewedAtIsRecordedOnlyOnce() {
        FamilyPost post = FamilyPost.builder().build();
        LocalDateTime firstViewedAt = LocalDateTime.of(2026, 9, 8, 10, 0);

        post.markViewed(firstViewedAt);
        post.markViewed(firstViewedAt.plusHours(1));

        assertEquals(firstViewedAt, post.getViewedAt());
    }
}

package com.team0123.dndn.family.dto;

import java.math.BigDecimal;

public record WalkingWeeklySummary(
        int recordedDays,
        int completedDays,
        Long totalStepCount,
        BigDecimal averageStepCount
) {
}

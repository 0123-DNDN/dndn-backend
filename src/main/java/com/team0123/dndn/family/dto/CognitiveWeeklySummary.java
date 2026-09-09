package com.team0123.dndn.family.dto;

import java.math.BigDecimal;
import java.util.List;

public record CognitiveWeeklySummary(
        int activityCount,
        BigDecimal averageScore,
        List<CognitiveScorePoint> recentScores
) {
}

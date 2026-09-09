package com.team0123.dndn.family.dto;

import java.time.LocalDate;

public record WeeklyFamilyReportResponse(
        LocalDate weekStart,
        LocalDate weekEnd,
        SeniorSummary senior,
        WeeklyActivitySummary weeklyActivity,
        VoiceWeeklySummary voice,
        CognitiveWeeklySummary cognitive,
        WalkingWeeklySummary walking
) {
}

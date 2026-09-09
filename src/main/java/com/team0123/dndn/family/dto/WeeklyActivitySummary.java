package com.team0123.dndn.family.dto;

import java.util.List;

public record WeeklyActivitySummary(
        int completedDays,
        List<WeeklyActivityDayResponse> days
) {
}

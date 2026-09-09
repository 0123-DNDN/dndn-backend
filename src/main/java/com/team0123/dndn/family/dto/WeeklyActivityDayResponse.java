package com.team0123.dndn.family.dto;

import com.team0123.dndn.activity.entity.ActivityType;

import java.time.LocalDate;
import java.util.List;

public record WeeklyActivityDayResponse(
        LocalDate date,
        List<ActivityType> completedActivities
) {
}

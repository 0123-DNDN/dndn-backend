package com.team0123.dndn.activity.dto;

import com.team0123.dndn.activity.entity.Activity;
import com.team0123.dndn.activity.entity.ActivityResult;
import com.team0123.dndn.activity.entity.ActivityStatus;
import com.team0123.dndn.activity.entity.ActivityType;

public record TodayActivityResponse(
        Long activityId,
        ActivityType activityType,
        String title,
        String description,
        Integer targetValue,
        Integer displayOrder,
        String status,
        Integer score,
        Integer stepCount,
        boolean completed
) {

    public static TodayActivityResponse from(
            Activity activity,
            ActivityResult result
    ) {
        return new TodayActivityResponse(
                activity.getActivityId(),
                activity.getActivityType(),
                activity.getTitle(),
                activity.getDescription(),
                activity.getTargetValue(),
                activity.getDisplayOrder(),
                result == null ? null : result.getStatus().name(),
                result == null ? null : result.getScore(),
                result == null ? null : result.getStepCount(),
                result != null && result.getStatus() == ActivityStatus.COMPLETED
        );
    }
}

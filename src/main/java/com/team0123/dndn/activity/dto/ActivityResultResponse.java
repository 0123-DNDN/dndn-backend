package com.team0123.dndn.activity.dto;

import com.team0123.dndn.activity.entity.ActivityResult;
import com.team0123.dndn.activity.entity.ActivityType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ActivityResultResponse(
        Long activityResultId,
        Long activityId,
        ActivityType activityType,
        Long sessionId,
        LocalDate activityDate,
        String status,
        Integer score,
        Integer stepCount,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {

    public static ActivityResultResponse from(
            ActivityResult result,
            ActivityType activityType
    ) {
        return new ActivityResultResponse(
                result.getActivityResultId(),
                result.getActivityId(),
                activityType,
                result.getSessionId(),
                result.getActivityDate(),
                result.getStatus().name(),
                result.getScore(),
                result.getStepCount(),
                result.getStartedAt(),
                result.getCompletedAt()
        );
    }
}

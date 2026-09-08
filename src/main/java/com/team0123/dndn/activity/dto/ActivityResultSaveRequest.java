package com.team0123.dndn.activity.dto;

import com.team0123.dndn.activity.entity.ActivityStatus;
import jakarta.validation.constraints.PositiveOrZero;

public record ActivityResultSaveRequest(
        @PositiveOrZero(message = "점수는 0 이상이어야 합니다.")
        Integer score,

        @PositiveOrZero(message = "걸음 수는 0 이상이어야 합니다.")
        Integer stepCount,

        Long sessionId,

        ActivityStatus status
) {
}

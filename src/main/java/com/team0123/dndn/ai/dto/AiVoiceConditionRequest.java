package com.team0123.dndn.ai.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * 프론트에서 측정한 AI 비서 음성 입력 상태입니다.
 * Pitch 관련 값은 이번 MVP에서 측정하지 않으므로
 * 요청 DTO에 포함하지 않습니다.
 */
public record AiVoiceConditionRequest(

        // 실제 사용자가 발화한 시간, 단위는 밀리초
        @PositiveOrZero(
                message = "발화 시간은 0 이상이어야 합니다."
        )
        Long speechDurationMs,

        /*
         * 단위 시간당 발화량입니다.
         *
         * 프론트와 백엔드가 같은 계산 단위를 사용해야 합니다.
         * 예: 초당 음절 수 또는 분당 단어 수
         */
        @DecimalMin(
                value = "0.0",
                inclusive = true,
                message = "발화 속도는 0 이상이어야 합니다."
        )
        BigDecimal speechRate,

        // 발화 도중 발생한 평균 침묵시간, 단위는 밀리초
        @PositiveOrZero(
                message = "평균 침묵시간은 0 이상이어야 합니다."
        )
        Long avgPauseDurationMs,

        // 기준 시간보다 긴 침묵이 발생한 횟수
        @PositiveOrZero(
                message = "긴 침묵 횟수는 0 이상이어야 합니다."
        )
        Integer longPauseCount

) {

    /**
     * 긴 침묵 횟수가 생략되면
     * DB 기본값과 동일하게 0으로 처리합니다.
     */
    public int normalizedLongPauseCount() {
        return longPauseCount == null
                ? 0
                : longPauseCount;
    }
}
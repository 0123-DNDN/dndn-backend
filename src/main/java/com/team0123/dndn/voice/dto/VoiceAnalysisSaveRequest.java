package com.team0123.dndn.voice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record VoiceAnalysisSaveRequest(
        @NotBlank(message = "음성 변환 텍스트를 입력해 주세요.")
        String text,

        @PositiveOrZero(message = "녹음 시간은 0 이상이어야 합니다.")
        Double recordingDuration,

        @PositiveOrZero(message = "응답 시간은 0 이상이어야 합니다.")
        Double responseTime,

        @PositiveOrZero(message = "발화 속도는 0 이상이어야 합니다.")
        Double speechRate
) {
}

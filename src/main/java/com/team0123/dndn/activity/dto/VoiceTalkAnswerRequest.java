package com.team0123.dndn.activity.dto;

import jakarta.validation.constraints.NotBlank;

public record VoiceTalkAnswerRequest(
        @NotBlank(message = "답변 내용을 입력해 주세요.")
        String text
) {
}

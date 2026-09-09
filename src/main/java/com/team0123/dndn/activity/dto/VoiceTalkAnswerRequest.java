package com.team0123.dndn.activity.dto;

import com.team0123.dndn.ai.dto.AiVoiceConditionRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record VoiceTalkAnswerRequest(
        @NotBlank(message = "답변 내용을 입력해 주세요.")
        String text,

        @Valid
        AiVoiceConditionRequest voiceCondition
) {

    public VoiceTalkAnswerRequest(String text) {
        this(text, null);
    }
}

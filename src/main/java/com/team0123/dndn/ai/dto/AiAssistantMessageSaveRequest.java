package com.team0123.dndn.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * AI 비서가 사용자에게 제공한 응답을 저장하는 요청입니다.
 * senderType과 inputType은 요청에서 받지 않고
 * 백엔드가 ASSISTANT와 null로 고정합니다.
 */
public record AiAssistantMessageSaveRequest(

        // 실제로 사용자 화면에 표시하거나 TTS로 읽어준 AI 응답
        @NotBlank(message = "AI 응답 내용을 입력해 주세요.")
        @Size(
                max = 2000,
                message = "AI 응답은 2000자 이하로 입력해 주세요."
        )
        String content

) {
}
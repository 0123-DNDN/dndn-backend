package com.team0123.dndn.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 송금 목적 Context 분석 요청입니다.
 * 프론트 또는 A가 사용자가 설명한 송금 목적을 전달합니다.
 */
public record ContextAnalyzeRequest(

        // 송금 목적은 공백일 수 없습니다.
        @NotBlank(message = "송금 목적을 입력해 주세요.")

        // 지나치게 긴 입력과 불필요한 Gemini 사용량을 제한합니다.
        @Size(
                max = 500,
                message = "송금 목적은 500자 이하로 입력해 주세요."
        )
        String purposeText

) {
}
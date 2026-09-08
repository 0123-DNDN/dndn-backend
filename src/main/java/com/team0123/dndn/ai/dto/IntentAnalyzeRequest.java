package com.team0123.dndn.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 금융 발화 Intent 분석 API의 요청 DTO입니다.
 * 요청 예시:
 * {
 *   "text": "엄마에게 20만 원 보내줘"
 * }
 */
public record IntentAnalyzeRequest(
        // null, 빈 문자열, 공백만 있는 문자열을 허용하지 않습니다.
        @NotBlank(message = "분석할 문장을 입력해 주세요.")
        // 지나치게 긴 입력으로 인한 비용 및 오용을 줄이기 위한 제한입니다.
        @Size(max = 500, message = "문장은 500자 이하로 입력해 주세요.")
        String text) {
}
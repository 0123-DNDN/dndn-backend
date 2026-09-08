package com.team0123.dndn.ai.dto;

/**
 * Gemini가 생성한 JSON 분석 결과를 담는 내부 DTO입니다.
 * Gemini 응답 예시:
 * {
 *   "intent": "TRANSFER",
 *   "recipientKeyword": "엄마",
 *   "amount": 200000
 * }
 */
public record GeminiIntentResult(

        // Gemini가 분류한 금융 Intent
        String intent,

        // 사용자 발화에서 추출한 수취인 이름 또는 별칭
        String recipientKeyword,

        // 원 단위로 변환된 송금 금액
        Long amount

) {
}
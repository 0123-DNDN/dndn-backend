package com.team0123.dndn.ai.service;

import com.team0123.dndn.ai.client.GeminiClient;
import com.team0123.dndn.ai.dto.GeminiIntentResult;
import com.team0123.dndn.ai.dto.IntentAnalyzeResponse;
import com.team0123.dndn.ai.type.FinancialIntent;
import org.springframework.stereotype.Service;

/**
 * Gemini가 분석한 결과를 검증하고
 * 프론트에 반환할 최종 응답을 만드는 서비스입니다.
 * Gemini 결과를 그대로 신뢰하지 않고 비즈니스 규칙에 맞게 정리합니다.
 */
@Service
public class AiIntentService {
    private final GeminiClient geminiClient;
    public AiIntentService(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }
    /**
     * 사용자의 금융 발화를 분석합니다.
     * Gemini 분석에 성공하면 결과를 검증하고,
     * 실패하면 UNKNOWN 응답을 반환합니다.
     */
    public IntentAnalyzeResponse analyze(String text) {
        return geminiClient.analyze(text)
                .map(this::validateAndConvert)
                .orElseGet(IntentAnalyzeResponse::unknown);
    }

    /**
     * Gemini가 반환한 값을 서버 기준에 맞게 검증합니다.
     */
    private IntentAnalyzeResponse validateAndConvert(
            GeminiIntentResult result
    ) {
        // Gemini의 문자열 Intent를 안전하게 enum으로 변환합니다.
        FinancialIntent intent =
                FinancialIntent.from(result.intent());

        /*
         * 송금이 아닌 요청에는 수취인과 금액이 필요하지 않습니다.
         *
         * Gemini가 잘못된 부가 정보를 생성했더라도 null로 제거합니다.
         */
        if (intent != FinancialIntent.TRANSFER) {
            return new IntentAnalyzeResponse(
                    intent,
                    null,
                    null
            );
        }

        // 빈 수취인 값을 null로 정리합니다.
        String recipientKeyword =
                normalizeRecipient(result.recipientKeyword());

        // 0원 또는 음수 금액을 null로 정리합니다.
        Long amount =
                normalizeAmount(result.amount());

        return new IntentAnalyzeResponse(
                FinancialIntent.TRANSFER,
                recipientKeyword,
                amount
        );
    }

    /**
     * 수취인 문자열의 앞뒤 공백을 제거합니다.
     * 값이 없거나 공백뿐이면 null을 반환합니다.
     */
    private String normalizeRecipient(String recipientKeyword) {
        if (recipientKeyword == null
                || recipientKeyword.isBlank()) {
            return null;
        }

        return recipientKeyword.trim();
    }

    /**
     * 송금액이 실제로 사용할 수 있는 양수인지 확인합니다.
     */
    private Long normalizeAmount(Long amount) {
        if (amount == null || amount <= 0) {
            return null;
        }

        return amount;
    }
}
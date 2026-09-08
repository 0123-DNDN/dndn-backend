package com.team0123.dndn.ai.service;

import com.team0123.dndn.ai.client.GeminiClient;
import com.team0123.dndn.ai.dto.GeminiIntentResult;
import com.team0123.dndn.ai.dto.IntentAnalyzeResponse;
import com.team0123.dndn.ai.type.FinancialIntent;
import org.springframework.stereotype.Service;

/**
 * Gemini가 분석한 결과를 검증하고
 * 프론트에 반환할 최종 응답을 만드는 서비스입니다.
 * Gemini 결과를 그대로 신뢰하지 않고
 * 서버의 비즈니스 규칙에 맞게 정리합니다.
 */
@Service
public class AiIntentService {

    private final GeminiClient geminiClient;

    public AiIntentService(
            GeminiClient geminiClient
    ) {
        this.geminiClient = geminiClient;
    }

    /**
     * intentHint가 없는 기존 금융 발화 분석입니다.
     * 일반 AI 비서에서는 기존처럼
     * Gemini가 전체 금융 Intent를 분류합니다.
     */
    public IntentAnalyzeResponse analyze(
            String text
    ) {
        return analyze(text, null);
    }

    /**
     * 사용자의 금융 발화를 분석합니다.
     * intentHint가 TRANSFER이면 사용자가 이미
     * 돈 보내기 화면에 진입한 것으로 판단합니다.
     *
     * @param text 사용자가 입력하거나 말한 금융 발화
     * @param intentHint 사용자가 진입한 금융 기능
     * @return 검증된 금융 Intent 분석 결과
     */
    public IntentAnalyzeResponse analyze(
            String text,
            FinancialIntent intentHint
    ) {
        /*
         * 현재 MVP에서는 TRANSFER 힌트만 사용합니다.
         *
         * 다른 값이 들어오면 일반 AI 비서 분석과
         * 동일하게 처리합니다.
         */
        FinancialIntent validatedHint =
                validateIntentHint(intentHint);

        return geminiClient
                .analyze(text, validatedHint)

                /*
                 * Gemini 분석에 성공하면
                 * 결과와 intentHint를 함께 검증합니다.
                 */
                .map(result ->
                        validateAndConvert(
                                result,
                                validatedHint
                        )
                )

                /*
                 * Gemini 호출에 실패하더라도
                 * TRANSFER 화면에 진입했다는 사실은 알고 있습니다.
                 *
                 * 따라서 TRANSFER 힌트가 있으면
                 * UNKNOWN이 아니라 TRANSFER를 반환하고,
                 * 수취인과 금액은 null로 처리합니다.
                 */
                .orElseGet(() ->
                        createFailureResponse(validatedHint)
                );
    }

    /**
     * 현재 지원하는 Intent Hint인지 검증합니다.
     * MVP에서는 홈의 돈 보내기 버튼으로 진입하는
     * TRANSFER Hint만 지원합니다.
     */
    private FinancialIntent validateIntentHint(
            FinancialIntent intentHint
    ) {
        if (intentHint == FinancialIntent.TRANSFER) {
            return FinancialIntent.TRANSFER;
        }

        return null;
    }

    /**
     * Gemini가 반환한 값을 서버 기준에 맞게 검증합니다.
     */
    private IntentAnalyzeResponse validateAndConvert(
            GeminiIntentResult result,
            FinancialIntent intentHint
    ) {
        /*
         * TRANSFER Hint가 있다면 Gemini가 다른 Intent를
         * 반환해도 최종 결과는 TRANSFER로 고정합니다.
         *
         * intentHint는 실제 송금을 실행하는 권한이 아니라
         * 사용자가 송금 화면에 있다는 분석 Context입니다.
         */
        FinancialIntent intent =
                intentHint == FinancialIntent.TRANSFER
                        ? FinancialIntent.TRANSFER
                        : FinancialIntent.from(
                        result.intent()
                );

        /*
         * 송금이 아닌 요청에는 수취인과 금액이
         * 필요하지 않습니다.
         *
         * Gemini가 잘못된 부가 정보를 생성하더라도
         * null로 제거합니다.
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
                normalizeRecipient(
                        result.recipientKeyword()
                );

        // 0원 또는 음수 금액을 null로 정리합니다.
        Long amount =
                normalizeAmount(
                        result.amount()
                );

        return new IntentAnalyzeResponse(
                FinancialIntent.TRANSFER,
                recipientKeyword,
                amount
        );
    }

    /**
     * Gemini 호출 또는 응답 분석에 실패했을 때
     * 반환할 안전한 결과를 만듭니다.
     */
    private IntentAnalyzeResponse createFailureResponse(
            FinancialIntent intentHint
    ) {
        /*
         * 사용자가 이미 돈 보내기 화면에 있다면
         * 송금 의도 자체는 확실합니다.
         *
         * 다만 Gemini가 수취인과 금액을 추출하지 못했으므로
         * 두 값은 null로 반환하고 A/프론트가 다시 질문합니다.
         */
        if (intentHint == FinancialIntent.TRANSFER) {
            return new IntentAnalyzeResponse(
                    FinancialIntent.TRANSFER,
                    null,
                    null
            );
        }

        /*
         * 일반 AI 비서 요청에서 분석에 실패하면
         * 기존처럼 UNKNOWN을 반환합니다.
         */
        return IntentAnalyzeResponse.unknown();
    }

    /**
     * 수취인 문자열의 앞뒤 공백을 제거합니다.
     * 값이 없거나 공백뿐이면 null을 반환합니다.
     */
    private String normalizeRecipient(
            String recipientKeyword
    ) {
        if (recipientKeyword == null
                || recipientKeyword.isBlank()) {
            return null;
        }

        return recipientKeyword.trim();
    }

    /**
     * 송금액이 실제로 사용할 수 있는 양수인지 확인합니다.
     */
    private Long normalizeAmount(
            Long amount
    ) {
        if (amount == null || amount <= 0) {
            return null;
        }

        return amount;
    }
}
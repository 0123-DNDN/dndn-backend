package com.team0123.dndn.ai.service;

import com.team0123.dndn.ai.client.GeminiClient;
import com.team0123.dndn.ai.dto.GeminiIntentResult;
import com.team0123.dndn.ai.dto.IntentAnalyzeResponse;
import com.team0123.dndn.ai.type.FinancialIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * AiIntentService의 Intent Hint 처리를 검증합니다.
 *
 * 실제 Gemini API는 호출하지 않습니다.
 */
class AiIntentServiceTest {

    private GeminiClient geminiClient;
    private AiIntentService aiIntentService;

    @BeforeEach
    void setUp() {
        geminiClient = mock(GeminiClient.class);

        aiIntentService =
                new AiIntentService(geminiClient);
    }

    /**
     * TRANSFER Hint가 있으면 "보내줘"라는 표현이 없어도
     * 최종 Intent를 TRANSFER로 결정해야 합니다.
     */
    @Test
    void transferHintForcesTransferIntent() {
        // given
        String text = "엄마한테 20만 원";

        /*
         * Gemini가 실수로 UNKNOWN을 반환해도
         * 사용자가 송금 화면에 있으므로 Service가
         * 최종 Intent를 TRANSFER로 고정해야 합니다.
         */
        GeminiIntentResult geminiResult =
                new GeminiIntentResult(
                        "UNKNOWN",
                        "엄마",
                        200000L
                );

        when(
                geminiClient.analyze(
                        text,
                        FinancialIntent.TRANSFER
                )
        ).thenReturn(
                Optional.of(geminiResult)
        );

        // when
        IntentAnalyzeResponse response =
                aiIntentService.analyze(
                        text,
                        FinancialIntent.TRANSFER
                );

        // then
        assertEquals(
                FinancialIntent.TRANSFER,
                response.intent()
        );

        assertEquals(
                "엄마",
                response.recipientKeyword()
        );

        assertEquals(
                200000L,
                response.amount()
        );
    }

    /**
     * 송금 화면에서 수취인만 입력하면
     * 금액은 null로 유지해야 합니다.
     */
    @Test
    void transferHintKeepsMissingAmountNull() {
        // given
        String text = "엄마한테";

        GeminiIntentResult geminiResult =
                new GeminiIntentResult(
                        "TRANSFER",
                        "엄마",
                        null
                );

        when(
                geminiClient.analyze(
                        text,
                        FinancialIntent.TRANSFER
                )
        ).thenReturn(
                Optional.of(geminiResult)
        );

        // when
        IntentAnalyzeResponse response =
                aiIntentService.analyze(
                        text,
                        FinancialIntent.TRANSFER
                );

        // then
        assertEquals(
                FinancialIntent.TRANSFER,
                response.intent()
        );

        assertEquals(
                "엄마",
                response.recipientKeyword()
        );

        assertNull(response.amount());
    }

    /**
     * 송금 화면에서 금액만 입력하면
     * 수취인은 null로 유지해야 합니다.
     */
    @Test
    void transferHintKeepsMissingRecipientNull() {
        // given
        String text = "20만 원";

        GeminiIntentResult geminiResult =
                new GeminiIntentResult(
                        "TRANSFER",
                        null,
                        200000L
                );

        when(
                geminiClient.analyze(
                        text,
                        FinancialIntent.TRANSFER
                )
        ).thenReturn(
                Optional.of(geminiResult)
        );

        // when
        IntentAnalyzeResponse response =
                aiIntentService.analyze(
                        text,
                        FinancialIntent.TRANSFER
                );

        // then
        assertEquals(
                FinancialIntent.TRANSFER,
                response.intent()
        );

        assertNull(response.recipientKeyword());

        assertEquals(
                200000L,
                response.amount()
        );
    }

    /**
     * TRANSFER Hint가 있는 상태에서 Gemini가 실패해도
     * 사용자가 송금 화면에 있다는 사실은 유지해야 합니다.
     */
    @Test
    void transferHintSurvivesGeminiFailure() {
        // given
        String text = "엄마한테 20만 원";

        when(
                geminiClient.analyze(
                        text,
                        FinancialIntent.TRANSFER
                )
        ).thenReturn(Optional.empty());

        // when
        IntentAnalyzeResponse response =
                aiIntentService.analyze(
                        text,
                        FinancialIntent.TRANSFER
                );

        // then
        assertEquals(
                FinancialIntent.TRANSFER,
                response.intent()
        );

        /*
         * Gemini가 실패했으므로 수취인과 금액을
         * 임의로 추측하지 않습니다.
         */
        assertNull(response.recipientKeyword());
        assertNull(response.amount());
    }

    /**
     * Hint가 없는 일반 AI 비서는
     * 기존처럼 Gemini가 반환한 Intent를 사용해야 합니다.
     */
    @Test
    void requestWithoutHintUsesDetectedIntent() {
        // given
        String text = "이번 달 거래내역 보여줘";

        GeminiIntentResult geminiResult =
                new GeminiIntentResult(
                        "TRANSACTION_HISTORY",
                        null,
                        null
                );

        when(
                geminiClient.analyze(text, null)
        ).thenReturn(
                Optional.of(geminiResult)
        );

        // when
        IntentAnalyzeResponse response =
                aiIntentService.analyze(text);

        // then
        assertEquals(
                FinancialIntent.TRANSACTION_HISTORY,
                response.intent()
        );

        assertNull(response.recipientKeyword());
        assertNull(response.amount());
    }

    /**
     * Hint가 없는 일반 AI 비서에서 Gemini가 실패하면
     * 기존처럼 UNKNOWN을 반환해야 합니다.
     */
    @Test
    void requestWithoutHintReturnsUnknownOnFailure() {
        // given
        String text = "오늘 날씨 알려줘";

        when(
                geminiClient.analyze(text, null)
        ).thenReturn(Optional.empty());

        // when
        IntentAnalyzeResponse response =
                aiIntentService.analyze(text);

        // then
        assertEquals(
                FinancialIntent.UNKNOWN,
                response.intent()
        );

        assertNull(response.recipientKeyword());
        assertNull(response.amount());
    }
}
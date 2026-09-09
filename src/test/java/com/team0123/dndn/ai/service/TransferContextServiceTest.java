package com.team0123.dndn.ai.service;

import com.team0123.dndn.ai.client.GeminiContextClient;
import com.team0123.dndn.ai.dto.ContextAnalyzeResponse;
import com.team0123.dndn.ai.dto.GeminiContextResult;
import com.team0123.dndn.ai.type.FollowUpQuestionCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * TransferContextService의 위험 신호 검증과
 * 추가 확인 질문 생성을 검증하는 단위 테스트입니다.
 *
 * 실제 Gemini API는 호출하지 않습니다.
 */
class TransferContextServiceTest {

    private GeminiContextClient geminiContextClient;
    private TransferContextService transferContextService;

    @BeforeEach
    void setUp() {
        // 실제 Gemini 대신 Mock Client를 사용합니다.
        geminiContextClient =
                mock(GeminiContextClient.class);

        transferContextService =
                new TransferContextService(
                        geminiContextClient
                );
    }

    /**
     * 일반적인 송금 목적에는 추가 질문이 없어야 합니다.
     */
    @Test
    void safePurposeDoesNotRequireFollowUp() {
        // given
        String purposeText =
                "아들 생활비로 보내는 거야.";

        GeminiContextResult geminiResult =
                new GeminiContextResult(
                        false,
                        List.of(),
                        List.of()
                );

        when(geminiContextClient.analyze(purposeText))
                .thenReturn(Optional.of(geminiResult));

        // when
        ContextAnalyzeResponse response =
                transferContextService.analyze(purposeText);

        // then
        assertFalse(response.suspicious());
        assertFalse(response.requiresFollowUp());
        assertTrue(response.followUpQuestions().isEmpty());
    }

    /**
     * 기관 사칭과 제3자 송금 지시가 탐지되면
     * 최대 3개의 추가 질문을 반환해야 합니다.
     */
    @Test
    void authorityInstructionReturnsThreeFollowUpQuestions() {
        // given
        String purposeText =
                "김 검사가 보내래.";

        GeminiContextResult geminiResult =
                new GeminiContextResult(
                        true,
                        List.of(
                                "AUTHORITY_IMPERSONATION",
                                "THIRD_PARTY_INSTRUCTION"
                        ),
                        List.of(
                                "기관 관계자의 송금 지시가 확인됐어요."
                        )
                );

        when(geminiContextClient.analyze(purposeText))
                .thenReturn(Optional.of(geminiResult));

        // when
        ContextAnalyzeResponse response =
                transferContextService.analyze(purposeText);

        // then
        assertTrue(response.suspicious());
        assertTrue(response.requiresFollowUp());

        // 한 번에 최대 3개만 반환해야 합니다.
        assertEquals(
                3,
                response.followUpQuestions().size()
        );

        // 우선순위에 따라 긴급성, 안전계좌, 비밀 유지를 질문합니다.
        assertEquals(
                FollowUpQuestionCode.URGENCY,
                response.followUpQuestions().get(0).code()
        );

        assertEquals(
                FollowUpQuestionCode.SAFE_ACCOUNT_REQUEST,
                response.followUpQuestions().get(1).code()
        );

        assertEquals(
                FollowUpQuestionCode.SECRECY_REQUEST,
                response.followUpQuestions().get(2).code()
        );
    }

    /**
     * 최초 문장에서 이미 탐지된 신호는
     * 추가 질문으로 다시 반환하지 않아야 합니다.
     */
    @Test
    void alreadyDetectedSignalIsNotAskedAgain() {
        // given
        String purposeText =
                "김 검사가 지금 안전계좌로 보내래.";

        GeminiContextResult geminiResult =
                new GeminiContextResult(
                        true,
                        List.of(
                                "AUTHORITY_IMPERSONATION",
                                "THIRD_PARTY_INSTRUCTION",
                                "URGENCY",
                                "SAFE_ACCOUNT_REQUEST"
                        ),
                        List.of(
                                "기관이 안전계좌로 긴급 송금을 요구했어요."
                        )
                );

        when(geminiContextClient.analyze(purposeText))
                .thenReturn(Optional.of(geminiResult));

        // when
        ContextAnalyzeResponse response =
                transferContextService.analyze(purposeText);

        // then
        assertTrue(response.requiresFollowUp());

        /*
         * URGENCY와 SAFE_ACCOUNT_REQUEST는 이미 탐지됐으므로
         * 같은 질문이 다시 나오면 안 됩니다.
         */
        assertFalse(
                response.followUpQuestions()
                        .stream()
                        .anyMatch(question ->
                                question.code()
                                        == FollowUpQuestionCode.URGENCY
                        )
        );

        assertFalse(
                response.followUpQuestions()
                        .stream()
                        .anyMatch(question ->
                                question.code()
                                        == FollowUpQuestionCode
                                        .SAFE_ACCOUNT_REQUEST
                        )
        );
    }

    /**
     * 송금 목적이 비어 있으면 Gemini를 호출하지 않고
     * 목적 불명확 신호와 추가 질문을 반환해야 합니다.
     */
    @Test
    void blankPurposeReturnsFollowUpWithoutGeminiCall() {
        // when
        ContextAnalyzeResponse response =
                transferContextService.analyze(" ");

        // then
        assertTrue(response.suspicious());
        assertTrue(response.requiresFollowUp());
        assertEquals(
                3,
                response.followUpQuestions().size()
        );

        /*
         * 목적이 비어 있으면 외부 API를 호출할 필요가 없습니다.
         */
        verifyNoInteractions(geminiContextClient);
    }
}
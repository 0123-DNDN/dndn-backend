package com.team0123.dndn.fds.service;

import com.team0123.dndn.ai.dto.ContextAnalyzeResponse;
import com.team0123.dndn.ai.dto.FollowUpAnswer;
import com.team0123.dndn.ai.service.TransferContextService;
import com.team0123.dndn.ai.type.ContextRiskSignal;
import com.team0123.dndn.ai.type.FollowUpQuestionCode;
import com.team0123.dndn.fds.dto.FdsAnalyzeRequest;
import com.team0123.dndn.fds.dto.FdsAnalyzeResponse;
import com.team0123.dndn.fds.dto.FdsRiskScoreRequest;
import com.team0123.dndn.fds.dto.FdsRiskScoreResponse;
import com.team0123.dndn.fds.type.FdsDecisionRule;
import com.team0123.dndn.fds.type.RecommendedAction;
import com.team0123.dndn.fds.type.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FdsAnalyzeService의 추가 답변 병합과
 * 최종 위험 판단을 검증하는 단위 테스트입니다.
 *
 * 실제 Gemini API와 DB는 사용하지 않습니다.
 */
class FdsAnalyzeServiceTest {

    private TransferContextService transferContextService;
    private FdsRiskScoringService riskScoringService;
    private FdsAnalyzeService fdsAnalyzeService;

    @BeforeEach
    void setUp() {
        // 실제 Context 분석 대신 Mock 결과를 사용합니다.
        transferContextService =
                mock(TransferContextService.class);

        // 실제 점수 계산 대신 테스트용 결과를 사용합니다.
        riskScoringService =
                mock(FdsRiskScoringService.class);

        fdsAnalyzeService =
                new FdsAnalyzeService(
                        transferContextService,
                        riskScoringService
                );
    }

    /**
     * 일반적인 송금 목적과 추가 답변이 없으면
     * 기존처럼 LOW와 PROCEED를 반환해야 합니다.
     */
    @Test
    void urgentFamilyAccidentSettlementOverridesLowScoreEvenWhenContextAnalysisFails() {
        for (String purpose : List.of(
                "딸이 교통사고 났다고 지금 피해자한테 합의금 빨리 내야해",
                "아드님이 지금 교통사고를 냈습니다. 피해자가 많이 다쳐서 합의를 바로 보셔야 합니다. 피해자 쪽에서 합의금 500만 원을 요구하고 있습니다. 피해자 가족 계좌를 불러드릴게요.",
                "따님이 교통 사고를 냈대요. 합의금 당장 보내야 해요"
        )) {
            FdsAnalyzeRequest request = new FdsAnalyzeRequest(
                    purpose, List.of(), null, null, null, null, null, null);
            when(transferContextService.analyze(purpose)).thenReturn(ContextAnalyzeResponse.failed());
            when(riskScoringService.calculate(any())).thenReturn(scoreResponse(18));

            FdsAnalyzeResponse response = fdsAnalyzeService.analyze(request);

            assertEquals(RiskLevel.HIGH, response.riskLevel());
            assertEquals(RecommendedAction.from(RiskLevel.HIGH), response.recommendedAction());
            assertEquals(18, response.riskScore());
            assertTrue(response.triggeredRules().contains(FdsDecisionRule.FAMILY_ACCIDENT_URGENT_SETTLEMENT));
        }
    }

    @Test
    void ordinaryFamilyTransfersAndNonUrgentSettlementsDoNotTriggerAccidentRule() {
        for (String purpose : List.of(
                "딸한테 지금 생활비 보내줘",
                "교통사고 피해자에게 합의금을 보내요",
                "딸 교통사고 합의금을 서류 확인 후 다음 주에 보내요",
                "딸 교통사고 합의금인데 지금 급하지 않아 천천히 보낼 거야"
        )) {
            FdsAnalyzeRequest request = new FdsAnalyzeRequest(
                    purpose, List.of(), null, null, null, null, null, null);
            when(transferContextService.analyze(purpose)).thenReturn(ContextAnalyzeResponse.safe());
            when(riskScoringService.calculate(any())).thenReturn(scoreResponse(18));
            assertEquals(RiskLevel.CAUTION, fdsAnalyzeService.analyze(request).riskLevel());
        }
    }

    @Test
    void safeContextWithoutAnswersReturnsLow() {
        // given
        FdsAnalyzeRequest request =
                new FdsAnalyzeRequest(
                        "아들 생활비로 보내는 거야.",
                        List.of(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        when(transferContextService.analyze(
                request.purposeText()
        )).thenReturn(
                ContextAnalyzeResponse.safe()
        );

        when(riskScoringService.calculate(any()))
                .thenReturn(scoreResponse(0));

        // when
        FdsAnalyzeResponse response =
                fdsAnalyzeService.analyze(request);

        // then
        assertEquals(
                RiskLevel.LOW,
                response.riskLevel()
        );

        assertEquals(
                RecommendedAction.PROCEED,
                response.recommendedAction()
        );

        assertFalse(response.hardRuleTriggered());
        assertFalse(response.combinationRuleTriggered());
    }

    /**
     * '예' 답변은 Context 위험 신호에 추가하고,
     * '아니요' 답변은 추가하지 않아야 합니다.
     */
    @Test
    void yesAnswersAreMergedAndNoAnswersAreIgnored() {
        // given
        List<FollowUpAnswer> answers =
                List.of(
                        new FollowUpAnswer(
                                FollowUpQuestionCode.URGENCY,
                                true
                        ),
                        new FollowUpAnswer(
                                FollowUpQuestionCode
                                        .SAFE_ACCOUNT_REQUEST,
                                true
                        ),
                        new FollowUpAnswer(
                                FollowUpQuestionCode
                                        .SECRECY_REQUEST,
                                false
                        )
                );

        FdsAnalyzeRequest request =
                new FdsAnalyzeRequest(
                        "김 검사가 보내래.",
                        answers,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        /*
         * 최초 Gemini 분석에서는 기관 사칭과
         * 제3자 송금 지시만 탐지됐다고 가정합니다.
         */
        ContextAnalyzeResponse contextResponse =
                new ContextAnalyzeResponse(
                        true,
                        List.of(
                                ContextRiskSignal
                                        .AUTHORITY_IMPERSONATION,
                                ContextRiskSignal
                                        .THIRD_PARTY_INSTRUCTION
                        ),
                        List.of(
                                "기관 관계자의 송금 지시가 확인됐어요."
                        ),
                        true
                );

        when(transferContextService.analyze(
                request.purposeText()
        )).thenReturn(contextResponse);

        when(riskScoringService.calculate(any()))
                .thenReturn(scoreResponse(0));

        // when
        FdsAnalyzeResponse response =
                fdsAnalyzeService.analyze(request);

        /*
         * 점수 계산 Service에 실제로 전달된 요청을 가져옵니다.
         * 이를 통해 추가 답변 신호가 병합됐는지 확인합니다.
         */
        ArgumentCaptor<FdsRiskScoreRequest> captor =
                ArgumentCaptor.forClass(
                        FdsRiskScoreRequest.class
                );

        verify(riskScoringService)
                .calculate(captor.capture());

        List<ContextRiskSignal> mergedSignals =
                captor.getValue().contextSignals();

        // then
        assertTrue(
                mergedSignals.contains(
                        ContextRiskSignal.AUTHORITY_IMPERSONATION
                )
        );

        assertTrue(
                mergedSignals.contains(
                        ContextRiskSignal.THIRD_PARTY_INSTRUCTION
                )
        );

        // URGENCY=true이므로 위험 신호에 추가됩니다.
        assertTrue(
                mergedSignals.contains(
                        ContextRiskSignal.URGENCY
                )
        );

        // SAFE_ACCOUNT_REQUEST=true이므로 추가됩니다.
        assertTrue(
                mergedSignals.contains(
                        ContextRiskSignal.SAFE_ACCOUNT_REQUEST
                )
        );

        // SECRECY_REQUEST=false이므로 추가되면 안 됩니다.
        assertFalse(
                mergedSignals.contains(
                        ContextRiskSignal.SECRECY_REQUEST
                )
        );

        /*
         * 기관 사칭 + 안전계좌 + 제3자 송금 지시 조합이 완성되어
         * 기본 점수가 0점이어도 CRITICAL이 되어야 합니다.
         */
        assertEquals(
                RiskLevel.CRITICAL,
                response.riskLevel()
        );

        assertEquals(
                RecommendedAction.HOLD,
                response.recommendedAction()
        );

        assertTrue(response.combinationRuleTriggered());

        assertTrue(
                response.triggeredRules().contains(
                        FdsDecisionRule
                                .INSTITUTION_IMPERSONATION
                )
        );
    }

    @Test
    void previouslyDetectedSignalsAndFollowUpAnswersTriggerHighRiskRule() {
        FdsAnalyzeRequest request =
                new FdsAnalyzeRequest(
                        "검사가 계좌가 범죄에 연루됐다고 했어요.",
                        List.of(
                                new FollowUpAnswer(
                                        FollowUpQuestionCode.URGENCY,
                                        true
                                ),
                                new FollowUpAnswer(
                                        FollowUpQuestionCode.SAFE_ACCOUNT_REQUEST,
                                        true
                                ),
                                new FollowUpAnswer(
                                        FollowUpQuestionCode.SECRECY_REQUEST,
                                        true
                                )
                        ),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        when(transferContextService.analyze(request.purposeText()))
                .thenReturn(ContextAnalyzeResponse.safe());
        when(riskScoringService.calculate(any()))
                .thenReturn(scoreResponse(20));

        FdsAnalyzeResponse response = fdsAnalyzeService.analyze(
                request,
                List.of(
                        ContextRiskSignal.AUTHORITY_IMPERSONATION,
                        ContextRiskSignal.CRIME_OR_ACCOUNT_THREAT
                )
        );

        ArgumentCaptor<FdsRiskScoreRequest> captor =
                ArgumentCaptor.forClass(FdsRiskScoreRequest.class);
        verify(riskScoringService).calculate(captor.capture());

        assertTrue(captor.getValue().contextSignals().containsAll(List.of(
                ContextRiskSignal.AUTHORITY_IMPERSONATION,
                ContextRiskSignal.CRIME_OR_ACCOUNT_THREAT,
                ContextRiskSignal.URGENCY,
                ContextRiskSignal.SAFE_ACCOUNT_REQUEST,
                ContextRiskSignal.SECRECY_REQUEST
        )));
        assertEquals(RiskLevel.HIGH, response.riskLevel());
        assertEquals(RecommendedAction.WARN, response.recommendedAction());
        assertTrue(response.combinationRuleTriggered());
        assertTrue(response.triggeredRules().contains(
                FdsDecisionRule.AUTHORITY_THREAT
        ));
    }

    /**
     * 최초 Context와 추가 답변에 같은 위험 신호가 있어도
     * 한 번만 유지되어야 합니다.
     */
    @Test
    void duplicatedSignalsAreMergedOnlyOnce() {
        // given
        FdsAnalyzeRequest request =
                new FdsAnalyzeRequest(
                        "지금 바로 보내래.",
                        List.of(
                                new FollowUpAnswer(
                                        FollowUpQuestionCode.URGENCY,
                                        true
                                )
                        ),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );

        ContextAnalyzeResponse contextResponse =
                new ContextAnalyzeResponse(
                        true,
                        List.of(
                                ContextRiskSignal.URGENCY
                        ),
                        List.of(
                                "빠르게 송금하도록 압박받고 있어요."
                        ),
                        true
                );

        when(transferContextService.analyze(
                request.purposeText()
        )).thenReturn(contextResponse);

        when(riskScoringService.calculate(any()))
                .thenReturn(scoreResponse(4));

        // when
        fdsAnalyzeService.analyze(request);

        ArgumentCaptor<FdsRiskScoreRequest> captor =
                ArgumentCaptor.forClass(
                        FdsRiskScoreRequest.class
                );

        verify(riskScoringService)
                .calculate(captor.capture());

        List<ContextRiskSignal> mergedSignals =
                captor.getValue().contextSignals();

        // then
        long urgencyCount =
                mergedSignals.stream()
                        .filter(signal ->
                                signal
                                        == ContextRiskSignal.URGENCY
                        )
                        .count();

        assertEquals(1, urgencyCount);
    }

    /**
     * 테스트에서 사용할 점수 계산 결과를 생성합니다.
     */
    private FdsRiskScoreResponse scoreResponse(
            int totalScore
    ) {
        return new FdsRiskScoreResponse(
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                totalScore,
                List.of()
        );
    }
}

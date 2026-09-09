package com.team0123.dndn.fds.service;

import com.team0123.dndn.ai.type.ContextRiskSignal;
import com.team0123.dndn.fds.dto.FdsRiskScoreRequest;
import com.team0123.dndn.fds.dto.FdsRiskScoreResponse;
import com.team0123.dndn.fds.type.RiskInputType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FdsRiskScoringService 단위 테스트입니다.
 *
 * 외부 API와 DB 없이 Rule-based 점수 계산만 검증합니다.
 */
class FdsRiskScoringServiceTest {

    private FdsRiskScoringService scoringService;

    @BeforeEach
    void setUp() {
        scoringService = new FdsRiskScoringService();
    }

    /**
     * 아무 위험 정보도 없으면 모든 점수가 0점이어야 합니다.
     */
    @Test
    void noRiskInputReturnsZeroScore() {
        // given
        FdsRiskScoreRequest request =
                new FdsRiskScoreRequest(
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of(),
                        null
                );

        // when
        FdsRiskScoreResponse response =
                scoringService.calculate(request);

        // then
        assertEquals(0, response.transactionScore());
        assertEquals(0, response.recipientScore());
        assertEquals(0, response.velocityScore());
        assertEquals(0, response.deviceScore());
        assertEquals(0, response.behaviorScore());
        assertEquals(0, response.contextScore());
        assertEquals(0, response.conditionScore());
        assertEquals(0, response.totalScore());
        assertTrue(response.details().isEmpty());
    }

    /**
     * 요청 자체가 null이면 명확한 예외가 발생해야 합니다.
     */
    @Test
    void nullRequestThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> scoringService.calculate(null)
        );
    }

    /**
     * 평균 대비 송금액 규칙은 중복 적용하지 않고
     * 가장 높은 단계 하나만 적용해야 합니다.
     */
    @Test
    void transactionTierRulesApplyOnlyHighestScore() {
        // given
        FdsRiskScoreRequest.TransactionRiskInput transaction =
                new FdsRiskScoreRequest.TransactionRiskInput(
                        5.0,          // 평균 대비 5배
                        10_000_000L,  // 1,000만 원
                        0.9,          // 잔액의 90%
                        true          // 평소와 다른 시간
                );

        FdsRiskScoreRequest request =
                new FdsRiskScoreRequest(
                        transaction,
                        null,
                        null,
                        null,
                        null,
                        List.of(),
                        null
                );

        // when
        FdsRiskScoreResponse response =
                scoringService.calculate(request);

        // then
        /*
         * 평균 대비 5배: 7점
         * 1,000만 원 이상: 4점
         * 잔액 90% 이상: 5점
         * 비정상 시간: 2점
         * 합계: 18점
         */
        assertEquals(18, response.transactionScore());
        assertEquals(18, response.totalScore());

        /*
         * 단계형 규칙이 중복됐다면 2배, 300만 원,
         * 잔액 70% 규칙까지 포함되어 4개보다 많아집니다.
         */
        assertEquals(4, response.details().size());
    }

    /**
     * 수취인 위험 점수 합계가 15점을 초과해도
     * 영역 최대 점수인 15점까지만 반영해야 합니다.
     */
    @Test
    void recipientScoreIsCappedAtFifteen() {
        // given
        FdsRiskScoreRequest.RecipientRiskInput recipient =
                new FdsRiskScoreRequest.RecipientRiskInput(
                        true,   // 신규 수취인 +6
                        true,   // 1년 이상 미거래 +3
                        true,   // 위험 의심 계좌 +8
                        false
                );

        FdsRiskScoreRequest request =
                new FdsRiskScoreRequest(
                        null,
                        recipient,
                        null,
                        null,
                        null,
                        List.of(),
                        null
                );

        // when
        FdsRiskScoreResponse response =
                scoringService.calculate(request);

        // then
        // 원래 합은 17점이지만 영역 최대값은 15점입니다.
        assertEquals(15, response.recipientScore());
        assertEquals(15, response.totalScore());

        // Detail에 기록된 실제 반영 점수 합도 15점이어야 합니다.
        int detailScoreSum = response.details()
                .stream()
                .mapToInt(detail -> detail.score())
                .sum();

        assertEquals(15, detailScoreSum);
    }

    /**
     * 신고·차단된 사기계좌 여부는 B-3에서 점수를 부여하지 않습니다.
     * B-4 Hard Rule에서 CRITICAL 등급을 강제할 때 사용합니다.
     */
    @Test
    void confirmedFraudAccountDoesNotAddBaseScore() {
        // given
        FdsRiskScoreRequest.RecipientRiskInput recipient =
                new FdsRiskScoreRequest.RecipientRiskInput(
                        false,
                        false,
                        false,
                        true
                );

        FdsRiskScoreRequest request =
                new FdsRiskScoreRequest(
                        null,
                        recipient,
                        null,
                        null,
                        null,
                        List.of(),
                        null
                );

        // when
        FdsRiskScoreResponse response =
                scoringService.calculate(request);

        // then
        assertEquals(0, response.recipientScore());
        assertEquals(0, response.totalScore());
    }

    /**
     * 거래 빈도 위험 규칙이 많이 발생해도
     * 최대 10점까지만 반영해야 합니다.
     */
    @Test
    void velocityScoreIsCappedAtTen() {
        // given
        FdsRiskScoreRequest.VelocityRiskInput velocity =
                new FdsRiskScoreRequest.VelocityRiskInput(
                        3,      // 10분 내 3회
                        5,      // 30분 내 5회
                        3,      // 10분 내 실패 3회
                        3,      // 30분 내 서로 다른 수취인 3명
                        true    // 누적 금액 급증
                );

        FdsRiskScoreRequest request =
                new FdsRiskScoreRequest(
                        null,
                        null,
                        velocity,
                        null,
                        null,
                        List.of(),
                        null
                );

        // when
        FdsRiskScoreResponse response =
                scoringService.calculate(request);

        // then
        assertEquals(10, response.velocityScore());
        assertEquals(10, response.totalScore());
    }

    /**
     * 접속 단말 위험 점수가 최대 10점으로 제한되는지 확인합니다.
     */
    @Test
    void deviceScoreIsCappedAtTen() {
        // given
        FdsRiskScoreRequest.DeviceRiskInput device =
                new FdsRiskScoreRequest.DeviceRiskInput(
                        true,
                        true,
                        true,
                        true
                );

        FdsRiskScoreRequest request =
                new FdsRiskScoreRequest(
                        null,
                        null,
                        null,
                        device,
                        null,
                        List.of(),
                        null
                );

        // when
        FdsRiskScoreResponse response =
                scoringService.calculate(request);

        // then
        assertEquals(10, response.deviceScore());
        assertEquals(10, response.totalScore());
    }

    /**
     * 앱 행동 위험 점수가 최대 10점으로 제한되는지 확인합니다.
     */
    @Test
    void behaviorScoreIsCappedAtTen() {
        // given
        FdsRiskScoreRequest.BehaviorRiskInput behavior =
                new FdsRiskScoreRequest.BehaviorRiskInput(
                        2,  // 송금 취소
                        3,  // 금액 수정
                        2,  // 수취인 변경
                        3,  // 동일 단계 재진입
                        4,  // 뒤로가기
                        3   // 확인 화면 재진입
                );

        FdsRiskScoreRequest request =
                new FdsRiskScoreRequest(
                        null,
                        null,
                        null,
                        null,
                        behavior,
                        List.of(),
                        null
                );

        // when
        FdsRiskScoreResponse response =
                scoringService.calculate(request);

        // then
        assertEquals(10, response.behaviorScore());
        assertEquals(10, response.totalScore());
    }

    /**
     * 같은 Context 신호가 중복 전달되어도
     * 점수는 한 번만 적용해야 합니다.
     */
    @Test
    void duplicatedContextSignalsAreAppliedOnce() {
        // given
        List<ContextRiskSignal> contextSignals =
                List.of(
                        ContextRiskSignal.AUTHORITY_IMPERSONATION,
                        ContextRiskSignal.AUTHORITY_IMPERSONATION,
                        ContextRiskSignal.SAFE_ACCOUNT_REQUEST,
                        ContextRiskSignal.SAFE_ACCOUNT_REQUEST
                );

        FdsRiskScoreRequest request =
                new FdsRiskScoreRequest(
                        null,
                        null,
                        null,
                        null,
                        null,
                        contextSignals,
                        null
                );

        // when
        FdsRiskScoreResponse response =
                scoringService.calculate(request);

        // then
        // 기관 사칭 6점 + 안전계좌 8점 = 14점
        assertEquals(14, response.contextScore());
        assertEquals(14, response.totalScore());
        assertEquals(2, response.details().size());
    }

    /**
     * Context 신호 합계가 25점을 초과해도
     * Context 최대 점수까지만 반영해야 합니다.
     */
    @Test
    void contextScoreIsCappedAtTwentyFive() {
        // given
        List<ContextRiskSignal> contextSignals =
                List.of(
                        ContextRiskSignal.AUTHORITY_IMPERSONATION,
                        ContextRiskSignal.SAFE_ACCOUNT_REQUEST,
                        ContextRiskSignal.THIRD_PARTY_INSTRUCTION,
                        ContextRiskSignal.CRIME_OR_ACCOUNT_THREAT,
                        ContextRiskSignal.URGENCY,
                        ContextRiskSignal.SECRECY_REQUEST
                );

        FdsRiskScoreRequest request =
                new FdsRiskScoreRequest(
                        null,
                        null,
                        null,
                        null,
                        null,
                        contextSignals,
                        null
                );

        // when
        FdsRiskScoreResponse response =
                scoringService.calculate(request);

        // then
        assertEquals(25, response.contextScore());
        assertEquals(25, response.totalScore());

        int detailScoreSum = response.details()
                .stream()
                .mapToInt(detail -> detail.score())
                .sum();

        assertEquals(25, detailScoreSum);
    }

    /**
     * 가족 사칭 신호는 단독 점수가 없으므로
     * B-3 점수에 포함하지 않습니다.
     */
    @Test
    void familyImpersonationDoesNotAddBaseScore() {
        // given
        FdsRiskScoreRequest request =
                new FdsRiskScoreRequest(
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of(
                                ContextRiskSignal.FAMILY_IMPERSONATION
                        ),
                        null
                );

        // when
        FdsRiskScoreResponse response =
                scoringService.calculate(request);

        // then
        assertEquals(0, response.contextScore());
        assertEquals(0, response.totalScore());
    }

    /**
     * 음성 입력에는 공통 점수와 음성 전용 점수만 적용해야 합니다.
     * 채팅 전용 값이 들어 있어도 점수에 반영되면 안 됩니다.
     */
    @Test
    void voiceInputUsesCommonAndVoiceScoresOnly() {
        // given
        FdsRiskScoreRequest.ConditionRiskInput condition =
                new FdsRiskScoreRequest.ConditionRiskInput(
                        RiskInputType.VOICE,

                        // 공통 입력
                        3.0,   // 응답 지연 +3
                        2,     // 답변 번복 +2
                        2,     // 혼란 표현 +2, 공통 최대 6
                        3,     // 재설명 +2

                        // 음성 입력
                        0.5,   // 발화속도 큰 감소 +2
                        2.0,   // 침묵시간 증가 +2
                        true,  // 긴 침묵 +2, 음성 최대 4
                        true,  // Pitch 변화 +1

                        // 채팅 입력: VOICE이므로 무시되어야 함
                        3.0,
                        3,
                        2,
                        2
                );

        FdsRiskScoreRequest request =
                new FdsRiskScoreRequest(
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of(),
                        condition
                );

        // when
        FdsRiskScoreResponse response =
                scoringService.calculate(request);

        // then
        // 공통 최대 6 + 음성 최대 4
        assertEquals(10, response.conditionScore());
        assertEquals(10, response.totalScore());
    }

    /**
     * 채팅 입력에는 공통 점수와 채팅 전용 점수만 적용해야 합니다.
     * 음성 전용 값이 들어 있어도 점수에 반영되면 안 됩니다.
     */
    @Test
    void chatInputUsesCommonAndChatScoresOnly() {
        // given
        FdsRiskScoreRequest.ConditionRiskInput condition =
                new FdsRiskScoreRequest.ConditionRiskInput(
                        RiskInputType.CHAT,

                        // 공통 입력
                        2.0,   // 응답 지연 +2
                        2,     // 답변 번복 +2
                        2,     // 혼란 표현 +2
                        0,

                        // 음성 입력: CHAT이므로 무시되어야 함
                        0.5,
                        2.0,
                        true,
                        true,

                        // 채팅 입력
                        3.0,   // 작성시간 증가 +1
                        3,     // 내용 반복 수정 +2
                        2,     // 전체 삭제 반복 +1
                        2      // 입력 중단 반복 +1, 채팅 최대 4
                );

        FdsRiskScoreRequest request =
                new FdsRiskScoreRequest(
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of(),
                        condition
                );

        // when
        FdsRiskScoreResponse response =
                scoringService.calculate(request);

        // then
        // 공통 6 + 채팅 최대 4
        assertEquals(10, response.conditionScore());
        assertEquals(10, response.totalScore());
    }

    /**
     * 여러 영역의 점수가 정확하게 합산되는지 확인합니다.
     */
    @Test
    void scoresFromMultipleCategoriesAreSummed() {
        // given
        FdsRiskScoreRequest.TransactionRiskInput transaction =
                new FdsRiskScoreRequest.TransactionRiskInput(
                        2.0,       // +4
                        200_000L,
                        0.2,
                        false
                );

        FdsRiskScoreRequest.RecipientRiskInput recipient =
                new FdsRiskScoreRequest.RecipientRiskInput(
                        true,      // +6
                        false,
                        false,
                        false
                );

        List<ContextRiskSignal> contextSignals =
                List.of(
                        ContextRiskSignal.AUTHORITY_IMPERSONATION, // +6
                        ContextRiskSignal.SAFE_ACCOUNT_REQUEST     // +8
                );

        FdsRiskScoreRequest request =
                new FdsRiskScoreRequest(
                        transaction,
                        recipient,
                        null,
                        null,
                        null,
                        contextSignals,
                        null
                );

        // when
        FdsRiskScoreResponse response =
                scoringService.calculate(request);

        // then
        // 거래 4 + 수취인 6 + Context 14 = 24
        assertEquals(4, response.transactionScore());
        assertEquals(6, response.recipientScore());
        assertEquals(14, response.contextScore());
        assertEquals(24, response.totalScore());
    }
}
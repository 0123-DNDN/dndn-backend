package com.team0123.dndn.fds.service;

import com.team0123.dndn.ai.type.ContextRiskSignal;
import com.team0123.dndn.fds.dto.FdsRiskScoreRequest;
import com.team0123.dndn.fds.dto.FdsRiskScoreResponse;
import com.team0123.dndn.fds.dto.RiskScoreDetail;
import com.team0123.dndn.fds.type.RiskCategory;
import com.team0123.dndn.fds.type.RiskInputType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 전달받은 거래·행동·Context 정보를 이용해
 * Rule-based FDS 위험 점수를 계산하는 서비스입니다.
 * 이 서비스는 DB를 조회하거나 송금 상태를 변경하지 않습니다.
 * LOW, CAUTION, HIGH, CRITICAL 등급도 아직 결정하지 않습니다.
 */
@Service
public class FdsRiskScoringService {

    /**
     * 모든 위험 영역의 점수를 계산합니다.
     */
    public FdsRiskScoreResponse calculate(
            FdsRiskScoreRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "FDS 점수 계산 요청은 null일 수 없습니다."
            );
        }

        // 실제 적용된 규칙을 모두 모읍니다.
        List<RiskScoreDetail> allDetails =
                new ArrayList<>();

        int transactionScore =
                calculateTransactionScore(
                        request.transaction(),
                        allDetails
                );

        int recipientScore =
                calculateRecipientScore(
                        request.recipient(),
                        allDetails
                );

        int velocityScore =
                calculateVelocityScore(
                        request.velocity(),
                        allDetails
                );

        int deviceScore =
                calculateDeviceScore(
                        request.device(),
                        allDetails
                );

        int behaviorScore =
                calculateBehaviorScore(
                        request.behavior(),
                        allDetails
                );

        int contextScore =
                calculateContextScore(
                        request.contextSignals(),
                        allDetails
                );

        int conditionScore =
                calculateConditionScore(
                        request.condition(),
                        allDetails
                );

        // 영역별 최대 점수 합계가 100점이므로 총점도 최대 100점입니다.
        int totalScore = Math.min(
                transactionScore
                        + recipientScore
                        + velocityScore
                        + deviceScore
                        + behaviorScore
                        + contextScore
                        + conditionScore,
                100
        );

        return new FdsRiskScoreResponse(
                transactionScore,
                recipientScore,
                velocityScore,
                deviceScore,
                behaviorScore,
                contextScore,
                conditionScore,
                totalScore,
                List.copyOf(allDetails)
        );
    }

    /**
     * 거래 자체 위험 점수를 계산합니다.
     * 최대 점수: 20점
     */
    private int calculateTransactionScore(
            FdsRiskScoreRequest.TransactionRiskInput input,
            List<RiskScoreDetail> allDetails
    ) {
        ScoreAccumulator score =
                new ScoreAccumulator(
                        RiskCategory.TRANSACTION,
                        20,
                        allDetails
                );

        if (input == null) {
            return 0;
        }

        /*
         * 평균 송금액 대비 증가율은 단계형 규칙입니다.
         * 5배 이상이면 +4와 +7을 동시에 적용하지 않고 +7만 적용합니다.
         */
        if (input.amountRatioToAverage() >= 5.0) {
            score.add(
                    "TRANSACTION_AMOUNT_RATIO_5X",
                    7,
                    "평소보다 송금액이 매우 크게 증가했어요."
            );
        } else if (input.amountRatioToAverage() >= 2.0) {
            score.add(
                    "TRANSACTION_AMOUNT_RATIO_2X",
                    4,
                    "평소보다 송금액이 크게 증가했어요."
            );
        }

        /*
         * 송금액 구간도 가장 높은 조건 하나만 적용합니다.
         */
        if (input.amount() >= 10_000_000L) {
            score.add(
                    "TRANSACTION_VERY_HIGH_AMOUNT",
                    4,
                    "1,000만 원 이상의 매우 큰 금액을 보내려고 해요."
            );
        } else if (input.amount() >= 3_000_000L) {
            score.add(
                    "TRANSACTION_HIGH_AMOUNT",
                    2,
                    "300만 원 이상의 큰 금액을 보내려고 해요."
            );
        }

        /*
         * 잔액 대비 송금 비율 역시 가장 높은 조건 하나만 적용합니다.
         */
        if (input.balanceRatio() >= 0.9) {
            score.add(
                    "TRANSACTION_BALANCE_RATIO_90",
                    5,
                    "계좌 잔액의 대부분을 보내려고 해요."
            );
        } else if (input.balanceRatio() >= 0.7) {
            score.add(
                    "TRANSACTION_BALANCE_RATIO_70",
                    3,
                    "계좌 잔액의 70% 이상을 보내려고 해요."
            );
        }

        if (input.outsideUsualTime()) {
            score.add(
                    "TRANSACTION_UNUSUAL_TIME",
                    2,
                    "평소와 다른 시간대에 송금을 요청했어요."
            );
        }

        return score.value();
    }

    /**
     * 수취인 위험 점수를 계산합니다.
     * 최대 점수: 15점
     */
    private int calculateRecipientScore(
            FdsRiskScoreRequest.RecipientRiskInput input,
            List<RiskScoreDetail> allDetails
    ) {
        ScoreAccumulator score =
                new ScoreAccumulator(
                        RiskCategory.RECIPIENT,
                        15,
                        allDetails
                );

        if (input == null) {
            return 0;
        }

        if (input.newRecipient()) {
            score.add(
                    "RECIPIENT_NEW",
                    6,
                    "처음 송금하는 수취인이에요."
            );
        }

        if (input.inactiveForOneYear()) {
            score.add(
                    "RECIPIENT_INACTIVE_ONE_YEAR",
                    3,
                    "최근 1년 동안 거래하지 않은 수취인이에요."
            );
        }

        if (input.suspectedRiskAccount()) {
            score.add(
                    "RECIPIENT_SUSPECTED_RISK_ACCOUNT",
                    8,
                    "주의가 필요한 계좌 정보와 일치해요."
            );
        }

        /*
         * confirmedFraudAccount는 점수로 처리하지 않습니다.
         * 신고·차단 계좌 일치는 B-4의 Hard Rule에서
         * 최소 CRITICAL 등급을 강제할 때 사용합니다.
         */

        return score.value();
    }

    /**
     * 단시간 거래 빈도 위험 점수를 계산합니다.
     * 최대 점수: 10점
     */
    private int calculateVelocityScore(
            FdsRiskScoreRequest.VelocityRiskInput input,
            List<RiskScoreDetail> allDetails
    ) {
        ScoreAccumulator score =
                new ScoreAccumulator(
                        RiskCategory.VELOCITY,
                        10,
                        allDetails
                );

        if (input == null) {
            return 0;
        }

        if (input.transfersIn10Minutes() >= 3) {
            score.add(
                    "VELOCITY_THREE_IN_TEN_MINUTES",
                    3,
                    "짧은 시간 동안 송금을 반복했어요."
            );
        }

        if (input.transfersIn30Minutes() >= 5) {
            score.add(
                    "VELOCITY_FIVE_IN_THIRTY_MINUTES",
                    5,
                    "30분 안에 송금을 과도하게 반복했어요."
            );
        }

        if (input.failedTransfersIn10Minutes() >= 3) {
            score.add(
                    "VELOCITY_FAILED_THREE_IN_TEN_MINUTES",
                    2,
                    "짧은 시간 동안 송금 실패가 반복됐어요."
            );
        }

        if (input.distinctRecipientsIn30Minutes() >= 3) {
            score.add(
                    "VELOCITY_MULTIPLE_RECIPIENTS",
                    4,
                    "짧은 시간 동안 여러 사람에게 나누어 송금했어요."
            );
        }

        if (input.rapidCumulativeAmountIncrease()) {
            score.add(
                    "VELOCITY_CUMULATIVE_AMOUNT_INCREASE",
                    3,
                    "짧은 시간 동안 누적 송금액이 크게 증가했어요."
            );
        }

        return score.value();
    }

    /**
     * 접속 기기 및 환경 위험 점수를 계산합니다.
     * 최대 점수: 10점
     */
    private int calculateDeviceScore(
            FdsRiskScoreRequest.DeviceRiskInput input,
            List<RiskScoreDetail> allDetails
    ) {
        ScoreAccumulator score =
                new ScoreAccumulator(
                        RiskCategory.DEVICE,
                        10,
                        allDetails
                );

        if (input == null) {
            return 0;
        }

        if (input.newDevice()) {
            score.add(
                    "DEVICE_NEW",
                    4,
                    "등록되지 않은 새로운 기기에서 접속했어요."
            );
        }

        if (input.environmentChanged()) {
            score.add(
                    "DEVICE_ENVIRONMENT_CHANGED",
                    2,
                    "평소와 다른 네트워크나 지역에서 접속했어요."
            );
        }

        if (input.multipleDeviceChanges()) {
            score.add(
                    "DEVICE_MULTIPLE_CHANGES",
                    3,
                    "짧은 시간 동안 접속 기기가 여러 번 변경됐어요."
            );
        }

        if (input.remoteControlEnvironment()) {
            score.add(
                    "DEVICE_REMOTE_CONTROL_ENVIRONMENT",
                    6,
                    "원격제어 또는 위험한 접속 환경이 의심돼요."
            );
        }

        return score.value();
    }

    /**
     * 송금 과정의 앱 행동 이상 점수를 계산합니다.
     * 최대 점수: 10점
     */
    private int calculateBehaviorScore(
            FdsRiskScoreRequest.BehaviorRiskInput input,
            List<RiskScoreDetail> allDetails
    ) {
        ScoreAccumulator score =
                new ScoreAccumulator(
                        RiskCategory.BEHAVIOR,
                        10,
                        allDetails
                );

        if (input == null) {
            return 0;
        }

        if (input.transferCancelCount() >= 2) {
            score.add(
                    "BEHAVIOR_TRANSFER_CANCEL_REPEAT",
                    2,
                    "송금 취소가 여러 번 반복됐어요."
            );
        }

        if (input.amountEditCount() >= 3) {
            score.add(
                    "BEHAVIOR_AMOUNT_EDIT_REPEAT",
                    2,
                    "송금 금액을 여러 번 수정했어요."
            );
        }

        if (input.recipientChangeCount() >= 2) {
            score.add(
                    "BEHAVIOR_RECIPIENT_CHANGE_REPEAT",
                    3,
                    "송금할 사람을 여러 번 변경했어요."
            );
        }

        if (input.sameStepReentryCount() >= 3) {
            score.add(
                    "BEHAVIOR_SAME_STEP_REENTRY",
                    2,
                    "같은 송금 화면에 여러 번 다시 들어왔어요."
            );
        }

        if (input.backNavigationCount() >= 4) {
            score.add(
                    "BEHAVIOR_BACK_NAVIGATION_REPEAT",
                    1,
                    "송금 과정에서 뒤로가기를 여러 번 반복했어요."
            );
        }

        if (input.confirmationReentryCount() >= 3) {
            score.add(
                    "BEHAVIOR_CONFIRMATION_REENTRY",
                    2,
                    "최종 확인 화면을 여러 번 다시 확인했어요."
            );
        }

        return score.value();
    }

    /**
     * B-2에서 탐지한 Context 신호를 점수로 변환합니다.
     * 최대 점수: 25점
     */
    private int calculateContextScore(
            List<ContextRiskSignal> inputSignals,
            List<RiskScoreDetail> allDetails
    ) {
        ScoreAccumulator score =
                new ScoreAccumulator(
                        RiskCategory.CONTEXT,
                        25,
                        allDetails
                );

        if (inputSignals == null || inputSignals.isEmpty()) {
            return 0;
        }

        /*
         * EnumSet으로 변환하여 같은 위험 신호가 여러 번 전달돼도
         * 점수는 한 번만 적용되도록 합니다.
         */
        Set<ContextRiskSignal> signals =
                EnumSet.noneOf(ContextRiskSignal.class);

        for (ContextRiskSignal signal : inputSignals) {
            if (signal != null) {
                signals.add(signal);
            }
        }

        /*
         * 입력 배열 순서가 아니라 고정된 enum 순서로 계산하여
         * 항상 같은 결과가 나오도록 합니다.
         */
        for (ContextRiskSignal signal : ContextRiskSignal.values()) {
            if (!signals.contains(signal)) {
                continue;
            }

            switch (signal) {
                case AUTHORITY_IMPERSONATION -> score.add(
                        "CONTEXT_AUTHORITY_IMPERSONATION",
                        6,
                        "검찰·경찰 등 기관의 권위를 이용한 정황이 있어요."
                );

                case SAFE_ACCOUNT_REQUEST -> score.add(
                        "CONTEXT_SAFE_ACCOUNT_REQUEST",
                        8,
                        "안전계좌나 보호계좌로 송금을 요구받았어요."
                );

                case THIRD_PARTY_INSTRUCTION -> score.add(
                        "CONTEXT_THIRD_PARTY_INSTRUCTION",
                        6,
                        "다른 사람이 특정 계좌로 송금하도록 지시했어요."
                );

                case CRIME_OR_ACCOUNT_THREAT -> score.add(
                        "CONTEXT_CRIME_OR_ACCOUNT_THREAT",
                        6,
                        "범죄 연루나 계좌 동결을 이유로 압박받고 있어요."
                );

                case URGENCY -> score.add(
                        "CONTEXT_URGENCY",
                        4,
                        "빠르게 송금하도록 시간 압박을 받고 있어요."
                );

                case SECRECY_REQUEST -> score.add(
                        "CONTEXT_SECRECY_REQUEST",
                        5,
                        "가족이나 은행에 알리지 말라는 요구를 받았어요."
                );

                case LOAN_UPFRONT_PAYMENT -> score.add(
                        "CONTEXT_LOAN_UPFRONT_PAYMENT",
                        6,
                        "대출 실행 전에 돈을 먼저 보내라는 요구를 받았어요."
                );

                case REMOTE_CONTROL_REQUEST -> score.add(
                        "CONTEXT_REMOTE_CONTROL_REQUEST",
                        8,
                        "원격제어 앱 설치나 화면 공유를 요구받았어요."
                );

                case UNCLEAR_TRANSFER_PURPOSE -> score.add(
                        "CONTEXT_UNCLEAR_TRANSFER_PURPOSE",
                        2,
                        "송금 목적이 명확하지 않아요."
                );

                /*
                 * 가족 사칭은 단독 점수가 정해지지 않았습니다.
                 * B-4 Combination Rule에서 신규 수취인, 긴급성과 함께 판단합니다.
                 */
                case FAMILY_IMPERSONATION -> {
                    // B-3에서는 점수를 추가하지 않습니다.
                }
            }
        }

        return score.value();
    }

    /**
     * 음성 또는 채팅 상태 변화 점수를 계산합니다.
     * 공통 점수: 최대 6점
     * 입력 방식별 전용 점수: 최대 4점
     * 최종 최대 점수: 10점
     */
    private int calculateConditionScore(
            FdsRiskScoreRequest.ConditionRiskInput input,
            List<RiskScoreDetail> allDetails
    ) {
        if (input == null) {
            return 0;
        }

        ScoreAccumulator commonScore =
                new ScoreAccumulator(
                        RiskCategory.CONDITION,
                        6,
                        allDetails
                );

        /*
         * 응답 지연은 단계형 규칙이므로
         * 3배 이상이면 +2와 +3을 더하지 않고 +3만 적용합니다.
         */
        if (input.responseDelayRatio() >= 3.0) {
            commonScore.add(
                    "CONDITION_RESPONSE_DELAY_3X",
                    3,
                    "평소보다 응답이 매우 늦어졌어요."
            );
        } else if (input.responseDelayRatio() >= 2.0) {
            commonScore.add(
                    "CONDITION_RESPONSE_DELAY_2X",
                    2,
                    "평소보다 응답이 늦어졌어요."
            );
        }

        if (input.answerReversalCount() >= 2) {
            commonScore.add(
                    "CONDITION_ANSWER_REVERSAL",
                    2,
                    "같은 질문에 대한 답변이 여러 번 바뀌었어요."
            );
        }

        if (input.confusionCount() >= 2) {
            commonScore.add(
                    "CONDITION_CONFUSION_REPEAT",
                    2,
                    "혼란스러운 표현이 여러 번 나타났어요."
            );
        }

        if (input.reexplanationCount() >= 3) {
            commonScore.add(
                    "CONDITION_REEXPLANATION_REPEAT",
                    2,
                    "같은 내용을 여러 번 다시 설명해야 했어요."
            );
        }

        ScoreAccumulator inputSpecificScore =
                new ScoreAccumulator(
                        RiskCategory.CONDITION,
                        4,
                        allDetails
                );

        if (input.inputType() == RiskInputType.VOICE) {
            calculateVoiceConditionScore(
                    input,
                    inputSpecificScore
            );
        } else if (input.inputType() == RiskInputType.CHAT) {
            calculateChatConditionScore(
                    input,
                    inputSpecificScore
            );
        }

        return Math.min(
                commonScore.value()
                        + inputSpecificScore.value(),
                10
        );
    }

    /**
     * 음성 전용 상태 변화 점수를 계산합니다.
     * 최대 점수: 4점
     */
    private void calculateVoiceConditionScore(
            FdsRiskScoreRequest.ConditionRiskInput input,
            ScoreAccumulator score
    ) {
        /*
         * 발화속도 감소는 가장 높은 단계 하나만 적용합니다.
         */
        if (input.speechRateDecreaseRatio() >= 0.5) {
            score.add(
                    "CONDITION_VOICE_SPEECH_RATE_DECREASE_50",
                    2,
                    "평소보다 말하는 속도가 크게 느려졌어요."
            );
        } else if (input.speechRateDecreaseRatio() >= 0.3) {
            score.add(
                    "CONDITION_VOICE_SPEECH_RATE_DECREASE_30",
                    1,
                    "평소보다 말하는 속도가 느려졌어요."
            );
        }

        if (input.pauseIncreaseRatio() >= 2.0) {
            score.add(
                    "CONDITION_VOICE_PAUSE_INCREASE",
                    2,
                    "평소보다 말 사이의 침묵 시간이 길어졌어요."
            );
        }

        if (input.longPauseRepeated()) {
            score.add(
                    "CONDITION_VOICE_LONG_PAUSE_REPEAT",
                    2,
                    "긴 침묵이 여러 차례 반복됐어요."
            );
        }

        if (input.pitchChanged()) {
            score.add(
                    "CONDITION_VOICE_PITCH_CHANGED",
                    1,
                    "평소와 다른 음높이 변화가 확인됐어요."
            );
        }
    }

    /**
     * 채팅 전용 상태 변화 점수를 계산합니다.
     * 최대 점수: 4점
     */
    private void calculateChatConditionScore(
            FdsRiskScoreRequest.ConditionRiskInput input,
            ScoreAccumulator score
    ) {
        if (input.typingDurationRatio() >= 3.0) {
            score.add(
                    "CONDITION_CHAT_TYPING_DURATION_3X",
                    1,
                    "평소보다 메시지 작성 시간이 길어졌어요."
            );
        }

        if (input.textEditCount() >= 3) {
            score.add(
                    "CONDITION_CHAT_TEXT_EDIT_REPEAT",
                    2,
                    "메시지를 보내기 전에 내용을 여러 번 수정했어요."
            );
        }

        if (input.fullDeleteCount() >= 2) {
            score.add(
                    "CONDITION_CHAT_FULL_DELETE_REPEAT",
                    1,
                    "작성한 내용을 지우고 다시 입력하는 행동이 반복됐어요."
            );
        }

        if (input.typingPauseCount() >= 2) {
            score.add(
                    "CONDITION_CHAT_TYPING_PAUSE_REPEAT",
                    1,
                    "메시지를 작성하다가 오래 멈추는 행동이 반복됐어요."
            );
        }
    }

    /**
     * 영역별 최대 점수를 넘지 않도록 점수를 누적하는 내부 클래스입니다.
     * 예를 들어 Context 점수가 이미 20점이고
     * 8점 규칙이 추가되면 최대 25점까지만 5점을 반영합니다.
     */
    private static class ScoreAccumulator {

        private final RiskCategory category;
        private final int maximumScore;
        private final List<RiskScoreDetail> allDetails;

        private int score;

        private ScoreAccumulator(
                RiskCategory category,
                int maximumScore,
                List<RiskScoreDetail> allDetails
        ) {
            this.category = category;
            this.maximumScore = maximumScore;
            this.allDetails = allDetails;
            this.score = 0;
        }

        /**
         * 최대 점수 내에서 실제 반영 가능한 점수만 추가합니다.
         */
        private void add(
                String ruleCode,
                int requestedScore,
                String reason
        ) {
            int remainingScore =
                    maximumScore - score;

            if (remainingScore <= 0 || requestedScore <= 0) {
                return;
            }

            int appliedScore =
                    Math.min(requestedScore, remainingScore);

            score += appliedScore;

            allDetails.add(
                    new RiskScoreDetail(
                            category,
                            ruleCode,
                            appliedScore,
                            reason
                    )
            );
        }

        private int value() {
            return score;
        }
    }
}
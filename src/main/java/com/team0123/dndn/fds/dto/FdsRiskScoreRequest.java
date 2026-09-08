package com.team0123.dndn.fds.dto;

import com.team0123.dndn.ai.type.ContextRiskSignal;
import com.team0123.dndn.fds.type.RiskInputType;

import java.util.List;

/**
 * Rule-based FDS 점수 계산에 필요한 입력값입니다.
 * 조회하거나 계산한 데이터를 이 DTO로 전달합니다.
 */
public record FdsRiskScoreRequest(

        // 거래 자체 위험 정보
        TransactionRiskInput transaction,

        // 수취인 위험 정보
        RecipientRiskInput recipient,

        // 거래 빈도 정보
        VelocityRiskInput velocity,

        // 접속 기기 및 환경 정보
        DeviceRiskInput device,

        // 송금 과정의 앱 행동 정보
        BehaviorRiskInput behavior,

        // B-2에서 탐지한 대화 Context 위험 신호
        List<ContextRiskSignal> contextSignals,

        // 음성 또는 채팅 상태 변화 정보
        ConditionRiskInput condition

) {

    /**
     * 거래 자체 위험 입력입니다.
     */
    public record TransactionRiskInput(

            // 최근 평균 송금액 대비 현재 송금액 비율
            // 예: 평균 20만 원, 현재 100만 원이면 5.0
            double amountRatioToAverage,

            // 현재 송금액
            long amount,

            // 출금계좌 잔액에서 현재 송금액이 차지하는 비율
            // 예: 잔액 100만 원 중 70만 원 송금이면 0.7
            double balanceRatio,

            // 사용자의 평소 활동시간을 벗어난 거래인지 여부
            boolean outsideUsualTime

    ) {
    }

    /**
     * 수취인 위험 입력입니다.
     */
    public record RecipientRiskInput(

            // 처음 송금하는 수취인인지 여부
            boolean newRecipient,

            // 최근 1년 동안 거래하지 않은 수취인인지 여부
            boolean inactiveForOneYear,

            // 의심·주의 계좌 DB와 일치하는지 여부
            boolean suspectedRiskAccount,

            /*
             * 신고·차단된 사기계좌 DB와 일치하는지 여부입니다.
             *
             * B-3에서는 점수를 적용하지 않고,
             * B-4 Hard Rule 판단에 사용할 수 있도록 보존합니다.
             */
            boolean confirmedFraudAccount

    ) {
    }

    /**
     * 단시간 거래 빈도 위험 입력입니다.
     */
    public record VelocityRiskInput(

            // 최근 10분 이내 송금 횟수
            int transfersIn10Minutes,

            // 최근 30분 이내 송금 횟수
            int transfersIn30Minutes,

            // 최근 10분 이내 송금 실패 횟수
            int failedTransfersIn10Minutes,

            // 최근 30분 이내 송금한 서로 다른 수취인 수
            int distinctRecipientsIn30Minutes,

            /*
             * 최근 1시간 누적 송금액이
             * 평소 1시간 평균의 3배 이상인지 여부
             */
            boolean rapidCumulativeAmountIncrease

    ) {
    }

    /**
     * 접속 기기 및 환경 위험 입력입니다.
     */
    public record DeviceRiskInput(

            // 등록되지 않은 새로운 기기인지 여부
            boolean newDevice,

            // 평소와 다른 네트워크 또는 지역인지 여부
            boolean environmentChanged,

            // 단기간에 여러 기기로 변경됐는지 여부
            boolean multipleDeviceChanges,

            // 원격제어 또는 악성 환경이 탐지됐는지 여부
            boolean remoteControlEnvironment

    ) {
    }

    /**
     * 송금 과정에서 발생한 앱 행동 위험 입력입니다.
     */
    public record BehaviorRiskInput(

            // 송금을 취소한 횟수
            int transferCancelCount,

            // 송금 금액을 수정한 횟수
            int amountEditCount,

            // 수취인을 변경한 횟수
            int recipientChangeCount,

            // 동일 송금 단계에 다시 진입한 횟수
            int sameStepReentryCount,

            // 송금 과정에서 뒤로가기를 누른 횟수
            int backNavigationCount,

            // 최종 확인 화면에 다시 진입한 횟수
            int confirmationReentryCount

    ) {
    }

    /**
     * 음성·채팅 상태 변화 위험 입력입니다.
     */
    public record ConditionRiskInput(

            // VOICE 또는 CHAT
            RiskInputType inputType,

            /*
             * 음성·채팅 공통 입력
             */

            // 개인 baseline 대비 응답 지연 비율
            double responseDelayRatio,

            // 동일 질문에 답변을 번복한 횟수
            int answerReversalCount,

            // "잘 모르겠어요", "헷갈려요" 등 혼란 표현 횟수
            int confusionCount,

            // 같은 내용을 다른 방식으로 다시 설명한 횟수
            int reexplanationCount,

            /*
             * 음성 전용 입력
             */

            // 개인 baseline 대비 발화속도 감소 비율
            // 예: 30% 감소는 0.3, 50% 감소는 0.5
            double speechRateDecreaseRatio,

            // 개인 baseline 대비 평균 침묵시간 증가 비율
            // 예: 평균의 2배가 됐다면 2.0
            double pauseIncreaseRatio,

            // 긴 침묵이 여러 차례 반복됐는지 여부
            boolean longPauseRepeated,

            // 개인 baseline 대비 음높이 패턴이 크게 변했는지 여부
            boolean pitchChanged,

            /*
             * 채팅 전용 입력
             */

            // 개인 baseline 대비 메시지 작성시간 비율
            double typingDurationRatio,

            // 전송 전 입력 내용을 크게 수정한 횟수
            int textEditCount,

            // 입력 내용을 전체 삭제한 횟수
            int fullDeleteCount,

            // 메시지 작성 중 비정상적으로 오래 멈춘 횟수
            int typingPauseCount

    ) {
    }
}
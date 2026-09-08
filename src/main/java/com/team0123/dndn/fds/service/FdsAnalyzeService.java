package com.team0123.dndn.fds.service;

import com.team0123.dndn.ai.dto.ContextAnalyzeResponse;
import com.team0123.dndn.ai.service.TransferContextService;
import com.team0123.dndn.ai.type.ContextRiskSignal;
import com.team0123.dndn.fds.dto.FdsAnalyzeRequest;
import com.team0123.dndn.fds.dto.FdsAnalyzeResponse;
import com.team0123.dndn.fds.dto.FdsRiskScoreRequest;
import com.team0123.dndn.fds.dto.FdsRiskScoreResponse;
import com.team0123.dndn.fds.type.FdsDecisionRule;
import com.team0123.dndn.fds.type.RecommendedAction;
import com.team0123.dndn.fds.type.RiskInputType;
import com.team0123.dndn.fds.type.RiskLevel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Context 분석, 점수 계산, Hard/Combination Rule 판단을 묶어
 * 최종 FDS 분석 결과를 만드는 서비스입니다.
 * 이 서비스는 실제 송금을 진행하거나 보류하지 않습니다.
 * 최종 판단과 권장 대응만 반환하고, 실제 처리는 A가 담당합니다.
 */
@Service
public class FdsAnalyzeService {

    private final TransferContextService transferContextService;
    private final FdsRiskScoringService riskScoringService;

    public FdsAnalyzeService(
            TransferContextService transferContextService,
            FdsRiskScoringService riskScoringService
    ) {
        this.transferContextService = transferContextService;
        this.riskScoringService = riskScoringService;
    }

    /**
     * 전달받은 모든 위험 정보를 이용하여 최종 위험도를 판단합니다.
     */
    public FdsAnalyzeResponse analyze(FdsAnalyzeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "FDS 분석 요청은 null일 수 없습니다."
            );
        }

        // 사용자가 말한 송금 목적에서 위험 Context를 탐지합니다.
        ContextAnalyzeResponse contextResult =
                transferContextService.analyze(request.purposeText());

        // Context 분석 결과와 A/C가 전달한 정보를 점수 계산 입력으로 합칩니다.
        FdsRiskScoreRequest scoreRequest =
                new FdsRiskScoreRequest(
                        request.transaction(),
                        request.recipient(),
                        request.velocity(),
                        request.device(),
                        request.behavior(),
                        contextResult.detectedSignals(),
                        request.condition()
                );

        FdsRiskScoreResponse scoreResponse =
                riskScoringService.calculate(scoreRequest);

        // 먼저 총점을 기준으로 기본 위험 등급을 정합니다.
        RiskLevel riskLevel =
                RiskLevel.fromScore(scoreResponse.totalScore());

        List<FdsDecisionRule> triggeredRules =
                new ArrayList<>();

        // 신고·차단 사기계좌는 현재 적용하는 유일한 Hard Rule입니다.
        boolean hardRuleTriggered =
                isConfirmedFraudAccount(request.recipient());

        if (hardRuleTriggered) {
            triggeredRules.add(
                    FdsDecisionRule.CONFIRMED_FRAUD_ACCOUNT
            );
            riskLevel = riskLevel.atLeast(RiskLevel.CRITICAL);
        }

        // 조합 조건이 충족되면 점수 등급보다 낮지 않도록 최소 등급을 적용합니다.
        List<RuleMatch> combinationMatches =
                findCombinationRules(
                        request,
                        contextResult.detectedSignals()
                );

        for (RuleMatch match : combinationMatches) {
            triggeredRules.add(match.rule());
            riskLevel = riskLevel.atLeast(match.minimumLevel());
        }

        List<String> reasons = collectReasons(
                scoreResponse,
                contextResult,
                hardRuleTriggered,
                combinationMatches
        );

        return new FdsAnalyzeResponse(
                riskLevel,
                scoreResponse.totalScore(),
                RecommendedAction.from(riskLevel),
                hardRuleTriggered,
                !combinationMatches.isEmpty(),
                List.copyOf(triggeredRules),
                reasons,
                scoreResponse,
                contextResult.analysisSucceeded()
        );
    }

    private boolean isConfirmedFraudAccount(
            FdsRiskScoreRequest.RecipientRiskInput recipient
    ) {
        return recipient != null
                && recipient.confirmedFraudAccount();
    }

    /**
     * 표에서 합의한 신호 조합을 검사합니다.
     * 하나의 요청에서 여러 조합이 동시에 발동할 수 있습니다.
     */
    private List<RuleMatch> findCombinationRules(
            FdsAnalyzeRequest request,
            List<ContextRiskSignal> detectedSignals
    ) {
        Set<ContextRiskSignal> signals =
                toSignalSet(detectedSignals);

        FdsRiskScoreRequest.TransactionRiskInput transaction =
                request.transaction();
        FdsRiskScoreRequest.RecipientRiskInput recipient =
                request.recipient();
        FdsRiskScoreRequest.VelocityRiskInput velocity =
                request.velocity();
        FdsRiskScoreRequest.DeviceRiskInput device =
                request.device();
        FdsRiskScoreRequest.BehaviorRiskInput behavior =
                request.behavior();
        FdsRiskScoreRequest.ConditionRiskInput condition =
                request.condition();

        boolean newRecipient = recipient != null
                && recipient.newRecipient();
        boolean newDevice = device != null
                && device.newDevice();
        boolean highComparedWithAverage = transaction != null
                && transaction.amountRatioToAverage() >= 2.0;
        boolean sendsMostOfBalance = transaction != null
                && transaction.balanceRatio() >= 0.7;

        List<RuleMatch> matches = new ArrayList<>();

        addMatch(
                matches,
                hasAll(signals,
                        ContextRiskSignal.AUTHORITY_IMPERSONATION,
                        ContextRiskSignal.SAFE_ACCOUNT_REQUEST,
                        ContextRiskSignal.THIRD_PARTY_INSTRUCTION),
                FdsDecisionRule.INSTITUTION_IMPERSONATION,
                RiskLevel.CRITICAL,
                "기관 사칭과 안전계좌 송금 지시가 함께 탐지됐어요."
        );

        addMatch(
                matches,
                hasAll(signals,
                        ContextRiskSignal.AUTHORITY_IMPERSONATION,
                        ContextRiskSignal.CRIME_OR_ACCOUNT_THREAT,
                        ContextRiskSignal.URGENCY),
                FdsDecisionRule.AUTHORITY_THREAT,
                RiskLevel.HIGH,
                "기관을 사칭하며 범죄 연루를 이유로 긴급 송금을 요구했어요."
        );

        addMatch(
                matches,
                hasAll(signals,
                        ContextRiskSignal.THIRD_PARTY_INSTRUCTION,
                        ContextRiskSignal.URGENCY,
                        ContextRiskSignal.SECRECY_REQUEST),
                FdsDecisionRule.SECRET_TRANSFER,
                RiskLevel.HIGH,
                "제3자의 지시와 긴급성, 비밀 유지 요구가 함께 탐지됐어요."
        );

        addMatch(
                matches,
                signals.contains(ContextRiskSignal.LOAN_UPFRONT_PAYMENT)
                        && newRecipient,
                FdsDecisionRule.LOAN_SCAM,
                RiskLevel.HIGH,
                "대출 선입금을 처음 보는 수취인에게 보내려고 해요."
        );

        addMatch(
                matches,
                signals.contains(ContextRiskSignal.REMOTE_CONTROL_REQUEST)
                        && signals.contains(ContextRiskSignal.THIRD_PARTY_INSTRUCTION),
                FdsDecisionRule.REMOTE_CONTROL,
                RiskLevel.CRITICAL,
                "원격제어 요구와 제3자의 송금 지시가 함께 탐지됐어요."
        );

        addMatch(
                matches,
                newDevice && newRecipient && highComparedWithAverage,
                FdsDecisionRule.ACCOUNT_TAKEOVER,
                RiskLevel.HIGH,
                "새 기기에서 신규 수취인에게 평소보다 큰 금액을 보내려고 해요."
        );

        addMatch(
                matches,
                newRecipient && sendsMostOfBalance && highComparedWithAverage,
                FdsDecisionRule.RAPID_FUND_MOVEMENT,
                RiskLevel.HIGH,
                "신규 수취인에게 잔액 대부분을 평소보다 큰 금액으로 보내려고 해요."
        );

        addMatch(
                matches,
                behavior != null
                        && behavior.recipientChangeCount() >= 2
                        && behavior.amountEditCount() >= 3
                        && behavior.transferCancelCount() >= 2,
                FdsDecisionRule.MANIPULATION_CONFUSION,
                RiskLevel.CAUTION,
                "수취인·금액 변경과 송금 취소가 반복됐어요."
        );

        addMatch(
                matches,
                countRiskContextSignals(signals) >= 2
                        && countConditionSignals(condition) >= 2,
                FdsDecisionRule.CONTEXT_AND_CONDITION,
                RiskLevel.HIGH,
                "위험한 대화 내용과 평소와 다른 상태 변화가 함께 나타났어요."
        );

        addMatch(
                matches,
                signals.contains(ContextRiskSignal.SAFE_ACCOUNT_REQUEST)
                        && signals.contains(ContextRiskSignal.URGENCY)
                        && newRecipient,
                FdsDecisionRule.SAFE_ACCOUNT_PATTERN,
                RiskLevel.CRITICAL,
                "신규 수취인의 안전계좌로 긴급 송금을 요구받았어요."
        );

        addMatch(
                matches,
                signals.contains(ContextRiskSignal.FAMILY_IMPERSONATION)
                        && signals.contains(ContextRiskSignal.URGENCY)
                        && newRecipient,
                FdsDecisionRule.FAMILY_IMPERSONATION,
                RiskLevel.HIGH,
                "가족을 사칭해 신규 수취인에게 긴급 송금을 요구한 정황이 있어요."
        );

        return matches;
    }

    private Set<ContextRiskSignal> toSignalSet(
            List<ContextRiskSignal> detectedSignals
    ) {
        Set<ContextRiskSignal> signals =
                EnumSet.noneOf(ContextRiskSignal.class);

        if (detectedSignals != null) {
            for (ContextRiskSignal signal : detectedSignals) {
                if (signal != null) {
                    signals.add(signal);
                }
            }
        }

        return signals;
    }

    private boolean hasAll(
            Set<ContextRiskSignal> signals,
            ContextRiskSignal... requiredSignals
    ) {
        for (ContextRiskSignal requiredSignal : requiredSignals) {
            if (!signals.contains(requiredSignal)) {
                return false;
            }
        }
        return true;
    }

    private int countRiskContextSignals(
            Set<ContextRiskSignal> signals
    ) {
        return signals.size();
    }

    /**
     * Context+혼란형 판단을 위해 임계값을 넘은 상태 변화의 개수를 셉니다.
     * 공통 신호와 현재 입력 방식(VOICE/CHAT)의 전용 신호만 계산합니다.
     */
    private int countConditionSignals(
            FdsRiskScoreRequest.ConditionRiskInput condition
    ) {
        if (condition == null) {
            return 0;
        }

        int count = 0;

        if (condition.responseDelayRatio() >= 2.0) count++;
        if (condition.answerReversalCount() >= 2) count++;
        if (condition.confusionCount() >= 2) count++;
        if (condition.reexplanationCount() >= 3) count++;

        if (condition.inputType() == RiskInputType.VOICE) {
            if (condition.speechRateDecreaseRatio() >= 0.3) count++;
            if (condition.pauseIncreaseRatio() >= 2.0) count++;
            if (condition.longPauseRepeated()) count++;
            if (condition.pitchChanged()) count++;
        } else if (condition.inputType() == RiskInputType.CHAT) {
            if (condition.typingDurationRatio() >= 3.0) count++;
            if (condition.textEditCount() >= 3) count++;
            if (condition.fullDeleteCount() >= 2) count++;
            if (condition.typingPauseCount() >= 2) count++;
        }

        return count;
    }

    private void addMatch(
            List<RuleMatch> matches,
            boolean matched,
            FdsDecisionRule rule,
            RiskLevel minimumLevel,
            String reason
    ) {
        if (matched) {
            matches.add(
                    new RuleMatch(rule, minimumLevel, reason)
            );
        }
    }

    /**
     * 점수 규칙, Gemini 설명, Hard/Combination Rule 설명을 합치고
     * 같은 문장은 한 번만 반환합니다.
     */
    private List<String> collectReasons(
            FdsRiskScoreResponse scoreResponse,
            ContextAnalyzeResponse contextResult,
            boolean hardRuleTriggered,
            List<RuleMatch> combinationMatches
    ) {
        Set<String> reasons = new LinkedHashSet<>();

        scoreResponse.details().forEach(
                detail -> reasons.add(detail.reason())
        );

        if (contextResult.contextReasons() != null) {
            reasons.addAll(contextResult.contextReasons());
        }

        if (hardRuleTriggered) {
            reasons.add(
                    "신고되었거나 차단된 사기계좌와 일치해요."
            );
        }

        combinationMatches.forEach(
                match -> reasons.add(match.reason())
        );

        return List.copyOf(reasons);
    }

    /**
     * 발동한 조합 규칙과 해당 규칙이 보장하는 최소 등급입니다.
     */
    private record RuleMatch(
            FdsDecisionRule rule,
            RiskLevel minimumLevel,
            String reason
    ) {
    }
}

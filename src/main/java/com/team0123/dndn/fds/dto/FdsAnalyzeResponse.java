package com.team0123.dndn.fds.dto;

import com.team0123.dndn.fds.type.FdsDecisionRule;
import com.team0123.dndn.fds.type.RecommendedAction;
import com.team0123.dndn.fds.type.RiskLevel;

import java.util.List;

/**
 * POST /api/fds/analyze 최종 응답 DTO입니다.
 */
public record FdsAnalyzeResponse(

        // 최종 위험 등급
        RiskLevel riskLevel,

        // B-3에서 계산한 총 위험 점수
        int riskScore,

        // A에게 전달하는 권장 대응
        RecommendedAction recommendedAction,

        // 신고·차단 사기계좌 Hard Rule 발동 여부
        boolean hardRuleTriggered,

        // Combination Rule이 하나 이상 발동했는지 여부
        boolean combinationRuleTriggered,

        // 실제로 최종 판단에 적용된 Hard/Combination Rule
        List<FdsDecisionRule> triggeredRules,

        // 사용자에게 보여줄 수 있는 위험 사유
        List<String> reasons,

        // 영역별 점수와 세부 점수
        FdsRiskScoreResponse scoreBreakdown,

        // Gemini Context 분석 성공 여부
        boolean contextAnalysisSucceeded

) {
}
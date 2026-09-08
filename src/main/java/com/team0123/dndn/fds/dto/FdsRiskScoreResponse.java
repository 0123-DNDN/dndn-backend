package com.team0123.dndn.fds.dto;

import java.util.List;

/**
 * Rule-based FDS 점수 계산 결과입니다.
 *
 * 이 단계에서는 LOW, CAUTION, HIGH, CRITICAL을 판단하지 않습니다.
 * 최종 위험 등급은 B-4에서 결정합니다.
 */
public record FdsRiskScoreResponse(

        // 거래 자체 위험 점수, 최대 20점
        int transactionScore,

        // 수취인 위험 점수, 최대 15점
        int recipientScore,

        // 거래 빈도 위험 점수, 최대 10점
        int velocityScore,

        // 접속 단말 위험 점수, 최대 10점
        int deviceScore,

        // 앱 행동 이상 점수, 최대 10점
        int behaviorScore,

        // 대화 Context 위험 점수, 최대 25점
        int contextScore,

        // 음성 또는 채팅 상태 변화 점수, 최대 10점
        int conditionScore,

        // 모든 영역 점수의 합계, 최대 100점
        int totalScore,

        // 실제로 적용된 위험 규칙 목록
        List<RiskScoreDetail> details

) {
}
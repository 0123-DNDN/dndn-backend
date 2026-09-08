package com.team0123.dndn.fds.dto;

import com.team0123.dndn.fds.type.RiskCategory;

/**
 * 실제로 적용된 개별 위험 규칙 결과입니다.
 */
public record RiskScoreDetail(

        // 위험 규칙이 속한 영역
        RiskCategory category,

        // 서버 내부에서 규칙을 식별하기 위한 코드
        String ruleCode,

        // 해당 규칙으로 추가된 점수
        int score,

        // 사용자에게 보여줄 수 있는 쉬운 한국어 설명
        String reason

) {
}
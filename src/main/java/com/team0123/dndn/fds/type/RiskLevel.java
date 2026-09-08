package com.team0123.dndn.fds.type;

/**
 * Context-FDS의 최종 위험 등급입니다.
 */
public enum RiskLevel {

    // 0~24점: 정상
    LOW(0),

    // 25~44점: 주의
    CAUTION(1),

    // 45~69점: 위험
    HIGH(2),

    // 70점 이상 또는 Hard Rule: 고위험
    CRITICAL(3);

    private final int priority;

    RiskLevel(int priority) {
        this.priority = priority;
    }

    /**
     * 기본 점수를 위험 등급으로 변환합니다.
     */
    public static RiskLevel fromScore(int score) {
        if (score >= 70) {
            return CRITICAL;
        }

        if (score >= 45) {
            return HIGH;
        }

        if (score >= 25) {
            return CAUTION;
        }

        return LOW;
    }

    /**
     * 현재 등급과 Combination/Hard Rule 등급 중
     * 더 위험한 등급을 반환합니다.
     * 예:
     * 점수 등급 CAUTION + 조합 규칙 HIGH
     * → HIGH
     * 점수 등급 CRITICAL + 조합 규칙 HIGH
     * → CRITICAL 유지
     */
    public RiskLevel atLeast(RiskLevel minimumLevel) {
        if (minimumLevel == null) {
            return this;
        }

        return this.priority >= minimumLevel.priority
                ? this
                : minimumLevel;
    }
}
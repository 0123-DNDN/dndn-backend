package com.team0123.dndn.fds.type;

/**
 * B가 A에게 전달하는 권장 대응입니다.
 * B는 실제 송금 상태를 변경하지 않습니다.
 * A가 이 값을 참고하여 송금 흐름을 처리합니다.
 */
public enum RecommendedAction {
    // 정상 송금 진행
    PROCEED,

    // 사용자에게 한 번 더 확인
    RECONFIRM,

    // 위험 이유를 설명하고 다시 확인
    WARN,

    // 송금 보류 및 가족 승인 제안
    HOLD;

    /**
     * 최종 위험 등급을 권장 대응으로 변환합니다.
     */
    public static RecommendedAction from(
            RiskLevel riskLevel
    ) {
        return switch (riskLevel) {
            case LOW -> PROCEED;
            case CAUTION -> RECONFIRM;
            case HIGH -> WARN;
            case CRITICAL -> HOLD;
        };
    }
}
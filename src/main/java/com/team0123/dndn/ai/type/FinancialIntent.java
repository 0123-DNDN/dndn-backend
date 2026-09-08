package com.team0123.dndn.ai.type;

import java.util.Locale;

/**
 * DNDN에서 지원하는 금융 발화 의도입니다.
 * UNKNOWN은 실제 금융 기능이 아니라,
 * 사용자의 발화를 정확하게 분류하지 못했을 때 사용하는 안전한 실패 값입니다.
 */
public enum FinancialIntent {
    // 계좌 잔액 조회
    BALANCE_CHECK,
    // 송금 또는 계좌이체
    TRANSFER,
    // 입출금 및 거래내역 조회
    TRANSACTION_HISTORY,
    // 지원하지 않거나 분류할 수 없는 요청
    UNKNOWN;
    /**
     * Gemini가 반환한 문자열을 FinancialIntent enum으로 변환합니다.
     * Gemini가 허용되지 않은 문자열을 반환하거나 값이 비어 있으면
     * 예외를 외부로 던지지 않고 UNKNOWN으로 처리합니다.
     */
    public static FinancialIntent from(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }

        try {
            // 소문자나 앞뒤 공백이 포함되어도 정상적으로 변환합니다.
            return FinancialIntent.valueOf(
                    value.trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            // 등록되지 않은 Intent라면 안전하게 UNKNOWN으로 처리합니다.
            return UNKNOWN;
        }
    }
}
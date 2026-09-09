package com.team0123.dndn.fds.type;

/**
 * 최종 위험 등급을 상향시킨
 * Hard Rule 및 Combination Rule 식별자입니다.
 */
public enum FdsDecisionRule {

    /*
     * Hard Rule
     */

    // 신고·차단된 사기계좌 DB와 일치
    CONFIRMED_FRAUD_ACCOUNT,

    /*
     * Combination Rules
     */

    // 기관 사칭 + 안전계좌 + 제3자 송금 지시
    INSTITUTION_IMPERSONATION,

    // 기관 사칭 + 범죄·계좌 협박 + 긴급성
    AUTHORITY_THREAT,
    FAMILY_ACCIDENT_URGENT_SETTLEMENT,

    // 제3자 송금 지시 + 긴급성 + 비밀 유지 요구
    SECRET_TRANSFER,

    // 대출 선입금 요구 + 신규 수취인
    LOAN_SCAM,

    // 원격제어 요구 + 제3자 송금 지시
    REMOTE_CONTROL,

    // 신규 기기 + 신규 수취인 + 평소 대비 고액
    ACCOUNT_TAKEOVER,

    // 신규 수취인 + 잔액 70% 이상 + 평소 대비 고액
    RAPID_FUND_MOVEMENT,

    // 수취인 변경 + 금액 변경 + 송금 취소 반복
    MANIPULATION_CONFUSION,

    // 위험 Context 2개 이상 + 상태 변화 신호 2개 이상
    CONTEXT_AND_CONDITION,

    // 안전계좌 + 긴급성 + 신규 수취인
    SAFE_ACCOUNT_PATTERN,

    // 가족 사칭 + 신규 수취인 + 긴급성
    FAMILY_IMPERSONATION
}

package com.team0123.dndn.ai.type;

/**
 * 송금 목적 발화에서 탐지할 금융사기 위험 신호입니다.
 * 이 enum은 위험 신호의 종류만 나타냅니다.
 */
public enum ContextRiskSignal {

    // 검찰, 경찰, 금감원, 은행 직원 등 기관의 권위를 이용한 경우
    AUTHORITY_IMPERSONATION,

    // 안전계좌 또는 보호계좌로 돈을 옮기라고 요구한 경우
    SAFE_ACCOUNT_REQUEST,

    // 다른 사람이 특정 계좌로 송금하도록 지시한 경우
    THIRD_PARTY_INSTRUCTION,

    // 범죄 연루, 계좌 동결 등으로 위협한 경우
    CRIME_OR_ACCOUNT_THREAT,

    // 지금, 즉시, 오늘 안에 등 송금을 재촉한 경우
    URGENCY,

    // 가족이나 은행에 알리지 말라고 요구한 경우
    SECRECY_REQUEST,

    // 대출 실행 전에 돈을 먼저 보내라고 요구한 경우
    LOAN_UPFRONT_PAYMENT,

    // 원격제어 앱 설치 또는 화면 공유를 요구한 경우
    REMOTE_CONTROL_REQUEST,

    // 사용자가 송금 목적을 제대로 설명하지 못한 경우
    UNCLEAR_TRANSFER_PURPOSE,

    // 가족이나 지인을 사칭한 정황이 있는 경우
    FAMILY_IMPERSONATION
}
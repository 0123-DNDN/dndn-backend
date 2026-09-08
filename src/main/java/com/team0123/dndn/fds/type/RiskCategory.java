package com.team0123.dndn.fds.type;

/**
 * FDS 위험 점수 영역입니다.
 * 각 영역별 최대 점수를 독립적으로 적용한 뒤
 * 최종 점수를 합산합니다.
 */
public enum RiskCategory {

    // 송금액, 잔액 비율, 거래 시간
    TRANSACTION,

    // 신규 수취인, 장기 미거래, 위험 계좌
    RECIPIENT,

    // 단시간 반복 송금 및 누적 금액 증가
    VELOCITY,

    // 신규 기기 및 접속 환경 변화
    DEVICE,

    // 송금 과정에서 발생한 비정상적인 앱 조작
    BEHAVIOR,

    // 송금 목적 대화에서 탐지된 금융사기 맥락
    CONTEXT,

    // 음성 또는 채팅 상태 변화
    CONDITION
}
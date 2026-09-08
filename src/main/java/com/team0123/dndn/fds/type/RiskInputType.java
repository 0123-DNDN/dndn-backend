package com.team0123.dndn.fds.type;

/**
 * 사용자가 송금 목적을 입력한 방식입니다.
 * VOICE와 CHAT 전용 점수를 동시에 적용하지 않기 위해 사용합니다.
 */
public enum RiskInputType {

    // 음성 입력
    VOICE,

    // 채팅 입력
    CHAT
}
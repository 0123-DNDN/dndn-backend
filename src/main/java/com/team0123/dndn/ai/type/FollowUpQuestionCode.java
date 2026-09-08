package com.team0123.dndn.ai.type;

/**
 * 금융사기 위험 Context를 추가로 확인하기 위한 질문 코드입니다.
 * 프론트는 code를 기준으로 사용자의 예/아니오 답변을 전달하고,
 * B는 연결된 ContextRiskSignal을 최종 FDS 판단에 사용합니다.
 */
public enum FollowUpQuestionCode {

    // 긴급하게 송금하도록 압박받았는지 확인
    URGENCY(
            ContextRiskSignal.URGENCY,
            "상대방이 지금 바로 보내라고 했나요?"
    ),

    // 안전계좌 또는 보호계좌라고 안내받았는지 확인
    SAFE_ACCOUNT_REQUEST(
            ContextRiskSignal.SAFE_ACCOUNT_REQUEST,
            "안전계좌나 보호계좌로 보내라고 했나요?"
    ),

    // 가족이나 은행에 알리지 말라는 요구가 있었는지 확인
    SECRECY_REQUEST(
            ContextRiskSignal.SECRECY_REQUEST,
            "가족이나 은행에 알리지 말라고 했나요?"
    ),

    // 범죄 연루 또는 계좌 동결 협박이 있었는지 확인
    CRIME_OR_ACCOUNT_THREAT(
            ContextRiskSignal.CRIME_OR_ACCOUNT_THREAT,
            "범죄에 연루됐거나 계좌가 동결된다고 했나요?"
    ),

    // 제3자가 특정 계좌로 송금을 지시했는지 확인
    THIRD_PARTY_INSTRUCTION(
            ContextRiskSignal.THIRD_PARTY_INSTRUCTION,
            "다른 사람이 특정 계좌로 보내라고 했나요?"
    ),

    // 원격제어 앱 설치 또는 화면 공유 요구가 있었는지 확인
    REMOTE_CONTROL_REQUEST(
            ContextRiskSignal.REMOTE_CONTROL_REQUEST,
            "앱을 설치하거나 휴대전화 화면을 보여달라고 했나요?"
    );

    // 이 질문에 '예'라고 답했을 때 추가할 위험 신호
    private final ContextRiskSignal riskSignal;

    // 시니어 사용자에게 표시할 질문 문구
    private final String questionText;

    FollowUpQuestionCode(
            ContextRiskSignal riskSignal,
            String questionText
    ) {
        this.riskSignal = riskSignal;
        this.questionText = questionText;
    }

    /**
     * '예' 답변을 최종 Context 신호로 변환할 때 사용합니다.
     */
    public ContextRiskSignal riskSignal() {
        return riskSignal;
    }

    /**
     * 프론트에 표시할 사용자 친화 질문입니다.
     */
    public String questionText() {
        return questionText;
    }
}
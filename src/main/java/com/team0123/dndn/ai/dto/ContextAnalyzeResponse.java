package com.team0123.dndn.ai.dto;

import com.team0123.dndn.ai.type.ContextRiskSignal;

import java.util.List;

/**
 * 검증이 끝난 송금 목적 Context 분석 결과입니다.
 */
public record ContextAnalyzeResponse(

        // 유효한 위험 신호가 존재하는지 여부
        boolean suspicious,

        // 서버에서 검증된 위험 신호
        List<ContextRiskSignal> detectedSignals,

        // 사용자에게 바로 표시할 수 있는 위험 사유
        List<String> contextReasons,

        // Gemini 분석 자체가 성공했는지 여부
        boolean analysisSucceeded,

        // 사용자에게 추가 확인 질문이 필요한지 여부
        boolean requiresFollowUp,

        // 프론트에 표시할 추가 확인 질문 목록
        List<FollowUpQuestion> followUpQuestions

) {

    /**
     * 기존 코드와 테스트가 바로 깨지지 않도록 제공하는 생성자입니다.
     * 추가 질문 정보가 없는 기존 호출은
     * requiresFollowUp=false, followUpQuestions=[]로 처리합니다.
     */
    public ContextAnalyzeResponse(
            boolean suspicious,
            List<ContextRiskSignal> detectedSignals,
            List<String> contextReasons,
            boolean analysisSucceeded
    ) {
        this(
                suspicious,
                detectedSignals,
                contextReasons,
                analysisSucceeded,
                false,
                List.of()
        );
    }

    /**
     * 위험 표현이 없는 정상 분석 결과입니다.
     * 정상 송금에는 추가 질문을 제공하지 않습니다.
     */
    public static ContextAnalyzeResponse safe() {
        return new ContextAnalyzeResponse(
                false,
                List.of(),
                List.of(),
                true,
                false,
                List.of()
        );
    }

    /**
     * Gemini 호출 또는 응답 분석에 실패했을 때의 결과입니다.
     * 분석 실패를 정상 거래로 오해하지 않도록
     * analysisSucceeded를 false로 반환합니다.
     * 실패 시 사용자에게 어떤 재확인을 제공할지는
     * 이후 Service에서 별도로 처리할 수 있습니다.
     */
    public static ContextAnalyzeResponse failed() {
        return new ContextAnalyzeResponse(
                false,
                List.of(),
                List.of(),
                false,
                false,
                List.of()
        );
    }
}
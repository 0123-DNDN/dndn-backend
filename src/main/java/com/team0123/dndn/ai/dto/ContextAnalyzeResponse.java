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
        boolean analysisSucceeded
) {

    /**
     * 위험 표현이 없는 정상 분석 결과입니다.
     */
    public static ContextAnalyzeResponse safe() {
        return new ContextAnalyzeResponse(
                false,
                List.of(),
                List.of(),
                true
        );
    }

    /**
     * Gemini 호출 또는 응답 분석에 실패했을 때의 결과입니다.
     * 실패를 정상 거래로 오해하지 않도록
     * analysisSucceeded를 false로 반환합니다.
     */
    public static ContextAnalyzeResponse failed() {
        return new ContextAnalyzeResponse(
                false,
                List.of(),
                List.of(),
                false
        );
    }
}
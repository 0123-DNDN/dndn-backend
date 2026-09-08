package com.team0123.dndn.ai.dto;

import java.util.List;

/**
 * Gemini가 생성한 Context 분석 JSON을 담는 내부 DTO입니다.
 * detectedSignals는 아직 검증되지 않은 문자열이므로,
 * TransferContextService에서 ContextRiskSignal enum으로 변환합니다.
 */
public record GeminiContextResult(
        // 위험 맥락이 하나 이상 탐지됐는지 여부
        boolean suspicious,

        // Gemini가 탐지한 위험 신호 이름
        List<String> detectedSignals,

        // 사용자에게 보여줄 한국어 위험 사유
        List<String> contextReasons
) {
}
package com.team0123.dndn.ai.service;

import com.team0123.dndn.ai.client.GeminiContextClient;
import com.team0123.dndn.ai.dto.ContextAnalyzeResponse;
import com.team0123.dndn.ai.dto.GeminiContextResult;
import com.team0123.dndn.ai.type.ContextRiskSignal;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 송금 목적 Context 분석 결과를 검증하는 서비스입니다.
 * 담당 역할:
 * 1. Gemini Context 분석 요청
 * 2. Gemini가 반환한 위험 신호 검증
 * 3. 중복 신호와 중복 사유 제거
 * 4. 최종 suspicious 값 결정
 * 점수 계산과 위험 등급 결정은 이후 B-3/B-4에서 처리합니다.
 */
@Service
public class TransferContextService {

    private final GeminiContextClient geminiContextClient;

    public TransferContextService(
            GeminiContextClient geminiContextClient
    ) {
        this.geminiContextClient = geminiContextClient;
    }

    /**
     * 사용자가 설명한 송금 목적을 분석합니다.
     *
     * @param purposeText 사용자가 설명한 송금 목적
     * @return 검증이 끝난 Context 분석 결과
     */
    public ContextAnalyzeResponse analyze(String purposeText) {
        /*
         * 송금 목적이 없거나 공백뿐이라면 Gemini를 호출하지 않습니다.
         *
         * 목적을 설명하지 못한 상황 자체를
         * UNCLEAR_TRANSFER_PURPOSE 위험 신호로 처리합니다.
         */
        if (purposeText == null || purposeText.isBlank()) {
            return unclearPurpose();
        }

        /*
         * Gemini 호출에 성공하면 validateAndConvert()를 실행하고,
         * 호출 또는 파싱에 실패하면 analysisFailed=false 결과를 반환합니다.
         */
        return geminiContextClient.analyze(purposeText.trim())
                .map(this::validateAndConvert)
                .orElseGet(ContextAnalyzeResponse::failed);
    }

    /**
     * Gemini가 반환한 결과를 서버 규칙에 맞게 검증합니다.
     */
    private ContextAnalyzeResponse validateAndConvert(
            GeminiContextResult result
    ) {
        /*
         * Gemini가 반환한 문자열 신호를 enum으로 변환합니다.
         *
         * LinkedHashSet을 사용하는 이유:
         * - 중복 신호 제거
         * - Gemini가 반환한 순서 유지
         */
        Set<ContextRiskSignal> validatedSignals =
                validateSignals(result.detectedSignals());

        /*
         * 비어 있거나 중복된 위험 사유를 제거합니다.
         */
        Set<String> validatedReasons =
                validateReasons(result.contextReasons());

        /*
         * Gemini의 suspicious 값을 그대로 신뢰하지 않습니다.
         *
         * 서버가 검증한 위험 신호가 하나 이상 있을 때만
         * suspicious를 true로 결정합니다.
         */
        boolean suspicious =
                !validatedSignals.isEmpty();

        return new ContextAnalyzeResponse(
                suspicious,
                List.copyOf(validatedSignals),
                List.copyOf(validatedReasons),
                true
        );
    }

    /**
     * Gemini가 반환한 위험 신호 문자열을 검증합니다.
     * 허용되지 않은 문자열은 결과에서 제외합니다.
     */
    private Set<ContextRiskSignal> validateSignals(
            List<String> signals
    ) {
        Set<ContextRiskSignal> validatedSignals =
                new LinkedHashSet<>();

        // Gemini가 null을 반환했다면 빈 결과로 처리합니다.
        if (signals == null) {
            return validatedSignals;
        }

        for (String signal : signals) {
            // null 또는 공백인 값은 무시합니다.
            if (signal == null || signal.isBlank()) {
                continue;
            }

            try {
                /*
                 * 소문자 또는 앞뒤 공백이 포함되어도
                 * 정상적인 enum 이름이라면 변환합니다.
                 */
                ContextRiskSignal contextRiskSignal =
                        ContextRiskSignal.valueOf(
                                signal.trim()
                                        .toUpperCase(Locale.ROOT)
                        );

                // Set이므로 같은 신호가 여러 번 와도 한 번만 저장됩니다.
                validatedSignals.add(contextRiskSignal);

            } catch (IllegalArgumentException exception) {
                /*
                 * 정의하지 않은 위험 신호는 응답에서 제외합니다.
                 *
                 * 예:
                 * Gemini가 "UNKNOWN_SIGNAL"을 반환한 경우
                 */
            }
        }

        return validatedSignals;
    }

    /**
     * Gemini가 생성한 사용자 친화적 위험 사유를 검증합니다.
     */
    private Set<String> validateReasons(
            List<String> reasons
    ) {
        Set<String> validatedReasons =
                new LinkedHashSet<>();

        // Gemini가 null을 반환했다면 빈 결과로 처리합니다.
        if (reasons == null) {
            return validatedReasons;
        }

        for (String reason : reasons) {
            // null 또는 공백뿐인 설명은 제외합니다.
            if (reason == null || reason.isBlank()) {
                continue;
            }

            // 앞뒤 공백을 제거하고 중복 설명을 제거합니다.
            validatedReasons.add(reason.trim());
        }

        return validatedReasons;
    }

    /**
     * 사용자가 송금 목적을 입력하지 않은 경우의 결과입니다.
     * 외부 AI 호출 없이 서버 규칙만으로 판단합니다.
     */
    private ContextAnalyzeResponse unclearPurpose() {
        return new ContextAnalyzeResponse(
                true,
                List.of(
                        ContextRiskSignal.UNCLEAR_TRANSFER_PURPOSE
                ),
                List.of(
                        "송금 목적이 명확하지 않아요."
                ),
                true
        );
    }
}
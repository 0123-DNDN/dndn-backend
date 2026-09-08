package com.team0123.dndn.ai.client;

import com.team0123.dndn.ai.config.GeminiProperties;
import com.team0123.dndn.ai.dto.GeminiContextResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 송금 목적 문장을 Gemini에 전달하여
 * 금융사기 위험 Context를 분석하는 Client입니다.
 * 담당 역할:
 * 1. Gemini Context 분석 API 요청 생성
 * 2. Gemini API 호출
 * 3. Gemini JSON 응답 파싱
 * 이 클래스에서는 점수 계산, 위험 등급 결정,
 * 송금 상태 변경을 수행하지 않습니다.
 */
@Component
public class GeminiContextClient {

    private static final Logger log =
            LoggerFactory.getLogger(GeminiContextClient.class);

    /**
     * Gemini가 송금 목적을 분석할 때 따를 규칙입니다.
     * 단순히 특정 단어가 포함됐는지가 아니라
     * 문장 전체의 의미와 맥락을 판단하도록 지시합니다.
     */
    private static final String SYSTEM_INSTRUCTION = """
            당신은 시니어 금융사기 예방을 위한 송금 목적 분석기입니다.

            사용자가 설명한 송금 목적을 읽고,
            금융사기 위험 맥락이 있는지 분석하세요.

            반환할 수 있는 위험 신호는 다음 열 가지뿐입니다.

            - AUTHORITY_IMPERSONATION
              검찰, 경찰, 금융감독원, 은행 직원 등 기관이나 권위를 사칭한 경우

            - SAFE_ACCOUNT_REQUEST
              자산 보호를 이유로 안전계좌 또는 보호계좌로 송금을 요구한 경우

            - THIRD_PARTY_INSTRUCTION
              다른 사람이 사용자에게 특정 계좌로 송금하도록 지시한 경우

            - CRIME_OR_ACCOUNT_THREAT
              범죄 연루, 계좌 동결, 수사 대상 등의 표현으로 사용자를 위협한 경우

            - URGENCY
              지금, 즉시, 오늘 안에 등 빠른 송금을 재촉하거나 시간 압박을 가한 경우

            - SECRECY_REQUEST
              가족, 은행, 경찰 등 다른 사람에게 알리지 말라고 요구한 경우

            - LOAN_UPFRONT_PAYMENT
              대출 실행 전에 수수료, 보증금, 기존 대출 상환 등의 명목으로
              돈을 먼저 보내라고 요구한 경우

            - REMOTE_CONTROL_REQUEST
              원격제어 앱 설치, 화면 공유, 휴대전화 조작 권한 제공 등을 요구한 경우

            - UNCLEAR_TRANSFER_PURPOSE
              사용자가 송금 이유를 설명하지 못하거나
              단순히 다른 사람이 시켜서 보낸다고만 답한 경우

            - FAMILY_IMPERSONATION
              가족이나 지인을 사칭하여 돈을 요구한 정황이 있는 경우

            분석 규칙:

            1. 반드시 문장 전체의 의미와 맥락을 분석하세요.
            2. 특정 단어가 들어갔다는 이유만으로 위험하다고 판단하지 마세요.
            3. 실제로 확인되는 위험 신호만 반환하세요.
            4. 위험 신호를 추측하거나 새로 만들지 마세요.
            5. 같은 위험 신호를 중복해서 반환하지 마세요.
            6. contextReasons는 시니어가 이해하기 쉬운 한국어로 작성하세요.
            7. 규칙 코드나 영어 이름을 contextReasons에 그대로 노출하지 마세요.
            8. 위험 신호가 없다면 suspicious는 false이고 두 배열은 비어 있어야 합니다.
            9. 위험 신호가 하나 이상 있다면 suspicious는 true여야 합니다.
            10. 점수나 최종 위험 등급은 계산하지 마세요.

            정상 예시:

            입력:
            아들에게 생활비를 보내요.

            출력:
            {
              "suspicious": false,
              "detectedSignals": [],
              "contextReasons": []
            }

            위험 예시:

            입력:
            검찰에서 지금 안전계좌로 보내라고 했어요.

            출력:
            {
              "suspicious": true,
              "detectedSignals": [
                "AUTHORITY_IMPERSONATION",
                "SAFE_ACCOUNT_REQUEST",
                "THIRD_PARTY_INSTRUCTION",
                "URGENCY"
              ],
              "contextReasons": [
                "검찰·경찰 등 기관의 권위를 이용한 표현이 확인됐어요.",
                "자산 보호를 이유로 안전계좌 송금을 요구받았어요.",
                "다른 사람이 특정 계좌로 송금하도록 지시했어요.",
                "즉시 송금하도록 재촉받고 있어요."
              ]
            }

            오탐 방지 예시:

            입력:
            검찰청 근처 식당에서 밥값을 보냈어요.

            출력:
            {
              "suspicious": false,
              "detectedSignals": [],
              "contextReasons": []
            }
            """;

    private final GeminiProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    /**
     * Gemini 설정과 JSON 변환기를 주입받습니다.
     */
    public GeminiContextClient(
            GeminiProperties properties,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;

        // 기존 Intent Client와 같은 Gemini 기본 URL을 사용합니다.
        this.restClient = RestClient.create(
                properties.getBaseUrl()
        );

        this.objectMapper = objectMapper;
    }

    /**
     * 사용자가 설명한 송금 목적을 Gemini로 분석합니다.

     * 분석 성공:
     * Optional 안에 GeminiContextResult 반환

     * 분석 실패:
     * Optional.empty() 반환
     */
    public Optional<GeminiContextResult> analyze(
            String purposeText
    ) {
        // API Key가 없다면 Gemini 요청을 보내지 않습니다.
        if (properties.getApiKey() == null
                || properties.getApiKey().isBlank()) {

            log.error("GEMINI_API_KEY가 설정되지 않았습니다.");
            return Optional.empty();
        }

        try {
            // Gemini generateContent REST API를 호출합니다.
            GeminiApiResponse response = restClient.post()
                    .uri(
                            "/v1beta/models/{model}:generateContent",
                            properties.getModel()
                    )

                    // API Key를 요청 헤더에 전달합니다.
                    .header(
                            "x-goog-api-key",
                            properties.getApiKey()
                    )

                    // 시스템 지시문, 송금 목적, JSON Schema를 전달합니다.
                    .body(createRequestBody(purposeText))

                    // 요청을 전송하고 응답을 받습니다.
                    .retrieve()

                    // Gemini 전체 응답을 내부 record로 변환합니다.
                    .body(GeminiApiResponse.class);

            // Gemini 응답에서 실제 JSON 문자열을 꺼냅니다.
            String resultText = extractText(response);

            if (resultText == null || resultText.isBlank()) {
                log.error("Gemini Context 응답에 분석 결과가 없습니다.");
                return Optional.empty();
            }

            // JSON 문자열을 GeminiContextResult로 변환합니다.
            GeminiContextResult result =
                    objectMapper.readValue(
                            resultText,
                            GeminiContextResult.class
                    );

            return Optional.of(result);

        } catch (Exception exception) {
            /*
             * API 호출 실패, 503 혼잡, JSON 파싱 실패 등을 처리합니다.
             *
             * 사용자 발화와 API Key는 로그에 남기지 않습니다.
             */
            log.error(
                    "Gemini Context 분석에 실패했습니다: {}",
                    exception.getMessage()
            );

            return Optional.empty();
        }
    }

    /**
     * Gemini API에 전달할 요청 본문을 생성합니다.
     */
    private Map<String, Object> createRequestBody(
            String purposeText
    ) {
        /*
         * Gemini가 반환할 수 있는 위험 신호를 제한합니다.
         */
        List<String> allowedSignals = List.of(
                "AUTHORITY_IMPERSONATION",
                "SAFE_ACCOUNT_REQUEST",
                "THIRD_PARTY_INSTRUCTION",
                "CRIME_OR_ACCOUNT_THREAT",
                "URGENCY",
                "SECRECY_REQUEST",
                "LOAN_UPFRONT_PAYMENT",
                "REMOTE_CONTROL_REQUEST",
                "UNCLEAR_TRANSFER_PURPOSE",
                "FAMILY_IMPERSONATION"
        );

        /*
         * Gemini의 응답 JSON 구조를 정의합니다.
         */
        Map<String, Object> responseSchema = Map.of(
                "type", "OBJECT",

                "properties", Map.of(
                        "suspicious", Map.of(
                                "type", "BOOLEAN"
                        ),

                        "detectedSignals", Map.of(
                                "type", "ARRAY",

                                // 배열 안에는 허용된 위험 신호만 들어갈 수 있습니다.
                                "items", Map.of(
                                        "type", "STRING",
                                        "enum", allowedSignals
                                )
                        ),

                        "contextReasons", Map.of(
                                "type", "ARRAY",

                                // 위험 사유는 사용자 친화적인 문자열 배열입니다.
                                "items", Map.of(
                                        "type", "STRING"
                                )
                        )
                ),

                // 세 필드가 항상 JSON 응답에 포함되도록 합니다.
                "required", List.of(
                        "suspicious",
                        "detectedSignals",
                        "contextReasons"
                )
        );

        return Map.of(
                // Context 분석 전용 시스템 지시문
                "systemInstruction", Map.of(
                        "parts", List.of(
                                Map.of(
                                        "text",
                                        SYSTEM_INSTRUCTION
                                )
                        )
                ),

                // 사용자가 실제로 설명한 송금 목적
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(
                                        Map.of(
                                                "text",
                                                purposeText
                                        )
                                )
                        )
                ),

                // 응답의 무작위성을 낮추고 JSON 형식을 강제합니다.
                "generationConfig", Map.of(
                        "temperature", 0,
                        "responseMimeType", "application/json",
                        "responseSchema", responseSchema
                )
        );
    }

    /**
     * Gemini 전체 응답에서 실제 생성된 JSON 문자열을 추출합니다.
     * 응답 경로:
     * candidates[0].content.parts[0].text
     */
    private String extractText(GeminiApiResponse response) {
        // Gemini가 응답 후보를 반환하지 않은 경우
        if (response == null
                || response.candidates() == null
                || response.candidates().isEmpty()) {
            return null;
        }

        GeminiCandidate candidate =
                response.candidates().get(0);

        // 첫 번째 응답 후보에 실제 내용이 없는 경우
        if (candidate.content() == null
                || candidate.content().parts() == null
                || candidate.content().parts().isEmpty()) {
            return null;
        }

        // Gemini가 생성한 JSON 문자열을 반환합니다.
        return candidate.content().parts().get(0).text();
    }

    /*
     * 아래 record들은 Gemini API의 응답 구조를 나타냅니다.
     * GeminiContextClient 내부에서만 사용합니다.
     */

    private record GeminiApiResponse(
            List<GeminiCandidate> candidates
    ) {
    }

    private record GeminiCandidate(
            GeminiContent content
    ) {
    }

    private record GeminiContent(
            List<GeminiPart> parts
    ) {
    }

    private record GeminiPart(
            String text
    ) {
    }
}
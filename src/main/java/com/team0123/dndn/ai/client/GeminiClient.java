package com.team0123.dndn.ai.client;

import com.team0123.dndn.ai.config.GeminiProperties;
import com.team0123.dndn.ai.dto.GeminiIntentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Gemini REST API와 직접 통신하는 Client입니다.
 * 담당 역할:
 * 1. Gemini에 사용자 발화 전달
 * 2. Gemini 응답에서 JSON 문자열 추출
 * 3. JSON을 GeminiIntentResult로 변환
 * 이 클래스에서는 DB 조회나 송금 처리를 수행하지 않습니다.
 */
@Component
public class GeminiClient {
    private static final Logger log =
            LoggerFactory.getLogger(GeminiClient.class);
    /**
     * Gemini가 금융 발화를 일정한 기준으로 분류하도록 전달하는 지시문입니다.
     * 사용자의 발화와 지시문을 분리하여,
     * 사용자 문장이 시스템 규칙으로 오해되는 가능성을 줄입니다.
     */
    private static final String SYSTEM_INSTRUCTION = """
            당신은 한국어 금융 발화를 분석하는 분류기입니다.
            지원하는 intent는 다음 네 가지뿐입니다.
            - BALANCE_CHECK: 잔액 또는 잔고 조회
            - TRANSFER: 송금 또는 이체 요청
            - TRANSACTION_HISTORY: 거래내역 또는 소비내역 조회
            - UNKNOWN: 위 세 가지에 해당하지 않거나 의미를 확정할 수 없는 요청
            분석 규칙:
            1. 발화에 명시된 정보만 추출하세요.
            2. 수취인 이름이나 별칭을 추측하지 마세요.
            3. 송금 금액은 원 단위 정수로 변환하세요.
            4. 수취인이 없으면 recipientKeyword를 null로 반환하세요.
            5. 금액이 없으면 amount를 null로 반환하세요.
            6. TRANSFER가 아니면 recipientKeyword와 amount는 null로 반환하세요.
            7. 계좌번호나 예금주 정보를 임의로 생성하지 마세요.
            예시:
            입력: 엄마에게 20만 원 보내줘
            출력: {"intent":"TRANSFER","recipientKeyword":"엄마","amount":200000}
            입력: 이번 달 거래내역 보여줘
            출력: {"intent":"TRANSACTION_HISTORY","recipientKeyword":null,"amount":null}
            """;

    private final GeminiProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    /**
     * Gemini 설정과 JSON 변환기를 주입받습니다.
     * 현재 프로젝트에는 RestClient.Builder Bean이 자동 등록되지 않으므로
     * RestClient.create()를 이용해 HTTP 클라이언트를 직접 생성합니다.
     */
    public GeminiClient(
            GeminiProperties properties,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;

        // Gemini REST API 기본 주소를 사용하는 클라이언트를 직접 생성합니다.
        this.restClient = RestClient.create(
                properties.getBaseUrl()
        );

        this.objectMapper = objectMapper;
    }
    /**
     * 사용자의 금융 발화를 Gemini로 분석합니다.
     * 성공: 분석 결과가 들어 있는 Optional 반환
     * 실패: Optional.empty() 반환
     * Optional을 사용함으로써 Gemini 장애가 발생해도
     * 서비스가 안전하게 UNKNOWN 응답을 만들 수 있습니다.
     */
    public Optional<GeminiIntentResult> analyze(String text) {
        // API Key가 없다면 외부 요청을 보내지 않습니다.
        if (properties.getApiKey() == null
                || properties.getApiKey().isBlank()) {
            log.error("GEMINI_API_KEY가 설정되지 않았습니다.");
            return Optional.empty();
        }

        try {
            // Gemini generateContent API를 호출합니다.
            GeminiApiResponse response = restClient.post()
                    .uri(
                            "/v1beta/models/{model}:generateContent",
                            properties.getModel()
                    )
                    // API Key는 URL이 아닌 헤더로 전달합니다.
                    .header(
                            "x-goog-api-key",
                            properties.getApiKey()
                    )
                    // 시스템 지시문, 사용자 문장, JSON Schema를 요청 본문에 담습니다.
                    .body(createRequestBody(text))

                    // 요청을 전송하고 응답을 받습니다.
                    .retrieve()

                    // Gemini의 전체 응답 JSON을 내부 record로 변환합니다.
                    .body(GeminiApiResponse.class);
            // candidates[0].content.parts[0].text를 추출합니다.
            String resultText = extractText(response);
            if (resultText == null || resultText.isBlank()) {
                log.error("Gemini 응답에 분석 결과가 없습니다.");
                return Optional.empty();
            }
            // Gemini가 생성한 JSON 문자열을 내부 DTO로 변환합니다.
            GeminiIntentResult result =
                    objectMapper.readValue(
                            resultText,
                            GeminiIntentResult.class
                    );

            return Optional.of(result);

        } catch (Exception exception) {
            /*
             * API 오류, 타임아웃, JSON 파싱 오류 등을 안전하게 처리합니다.
             *
             * 개인정보 보호를 위해 사용자 발화 원문과 API Key는
             * 로그에 기록하지 않습니다.
             */
            log.error(
                    "Gemini Intent 분석에 실패했습니다: {}",
                    exception.getMessage()
            );

            return Optional.empty();
        }
    }

    /**
     * Gemini API에 전달할 요청 본문을 만듭니다.
     */
    private Map<String, Object> createRequestBody(String text) {
        /*
         * Gemini가 반드시 이 구조의 JSON을 반환하도록 지정합니다.
         *
         * 예상 결과:
         * {
         *   "intent": "TRANSFER",
         *   "recipientKeyword": "엄마",
         *   "amount": 200000
         * }
         */
        Map<String, Object> responseSchema = Map.of(
                "type", "OBJECT",

                // 응답에 포함할 필드와 타입을 정의합니다.
                "properties", Map.of(
                        "intent", Map.of(
                                "type", "STRING",

                                // Gemini가 아래 네 값 중 하나만 반환하도록 제한합니다.
                                "enum", List.of(
                                        "BALANCE_CHECK",
                                        "TRANSFER",
                                        "TRANSACTION_HISTORY",
                                        "UNKNOWN"
                                )
                        ),

                        "recipientKeyword", Map.of(
                                "type", "STRING",

                                // 수취인을 찾지 못하면 null을 허용합니다.
                                "nullable", true
                        ),

                        "amount", Map.of(
                                "type", "INTEGER",

                                // Java Long과 대응할 수 있도록 int64로 지정합니다.
                                "format", "int64",

                                // 금액을 찾지 못하면 null을 허용합니다.
                                "nullable", true
                        )
                ),

                // 세 필드는 항상 JSON에 포함하되, 값 자체는 null일 수 있습니다.
                "required", List.of(
                        "intent",
                        "recipientKeyword",
                        "amount"
                )
        );

        return Map.of(
                // Gemini가 따라야 할 고정 분석 규칙입니다.
                "systemInstruction", Map.of(
                        "parts", List.of(
                                Map.of("text", SYSTEM_INSTRUCTION)
                        )
                ),

                // 실제 사용자가 입력한 금융 발화입니다.
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(
                                        Map.of("text", text)
                                )
                        )
                ),

                // 응답의 무작위성을 줄이고 JSON 형식을 강제합니다.
                "generationConfig", Map.of(
                        "temperature", 0,
                        "responseMimeType", "application/json",
                        "responseSchema", responseSchema
                )
        );
    }

    /**
     * Gemini 전체 응답에서 실제 생성된 텍스트를 꺼냅니다.
     * Gemini 응답 경로:
     * candidates[0].content.parts[0].text
     */
    private String extractText(GeminiApiResponse response) {
        // 응답 또는 후보 목록이 없으면 분석 실패로 처리합니다.
        if (response == null
                || response.candidates() == null
                || response.candidates().isEmpty()) {
            return null;
        }

        Candidate candidate = response.candidates().get(0);

        // 첫 번째 후보 안에 content 또는 parts가 없는지 검사합니다.
        if (candidate.content() == null
                || candidate.content().parts() == null
                || candidate.content().parts().isEmpty()) {
            return null;
        }

        // Gemini가 생성한 JSON 문자열을 반환합니다.
        return candidate.content().parts().get(0).text();
    }

    /*
     * 아래 record들은 Gemini API의 응답 구조를 표현하는 내부 DTO입니다.
     *
     * 외부에서 사용할 필요가 없으므로 private으로 선언합니다.
     */

    private record GeminiApiResponse(
            List<Candidate> candidates
    ) {
    }

    private record Candidate(
            Content content
    ) {
    }

    private record Content(
            List<Part> parts
    ) {
    }

    private record Part(
            String text
    ) {
    }
}
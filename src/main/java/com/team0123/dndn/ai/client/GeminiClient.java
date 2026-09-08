package com.team0123.dndn.ai.client;

import com.team0123.dndn.ai.config.GeminiProperties;
import com.team0123.dndn.ai.dto.GeminiIntentResult;
import com.team0123.dndn.ai.type.FinancialIntent;
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
 * 1. Gemini에 사용자 금융 발화 전달
 * 2. 사용자가 진입한 금융 기능 힌트 전달
 * 3. Gemini 응답에서 JSON 문자열 추출
 * 4. JSON을 GeminiIntentResult로 변환
 * DB 조회나 실제 송금 처리는 수행하지 않습니다.
 */
@Component
public class GeminiClient {

    private static final Logger log =
            LoggerFactory.getLogger(GeminiClient.class);

    /**
     * Gemini가 금융 발화를 일정한 기준으로 분류하도록
     * 전달하는 기본 지시문입니다.
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
     * 현재 프로젝트에는 RestClient.Builder Bean이
     * 자동 등록되지 않으므로 RestClient.create()로 생성합니다.
     */
    public GeminiClient(
            GeminiProperties properties,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;

        this.restClient = RestClient.create(
                properties.getBaseUrl()
        );

        this.objectMapper = objectMapper;
    }

    /**
     * intentHint가 없는 일반 AI 비서 요청입니다.
     * 기존 코드와 테스트가 계속 동작할 수 있도록
     * 기존 메서드를 유지합니다.
     */
    public Optional<GeminiIntentResult> analyze(
            String text
    ) {
        return analyze(text, null);
    }

    /**
     * 사용자의 금융 발화를 Gemini로 분석합니다.
     * intentHint가 TRANSFER이면 사용자가 이미
     * 돈 보내기 화면에 진입한 것으로 판단합니다.
     * 이 경우 송금 여부를 다시 분류하기보다
     * 수취인과 금액 추출에 집중하도록 Gemini에 지시합니다.
     */
    public Optional<GeminiIntentResult> analyze(
            String text,
            FinancialIntent intentHint
    ) {
        /*
         * API Key가 없다면 외부 요청을 보내지 않고
         * 분석 실패 결과를 반환합니다.
         */
        if (properties.getApiKey() == null
                || properties.getApiKey().isBlank()) {
            log.error(
                    "GEMINI_API_KEY가 설정되지 않았습니다."
            );

            return Optional.empty();
        }

        try {
            /*
             * Gemini generateContent API를 호출합니다.
             */
            GeminiApiResponse response =
                    restClient.post()
                            .uri(
                                    "/v1beta/models/{model}:generateContent",
                                    properties.getModel()
                            )

                            /*
                             * API Key는 URL이 아닌
                             * HTTP Header로 전달합니다.
                             */
                            .header(
                                    "x-goog-api-key",
                                    properties.getApiKey()
                            )

                            /*
                             * 시스템 지시문, 사용자 발화,
                             * 응답 JSON Schema를 전달합니다.
                             */
                            .body(
                                    createRequestBody(
                                            text,
                                            intentHint
                                    )
                            )

                            // 요청을 전송합니다.
                            .retrieve()

                            /*
                             * Gemini 전체 응답을
                             * 내부 record로 변환합니다.
                             */
                            .body(GeminiApiResponse.class);

            /*
             * candidates[0].content.parts[0].text에서
             * 실제 생성된 JSON 문자열을 꺼냅니다.
             */
            String resultText =
                    extractText(response);

            if (resultText == null
                    || resultText.isBlank()) {
                log.error(
                        "Gemini 응답에 분석 결과가 없습니다."
                );

                return Optional.empty();
            }

            /*
             * Gemini가 생성한 JSON 문자열을
             * 내부 DTO로 변환합니다.
             */
            GeminiIntentResult result =
                    objectMapper.readValue(
                            resultText,
                            GeminiIntentResult.class
                    );

            return Optional.of(result);

        } catch (Exception exception) {
            /*
             * API 오류, 타임아웃, JSON 파싱 오류 등을
             * 안전하게 처리합니다.
             *
             * 개인정보 보호를 위해 사용자 발화 원문과
             * API Key는 로그에 남기지 않습니다.
             */
            log.error(
                    "Gemini Intent 분석에 실패했습니다: {}",
                    exception.getMessage()
            );

            return Optional.empty();
        }
    }

    /**
     * 사용자가 진입한 금융 기능에 따라
     * Gemini에 전달할 시스템 지시문을 만듭니다.
     */
    private String createSystemInstruction(
            FinancialIntent intentHint
    ) {
        /*
         * 일반 AI 비서 요청에서는 사용자가 원하는 기능을
         * 알 수 없으므로 기존처럼 전체 Intent를 분류합니다.
         */
        if (intentHint == null
                || intentHint == FinancialIntent.UNKNOWN) {
            return SYSTEM_INSTRUCTION;
        }

        /*
         * 홈의 돈 보내기 버튼으로 진입한 경우입니다.
         *
         * 사용자가 이미 송금 기능을 선택했기 때문에
         * "보내줘", "이체해줘"라는 표현이 없어도
         * TRANSFER로 반환하도록 지시합니다.
         */
        if (intentHint == FinancialIntent.TRANSFER) {
            return SYSTEM_INSTRUCTION + """

                    추가 분석 조건:

                    현재 사용자는 이미 '돈 보내기' 화면에 진입했습니다.
                    따라서 사용자의 발화를 반드시 TRANSFER로 분류하세요.

                    사용자가 '보내줘' 또는 '이체해줘'라고
                    직접 말하지 않아도 됩니다.

                    발화에서 수취인과 금액만 추출하세요.

                    수취인이 없으면 recipientKeyword는 null로 반환하세요.
                    금액이 없으면 amount는 null로 반환하세요.

                    예시:

                    입력: 엄마한테 20만 원
                    출력: {"intent":"TRANSFER","recipientKeyword":"엄마","amount":200000}

                    입력: 엄마한테
                    출력: {"intent":"TRANSFER","recipientKeyword":"엄마","amount":null}

                    입력: 20만 원
                    출력: {"intent":"TRANSFER","recipientKeyword":null,"amount":200000}
                    """;
        }

        /*
         * 현재 MVP에서 화면 진입 힌트로 사용하는 값은
         * TRANSFER뿐입니다.
         *
         * 다른 값이 전달되면 기존 분석 방식을 사용합니다.
         */
        return SYSTEM_INSTRUCTION;
    }

    /**
     * Gemini API에 전달할 요청 본문을 만듭니다.
     */
    private Map<String, Object> createRequestBody(
            String text,
            FinancialIntent intentHint
    ) {
        /*
         * Gemini가 반드시 아래 구조의 JSON을
         * 반환하도록 Response Schema를 지정합니다.
         *
         * {
         *   "intent": "TRANSFER",
         *   "recipientKeyword": "엄마",
         *   "amount": 200000
         * }
         */
        Map<String, Object> responseSchema =
                Map.of(
                        "type", "OBJECT",

                        /*
                         * 응답에 포함할 필드와
                         * 각 필드의 타입을 정의합니다.
                         */
                        "properties", Map.of(
                                "intent", Map.of(
                                        "type", "STRING",

                                        /*
                                         * Gemini가 아래 네 값 중
                                         * 하나만 반환하도록 제한합니다.
                                         */
                                        "enum", List.of(
                                                "BALANCE_CHECK",
                                                "TRANSFER",
                                                "TRANSACTION_HISTORY",
                                                "UNKNOWN"
                                        )
                                ),

                                "recipientKeyword", Map.of(
                                        "type", "STRING",

                                        /*
                                         * 수취인을 찾지 못하면
                                         * null을 허용합니다.
                                         */
                                        "nullable", true
                                ),

                                "amount", Map.of(
                                        "type", "INTEGER",

                                        /*
                                         * Java Long과 대응할 수 있도록
                                         * int64로 지정합니다.
                                         */
                                        "format", "int64",

                                        /*
                                         * 금액을 찾지 못하면
                                         * null을 허용합니다.
                                         */
                                        "nullable", true
                                )
                        ),

                        /*
                         * 세 필드는 항상 JSON에 포함합니다.
                         *
                         * recipientKeyword와 amount는
                         * 필드가 존재하면서 값은 null일 수 있습니다.
                         */
                        "required", List.of(
                                "intent",
                                "recipientKeyword",
                                "amount"
                        )
                );

        return Map.of(
                /*
                 * Gemini가 따라야 할 분석 규칙입니다.
                 *
                 * intentHint에 따라 기본 분류 지시문 또는
                 * 송금 화면 전용 지시문이 사용됩니다.
                 */
                "systemInstruction", Map.of(
                        "parts", List.of(
                                Map.of(
                                        "text",
                                        createSystemInstruction(
                                                intentHint
                                        )
                                )
                        )
                ),

                // 실제 사용자가 입력한 금융 발화입니다.
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(
                                        Map.of(
                                                "text",
                                                text
                                        )
                                )
                        )
                ),

                /*
                 * 응답의 무작위성을 줄이고
                 * JSON 형식을 강제합니다.
                 */
                "generationConfig", Map.of(
                        "temperature", 0,
                        "responseMimeType",
                        "application/json",
                        "responseSchema",
                        responseSchema
                )
        );
    }

    /**
     * Gemini 전체 응답에서 실제 생성된 텍스트를 꺼냅니다.
     * 응답 경로:
     * candidates[0].content.parts[0].text
     */
    private String extractText(
            GeminiApiResponse response
    ) {
        /*
         * 응답 또는 후보 목록이 없으면
         * 분석 실패로 처리합니다.
         */
        if (response == null
                || response.candidates() == null
                || response.candidates().isEmpty()) {
            return null;
        }

        Candidate candidate =
                response.candidates().get(0);

        /*
         * 첫 번째 후보에 content 또는 parts가
         * 존재하지 않으면 분석 실패로 처리합니다.
         */
        if (candidate.content() == null
                || candidate.content().parts() == null
                || candidate.content().parts().isEmpty()) {
            return null;
        }

        return candidate.content()
                .parts()
                .get(0)
                .text();
    }

    /*
     * 아래 record들은 Gemini API 응답 구조를 표현합니다.
     *
     * 외부에서 사용할 필요가 없으므로
     * private으로 선언합니다.
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
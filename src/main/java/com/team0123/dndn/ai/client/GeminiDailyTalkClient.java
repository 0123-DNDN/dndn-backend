package com.team0123.dndn.ai.client;

import com.team0123.dndn.ai.config.GeminiProperties;
import com.team0123.dndn.interaction.entity.InteractionMessage;
import com.team0123.dndn.interaction.entity.SenderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class GeminiDailyTalkClient {

    private static final Logger log =
            LoggerFactory.getLogger(GeminiDailyTalkClient.class);

    private static final String SYSTEM_INSTRUCTION = """
            당신은 시니어와 '오늘 이야기 나누기' 대화를 진행하는 한국어 대화 상대입니다.

            다음 규칙을 반드시 지키세요.
            - 항상 존댓말을 사용하세요.
            - 사용자의 마지막 답변을 바탕으로 자연스러운 후속 질문을 하나만 하세요.
            - 질문 앞에는 사용자의 답변 맥락에 맞는 따뜻한 공감을 2~4어절의 짧은 구절 하나로만 덧붙이세요.
            - 공감보다 후속 질문에 문장 길이를 사용하세요.
            - "그러셨군요." 같은 표현만 기계적으로 반복하지 말고 자연스럽고 다양하게 표현하세요.
            - 공감할 때 사용자의 답변을 길게 반복하거나 요약하지 마세요.
            - 최근 경험과 일상 이야기를 중심으로 대화를 확장하세요.
            - 과거 회상 질문도 가능하지만 과거 이야기만 계속 파고들지 마세요.
            - 사용자가 기억하지 못한다고 하면 압박하거나 같은 내용을 추궁하지 마세요.
            - 정답을 요구하거나 기억력 시험처럼 느껴지는 질문을 하지 마세요.
            - 진단, 치료, 치매 여부 판단 또는 의료적 조언을 하지 마세요.
            - 사용자가 말하지 않은 사실을 만들어내지 마세요.
            - 한 번에 질문은 반드시 하나만 하세요.
            - 답변은 짧고 이해하기 쉬운 한두 문장으로 작성하세요.
            """;

    private static final String CLOSING_SYSTEM_INSTRUCTION = """
            당신은 시니어와 진행한 '오늘 이야기 나누기' 대화를 마무리하는 한국어 대화 상대입니다.

            지금까지의 전체 대화를 바탕으로, 대화 상대가 방금 들은 이야기를 자연스럽게 되짚으며 인사하는 마무리 메시지를 작성하세요.
            - 별도의 요약 보고서나 발표문처럼 쓰지 말고, 대화를 이어 말하듯 작성하세요.
            - "오늘 나누었던 이야기를 정리해 보니", "어르신께서는", "무엇보다"처럼 형식적이거나 거리감 있는 표현으로 시작하거나 나열하지 마세요.
            - 사용자가 실제로 말한 내용 중 중심이 되는 이야기만 자연스럽게 연결하고, 세부 내용을 빠짐없이 열거하지 마세요.
            - 사용자가 말하지 않은 새로운 사실을 만들지 마세요.
            - 사용자의 답변이 짧으면 내용을 억지로 부풀리지 마세요.
            - 자연스러운 한국어 존댓말로 2~4문장만 작성하세요.
            - 마지막에는 이야기를 들려주셔서 고맙다는 짧고 따뜻한 표현을 포함하세요.
            - 질문을 추가하거나 다음 행동을 요구하지 마세요.
            - 기억력, 인지 상태, 치매 여부 또는 건강 상태를 평가하거나 진단하지 마세요.
            - 치료나 의료적 조언을 하지 마세요.
            """;

    private final GeminiProperties properties;
    private final RestClient restClient;

    public GeminiDailyTalkClient(GeminiProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create(properties.getBaseUrl());
    }

    public Optional<String> generateNextQuestion(
            List<InteractionMessage> messages
    ) {
        return generate(
                messages,
                SYSTEM_INSTRUCTION,
                120,
                "Gemini 오늘 이야기 후속 질문"
        );
    }

    public Optional<String> generateClosingSummary(
            List<InteractionMessage> messages
    ) {
        return generate(
                messages,
                CLOSING_SYSTEM_INSTRUCTION,
                220,
                "Gemini 오늘 이야기 요약"
        );
    }

    private Optional<String> generate(
            List<InteractionMessage> messages,
            String systemInstruction,
            int maxOutputTokens,
            String operationName
    ) {
        if (properties.getApiKey() == null
                || properties.getApiKey().isBlank()) {
            log.error("GEMINI_API_KEY가 설정되지 않았습니다.");
            return Optional.empty();
        }

        try {
            GeminiApiResponse response = restClient.post()
                    .uri(
                            "/v1beta/models/{model}:generateContent",
                            properties.getModel()
                    )
                    .header("x-goog-api-key", properties.getApiKey())
                    .body(createRequestBody(
                            messages,
                            systemInstruction,
                            maxOutputTokens
                    ))
                    .retrieve()
                    .body(GeminiApiResponse.class);

            String text = extractText(response);
            if (text == null || text.isBlank()) {
                log.error("{} 응답이 비어 있습니다.", operationName);
                return Optional.empty();
            }

            return Optional.of(text.trim());
        } catch (Exception exception) {
            // 사용자 발화 원문, API Key, 응답 본문은 로그에 기록하지 않습니다.
            log.error("{} 호출에 실패했습니다.", operationName);
            return Optional.empty();
        }
    }

    private Map<String, Object> createRequestBody(
            List<InteractionMessage> messages,
            String systemInstruction,
            int maxOutputTokens
    ) {
        List<Map<String, Object>> contents = messages.stream()
                .map(message -> Map.<String, Object>of(
                        "role",
                        message.getSenderType() == SenderType.ASSISTANT
                                ? "model"
                                : "user",
                        "parts",
                        List.of(Map.of("text", message.getContent()))
                ))
                .toList();

        return Map.of(
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of(
                                "text", systemInstruction
                        ))
                ),
                "contents", contents,
                "generationConfig", Map.of(
                        "temperature", 0.7,
                        "maxOutputTokens", maxOutputTokens
                )
        );
    }

    private String extractText(GeminiApiResponse response) {
        if (response == null
                || response.candidates() == null
                || response.candidates().isEmpty()) {
            return null;
        }

        Candidate candidate = response.candidates().get(0);
        if (candidate == null
                || candidate.content() == null
                || candidate.content().parts() == null
                || candidate.content().parts().isEmpty()) {
            return null;
        }

        Part part = candidate.content().parts().get(0);
        return part == null ? null : part.text();
    }

    private record GeminiApiResponse(List<Candidate> candidates) {
    }

    private record Candidate(Content content) {
    }

    private record Content(List<Part> parts) {
    }

    private record Part(String text) {
    }
}

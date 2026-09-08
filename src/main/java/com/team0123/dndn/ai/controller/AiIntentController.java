package com.team0123.dndn.ai.controller;

import com.team0123.dndn.ai.dto.IntentAnalyzeRequest;
import com.team0123.dndn.ai.dto.IntentAnalyzeResponse;
import com.team0123.dndn.ai.service.AiIntentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
/**
 * 금융 발화 Intent 분석 요청을 받는 Controller입니다.
 * 실제 분석은 AiIntentService에 위임하고,
 * Controller는 HTTP 요청과 응답만 담당합니다.
 */
@RestController
@RequestMapping("/api/ai")
public class AiIntentController {
    private final AiIntentService aiIntentService;
    public AiIntentController(AiIntentService aiIntentService) {
        this.aiIntentService = aiIntentService;
    }
    /**
     * 사용자의 금융 발화를 분석합니다.
     * 최종 주소:
     * POST /api/ai/intent
     * 요청 예시:
     * {
     *   "text": "엄마에게 20만 원 보내줘"
     * }
     */
    @PostMapping(
            value = "/intent",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public ResponseEntity<IntentAnalyzeResponse> analyzeIntent(
            // @Valid를 통해 Request DTO의 @NotBlank, @Size를 검사합니다.
            @Valid @RequestBody IntentAnalyzeRequest request
    ) {
        // Controller에서 직접 분석하지 않고 Service에 위임합니다.
        IntentAnalyzeResponse response =
                aiIntentService.analyze(request.text());

        // 분석 결과를 HTTP 200 응답으로 반환합니다.
        return ResponseEntity.ok(response);
    }
}
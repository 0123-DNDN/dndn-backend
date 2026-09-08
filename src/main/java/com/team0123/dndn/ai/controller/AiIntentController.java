package com.team0123.dndn.ai.controller;

import com.team0123.dndn.ai.dto.IntentAnalyzeRequest;
import com.team0123.dndn.ai.dto.IntentAnalyzeResponse;
import com.team0123.dndn.ai.service.AiIntentService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 금융 발화 Intent 분석 요청을 받는 Controller입니다.
 * 실제 분석은 AiIntentService에 위임하고,
 * Controller는 HTTP 요청과 응답 전달만 담당합니다.
 */
@RestController
@RequestMapping("/api/ai")
public class AiIntentController {

    private final AiIntentService aiIntentService;

    public AiIntentController(
            AiIntentService aiIntentService
    ) {
        this.aiIntentService = aiIntentService;
    }

    /**
     * 사용자의 금융 발화를 분석합니다.
     * 일반 AI 비서에서는 intentHint를 생략하고,
     * 특정 금융 기능에서 진입한 경우에는
     * 해당 기능을 intentHint로 전달할 수 있습니다.
     * POST /api/ai/intent
     */
    @PostMapping(
            value = "/intent",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
                    + ";charset=UTF-8"
    )
    public ResponseEntity<IntentAnalyzeResponse> analyzeIntent(
            @Valid @RequestBody IntentAnalyzeRequest request
    ) {
        /*
         * 사용자 발화와 화면 진입 Intent Hint를
         * Service에 함께 전달합니다.
         *
         * intentHint가 생략되면 null이 전달되어
         * 기존 일반 AI 비서 분석 방식으로 처리됩니다.
         */
        IntentAnalyzeResponse response =
                aiIntentService.analyze(
                        request.text(),
                        request.intentHint()
                );

        return ResponseEntity.ok(response);
    }
}
package com.team0123.dndn.ai.controller;

import com.team0123.dndn.ai.dto.ContextAnalyzeRequest;
import com.team0123.dndn.ai.dto.ContextAnalyzeResponse;
import com.team0123.dndn.ai.service.TransferContextService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자가 설명한 송금 목적의 금융사기 위험 Context를
 * 분석하는 API Controller입니다.
 * 송금 목적을 사용자에게 질문하는 것은 A/프론트가 담당하고,
 * B는 전달받은 문장을 분석하여 위험 신호와 추가 질문을 반환합니다.
 */
@RestController
@RequestMapping("/api/ai")
public class ContextAnalyzeController {

    private final TransferContextService transferContextService;

    public ContextAnalyzeController(
            TransferContextService transferContextService
    ) {
        this.transferContextService =
                transferContextService;
    }

    /**
     * 송금 목적을 1차 분석합니다.
     * 위험하거나 판단 정보가 부족한 경우
     * 프론트에 표시할 추가 질문을 함께 반환합니다.
     * POST /api/ai/context
     */
    @PostMapping(
            value = "/context",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
                    + ";charset=UTF-8"
    )
    public ResponseEntity<ContextAnalyzeResponse> analyze(
            @Valid @RequestBody ContextAnalyzeRequest request
    ) {
        ContextAnalyzeResponse response =
                transferContextService.analyze(
                        request.purposeText()
                );

        return ResponseEntity.ok(response);
    }
}
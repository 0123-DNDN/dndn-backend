package com.team0123.dndn.ai.controller;

import com.team0123.dndn.ai.dto.AiMessageSaveRequest;
import com.team0123.dndn.ai.dto.AiMessageSaveResponse;
import com.team0123.dndn.ai.dto.AiSessionResponse;
import com.team0123.dndn.ai.service.AiConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.team0123.dndn.ai.dto.AiAssistantMessageSaveRequest;

/**
 * AI 비서 대화 세션과 메시지 저장 요청을 처리합니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai/sessions")
public class AiConversationController {

    private final AiConversationService
            aiConversationService;

    /**
     * 로그인 사용자의 AI 비서 대화 세션을 생성합니다.
     * POST /api/ai/sessions
     */
    @PostMapping
    public ResponseEntity<AiSessionResponse> createSession(
            @AuthenticationPrincipal Long userId
    ) {
        AiSessionResponse response =
                aiConversationService.createSession(userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * 로그인 사용자의 메시지와 행동·음성 상태를 저장합니다.
     * POST /api/ai/sessions/{sessionId}/messages
     */
    @PostMapping("/{sessionId}/messages")
    public ResponseEntity<AiMessageSaveResponse> saveUserMessage(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long sessionId,
            @Valid @RequestBody AiMessageSaveRequest request
    ) {
        AiMessageSaveResponse response =
                aiConversationService.saveUserMessage(
                        userId,
                        sessionId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
    /**
     * 사용자에게 실제로 표시된 AI 비서 응답을 저장합니다.
     * POST /api/ai/sessions/{sessionId}/assistant-messages
     */
    @PostMapping("/{sessionId}/assistant-messages")
    public ResponseEntity<AiMessageSaveResponse> saveAssistantMessage(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long sessionId,
            @Valid
            @RequestBody
            AiAssistantMessageSaveRequest request
    ) {
        AiMessageSaveResponse response =
                aiConversationService.saveAssistantMessage(
                        userId,
                        sessionId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * 로그인 사용자의 AI 비서 대화 세션을 종료합니다.
     * PATCH /api/ai/sessions/{sessionId}/end
     */
    @PatchMapping("/{sessionId}/end")
    public ResponseEntity<AiSessionResponse> endSession(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long sessionId
    ) {
        AiSessionResponse response =
                aiConversationService.endSession(
                        userId,
                        sessionId
                );

        return ResponseEntity.ok(response);
    }
}
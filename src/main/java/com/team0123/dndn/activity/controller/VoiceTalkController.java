package com.team0123.dndn.activity.controller;

import com.team0123.dndn.activity.dto.VoiceTalkAnswerRequest;
import com.team0123.dndn.activity.dto.VoiceTalkAnswerResponse;
import com.team0123.dndn.activity.dto.VoiceTalkStartResponse;
import com.team0123.dndn.activity.service.VoiceTalkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/activities/voice-talk/sessions")
public class VoiceTalkController {

    private final VoiceTalkService voiceTalkService;

    @PostMapping
    public ResponseEntity<VoiceTalkStartResponse> start(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(voiceTalkService.start(userId));
    }

    @PostMapping("/{sessionId}/answers")
    public ResponseEntity<VoiceTalkAnswerResponse> answer(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long sessionId,
            @Valid @RequestBody VoiceTalkAnswerRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(voiceTalkService.answer(userId, sessionId, request));
    }
}

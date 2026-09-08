package com.team0123.dndn.voice.controller;

import com.team0123.dndn.voice.dto.VoiceAnalysisSaveRequest;
import com.team0123.dndn.voice.dto.VoiceAnalysisSaveResponse;
import com.team0123.dndn.voice.service.VoiceAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/voice")
public class VoiceAnalysisController {

    private final VoiceAnalysisService voiceAnalysisService;

    @PostMapping("/analysis")
    public ResponseEntity<VoiceAnalysisSaveResponse> save(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody VoiceAnalysisSaveRequest request
    ) {
        VoiceAnalysisSaveResponse response =
                voiceAnalysisService.save(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

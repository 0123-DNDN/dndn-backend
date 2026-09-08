package com.team0123.dndn.recipient.controller;

import com.team0123.dndn.recipient.dto.RecipientCreateRequest;
import com.team0123.dndn.recipient.dto.RecipientResponse;
import com.team0123.dndn.recipient.service.RecipientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recipients")
public class RecipientController {

    private final RecipientService recipientService;

    /**
     * 수취인 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<RecipientResponse>> getRecipients(
            @AuthenticationPrincipal Long userId
    ) {
        List<RecipientResponse> response =
                recipientService.getRecipients(userId);

        return ResponseEntity.ok(response);
    }

    /**
     * 수취인 검색
     */
    @GetMapping("/search")
    public ResponseEntity<List<RecipientResponse>> searchRecipients(
            @AuthenticationPrincipal Long userId,
            @RequestParam String keyword
    ) {
        List<RecipientResponse> response =
                recipientService.searchRecipients(userId, keyword);

        return ResponseEntity.ok(response);
    }

    /**
     * 새로운 수취인 등록
     */
    @PostMapping
    public ResponseEntity<RecipientResponse> createRecipient(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody RecipientCreateRequest request
    ) {
        RecipientResponse response =
                recipientService.createRecipient(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
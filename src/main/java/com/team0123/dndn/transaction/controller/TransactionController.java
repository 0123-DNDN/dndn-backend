package com.team0123.dndn.transaction.controller;

import com.team0123.dndn.transaction.dto.TransactionResponse;
import com.team0123.dndn.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @AuthenticationPrincipal Long userId
    ) {
        List<TransactionResponse> response =
                transactionService.getTransactions(userId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long transactionId
    ) {
        TransactionResponse response =
                transactionService.getTransaction(
                        userId,
                        transactionId
                );

        return ResponseEntity.ok(response);
    }
}
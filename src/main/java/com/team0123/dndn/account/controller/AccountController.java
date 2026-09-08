package com.team0123.dndn.account.controller;

import com.team0123.dndn.account.dto.*;
import com.team0123.dndn.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/verify")
    public ResponseEntity<AccountVerifyResponse> verifyAccount(
            @Valid @RequestBody AccountVerifyRequest request
    ) {
        AccountVerifyResponse response =
                accountService.verifyAccount(request);

        return ResponseEntity.ok(response);
    }

    /**
     * 로그인한 사용자의 본인 미연동 계좌 조회
     */
    @GetMapping("/available")
    public ResponseEntity<List<AccountResponse>> getAvailableAccounts(
            @AuthenticationPrincipal Long userId
    ) {
        List<AccountResponse> response =
                accountService.getAvailableAccounts(userId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/connect")
    public ResponseEntity<AccountResponse> connectAccount(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AccountConnectRequest request
    ) {
        AccountResponse response =
                accountService.connectAccount(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/main")
    public ResponseEntity<AccountResponse> getMainAccount(
            @AuthenticationPrincipal Long userId
    ) {
        AccountResponse response =
                accountService.getMainAccount(userId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<AccountBalanceResponse> getAccountBalance(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long accountId
    ) {
        AccountBalanceResponse response =
                accountService.getAccountBalance(userId, accountId);

        return ResponseEntity.ok(response);
    }
}
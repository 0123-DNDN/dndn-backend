package com.team0123.dndn.transfer.controller;

import com.team0123.dndn.transfer.dto.FdsCheckResponse;
import com.team0123.dndn.transfer.dto.FdsCheckRequest;
import com.team0123.dndn.transfer.dto.TransferCreateRequest;
import com.team0123.dndn.transfer.dto.TransferResponse;
import com.team0123.dndn.transfer.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;
    private final com.team0123.dndn.user.repository.UserRepository userRepository;
    private final com.team0123.dndn.account.repository.AccountRepository accountRepository;

    @GetMapping("/guardian/pending")
    public ResponseEntity<List<TransferResponse>> guardianPending(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(transferService.getGuardianPendingTransfers(userId));
    }

    @GetMapping("/{transactionId}/guardian-review")
    public ResponseEntity<TransferResponse> guardianReview(
            @AuthenticationPrincipal Long userId, @PathVariable Long transactionId
    ) {
        TransferResponse result = transferService.getGuardianTransfer(userId, transactionId);
        accountRepository.findById(result.getSenderAccountId())
                .flatMap(account -> userRepository.findById(account.getUserId()))
                .ifPresent(user -> result.setSenderContact(user.getName(), user.getPhone()));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{transactionId}/delay-confirm")
    public ResponseEntity<TransferResponse> confirmDelay(
            @AuthenticationPrincipal Long userId, @PathVariable Long transactionId
    ) {
        return ResponseEntity.ok(transferService.confirmDelay(userId, transactionId));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<TransferResponse>> pending(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(transferService.getTransactions(userId).stream()
                .filter(t -> t.getStatus() == com.team0123.dndn.transfer.entity.TransferStatus.DELAY_CONFIRM
                        || t.getStatus() == com.team0123.dndn.transfer.entity.TransferStatus.WAITING_GUARDIAN
                        || t.getStatus() == com.team0123.dndn.transfer.entity.TransferStatus.GUARDIAN_APPROVED
                        || t.getStatus() == com.team0123.dndn.transfer.entity.TransferStatus.FINAL_CONFIRMED)
                .toList());
    }

    /**
     * 송금 생성
     *
     * 실제 잔액은 차감하지 않고
     * 송금 정보를 저장합니다.
     */
    @PostMapping
    public ResponseEntity<TransferResponse> createTransfer(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody TransferCreateRequest request
    ) {

        TransferResponse response =
                transferService.createTransfer(
                        userId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    /**
     * 전체 거래내역 조회
     */
    @GetMapping("/../transactions")
    public ResponseEntity<List<TransferResponse>> getTransactions(
            @AuthenticationPrincipal Long userId
    ) {

        List<TransferResponse> response =
                transferService.getTransactions(userId);

        return ResponseEntity.ok(response);
    }

    /**
     * 송금 조회
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransferResponse> getTransfer(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long transactionId
    ) {

        TransferResponse response =
                transferService.getTransfer(
                        userId,
                        transactionId
                );

        return ResponseEntity.ok(response);
    }

    /**
     * 수취인 확인
     *
     * CREATED
     * → RECIPIENT_CONFIRMED
     */
    @PostMapping("/{transactionId}/recipient-confirm")
    public ResponseEntity<TransferResponse> confirmRecipient(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long transactionId
    ) {

        TransferResponse response =
                transferService.confirmRecipient(
                        userId,
                        transactionId
                );

        return ResponseEntity.ok(response);
    }

    /**
     * 금액 확인
     *
     * RECIPIENT_CONFIRMED
     * → AMOUNT_CONFIRMED
     */
    @PostMapping("/{transactionId}/amount-confirm")
    public ResponseEntity<TransferResponse> confirmAmount(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long transactionId
    ) {

        TransferResponse response =
                transferService.confirmAmount(
                        userId,
                        transactionId
                );

        return ResponseEntity.ok(response);
    }

    /**
     * FDS 검사 시작
     *
     * AMOUNT_CONFIRMED
     * → FDS_CHECKING
     */
    @PostMapping("/{transactionId}/fds-check")
    public ResponseEntity<FdsCheckResponse> startFdsCheck(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long transactionId,
            @Valid @RequestBody(required = false) FdsCheckRequest request
    ) {

        FdsCheckResponse response =
                transferService.startFdsCheck(
                        userId,
                        transactionId,
                        request == null ? FdsCheckRequest.empty() : request
                );

        return ResponseEntity.ok(response);
    }

    /**
     * 송금 최종 확인
     */
    @PostMapping("/{transactionId}/final-confirm")
    public ResponseEntity<TransferResponse> finalConfirm(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long transactionId
    ) {

        TransferResponse response =
                transferService.finalConfirm(
                        userId,
                        transactionId
                );

        return ResponseEntity.ok(response);
    }

    /**
     * 송금 완료
     *
     * FINAL_CONFIRMED
     * → COMPLETED
     *
     * 실제 잔액 차감 및 거래 완료 처리
     */
    @PostMapping("/{transactionId}/complete")
    public ResponseEntity<TransferResponse> completeTransfer(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long transactionId
    ) {

        TransferResponse response =
                transferService.completeTransfer(
                        userId,
                        transactionId
                );

        return ResponseEntity.ok(response);
    }

    /**
     * 보호자 승인
     *
     * WAITING_GUARDIAN
     * → GUARDIAN_APPROVED
     */
    @PostMapping("/{transactionId}/guardian-approve")
    public ResponseEntity<TransferResponse> guardianApprove(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long transactionId
    ) {

        TransferResponse response =
                transferService.guardianApprove(
                        userId,
                        transactionId
                );

        return ResponseEntity.ok(response);
    }

    /**
     * 보호자 거절
     *
     * WAITING_GUARDIAN
     * → CANCELLED
     */
    @PostMapping("/{transactionId}/guardian-reject")
    public ResponseEntity<TransferResponse> guardianReject(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long transactionId
    ) {

        TransferResponse response =
                transferService.guardianReject(
                        userId,
                        transactionId
                );

        return ResponseEntity.ok(response);
    }

    /**
     * 송금 취소
     */
    @PostMapping("/{transactionId}/cancel")
    public ResponseEntity<TransferResponse> cancelTransfer(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long transactionId
    ) {

        TransferResponse response =
                transferService.cancelTransfer(
                        userId,
                        transactionId
                );

        return ResponseEntity.ok(response);
    }
}

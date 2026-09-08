package com.team0123.dndn.transfer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "sender_account_id", nullable = false)
    private Long senderAccountId;

    @Column(name = "receiver_account_id")
    private Long receiverAccountId;

    @Column(name = "receiver_bank_code", nullable = false, length = 10)
    private String receiverBankCode;

    @Column(name = "receiver_account_number", nullable = false, length = 30)
    private String receiverAccountNumber;

    @Column(name = "receiver_name", nullable = false, length = 50)
    private String receiverName;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "sender_balance_before", nullable = false)
    private Long senderBalanceBefore;

    @Column(name = "sender_balance_after")
    private Long senderBalanceAfter;

    @Column(name = "purpose", nullable = false, length = 255)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransferStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();

        if (this.status == null) {
            this.status = TransferStatus.CREATED;
        }
    }

    /**
     * 송금 상태 변경
     */
    public void changeStatus(TransferStatus status) {
        this.status = status;
    }

    /**
     * 송금 완료 처리
     */
    public void complete(Long senderBalanceAfter) {
        this.status = TransferStatus.COMPLETED;
        this.senderBalanceAfter = senderBalanceAfter;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 송금 취소 처리
     */
    public void cancel() {
        this.status = TransferStatus.CANCELLED;
    }
}
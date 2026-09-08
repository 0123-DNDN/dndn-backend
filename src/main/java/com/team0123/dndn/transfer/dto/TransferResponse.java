package com.team0123.dndn.transfer.dto;

import com.team0123.dndn.transfer.entity.Transfer;
import com.team0123.dndn.transfer.entity.TransferStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TransferResponse {

    private Long transactionId;

    private Long senderAccountId;

    private String senderAccountNumber;

    private Long receiverAccountId;

    private String receiverBankCode;

    private String receiverAccountNumber;

    private String receiverName;

    private Long amount;

    private Long senderBalanceBefore;

    private Long senderBalanceAfter;

    private String purpose;

    private TransferStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    public static TransferResponse from(Transfer transfer, String senderAccountNumber) {
        return TransferResponse.builder()
                .transactionId(transfer.getTransactionId())
                .senderAccountId(transfer.getSenderAccountId())
                .senderAccountNumber(senderAccountNumber)
                .receiverAccountId(transfer.getReceiverAccountId())
                .receiverBankCode(transfer.getReceiverBankCode())
                .receiverAccountNumber(transfer.getReceiverAccountNumber())
                .receiverName(transfer.getReceiverName())
                .amount(transfer.getAmount())
                .senderBalanceBefore(transfer.getSenderBalanceBefore())
                .senderBalanceAfter(transfer.getSenderBalanceAfter())
                .purpose(transfer.getPurpose())
                .status(transfer.getStatus())
                .createdAt(transfer.getCreatedAt())
                .completedAt(transfer.getCompletedAt())
                .build();
    }
}
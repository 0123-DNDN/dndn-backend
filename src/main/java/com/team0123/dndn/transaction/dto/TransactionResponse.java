package com.team0123.dndn.transaction.dto;

import com.team0123.dndn.fds.type.RiskLevel;
import com.team0123.dndn.transfer.entity.Transfer;
import com.team0123.dndn.transfer.entity.TransferStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TransactionResponse {

    private Long transactionId;

    private String senderAccountNumber;

    private String receiverBankCode;
    private String receiverAccountNumber;
    private String receiverName;

    private Long amount;
    private TransferStatus status;
    private RiskLevel riskLevel;
    private String purpose;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public static TransactionResponse from(
            Transfer transfer,
            String senderAccountNumber,
            RiskLevel riskLevel
    ) {
        return TransactionResponse.builder()
                .transactionId(transfer.getTransactionId())
                .senderAccountNumber(senderAccountNumber)
                .receiverBankCode(transfer.getReceiverBankCode())
                .receiverAccountNumber(transfer.getReceiverAccountNumber())
                .receiverName(transfer.getReceiverName())
                .amount(transfer.getAmount())
                .status(transfer.getStatus())
                .riskLevel(riskLevel)
                .purpose(transfer.getPurpose())
                .createdAt(transfer.getCreatedAt())
                .completedAt(transfer.getCompletedAt())
                .build();
    }
}
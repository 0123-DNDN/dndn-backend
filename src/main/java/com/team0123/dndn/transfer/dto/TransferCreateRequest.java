package com.team0123.dndn.transfer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

@Getter
public class TransferCreateRequest {

    @NotNull
    private Long senderAccountId;

    /**
     * 등록된 수취인 별칭으로 송금할 때 사용합니다.
     * 예: "아들", "딸", "은행"
     */
    private String recipientAlias;

    /**
     * 계좌번호로 직접 송금할 때 사용합니다.
     */
    private String receiverBankCode;

    private String receiverAccountNumber;

    @NotNull
    @Positive
    private Long amount;

    private String purpose;
}
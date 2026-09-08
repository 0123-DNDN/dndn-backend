package com.team0123.dndn.recipient.dto;

import com.team0123.dndn.recipient.entity.RecipientAlias;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RecipientResponse {

    private Long aliasId;
    private String aliasName;
    private String bankCode;
    private String accountNumber;
    private String recipientName;
    private LocalDateTime createdAt;

    public static RecipientResponse from(RecipientAlias recipientAlias) {
        return RecipientResponse.builder()
                .aliasId(recipientAlias.getAliasId())
                .aliasName(recipientAlias.getAliasName())
                .bankCode(recipientAlias.getBankCode())
                .accountNumber(recipientAlias.getAccountNumber())
                .recipientName(recipientAlias.getRecipientName())
                .createdAt(recipientAlias.getCreatedAt())
                .build();
    }
}
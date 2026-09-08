package com.team0123.dndn.recipient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class RecipientCreateRequest {

    @NotBlank
    @Size(max = 50)
    private String aliasName;

    @NotBlank
    @Size(max = 10)
    private String bankCode;

    @NotBlank
    @Size(max = 30)
    private String accountNumber;

    @NotBlank
    @Size(max = 50)
    private String recipientName;
}
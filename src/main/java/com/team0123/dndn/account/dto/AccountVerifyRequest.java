package com.team0123.dndn.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class AccountVerifyRequest {

    @NotBlank
    private String bankCode;

    @NotBlank
    private String accountNumber;

    @NotBlank
    private String accountHolder;

    @Past
    private LocalDate birthDate;
}
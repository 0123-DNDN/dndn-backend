package com.team0123.dndn.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class PhoneVerificationVerifyRequest {

    @NotBlank
    @Pattern(regexp = "^01[0-9]{8,9}$")
    private String phone;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$")
    private String code;
}
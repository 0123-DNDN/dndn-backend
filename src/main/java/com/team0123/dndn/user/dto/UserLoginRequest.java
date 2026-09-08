package com.team0123.dndn.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class UserLoginRequest {

    @NotBlank
    private String phone;

    @NotBlank
    private String password;
}
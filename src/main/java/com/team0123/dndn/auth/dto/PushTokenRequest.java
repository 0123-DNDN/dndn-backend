package com.team0123.dndn.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class PushTokenRequest {

    @NotBlank
    private String expoPushToken;
}
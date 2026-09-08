package com.team0123.dndn.account.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountVerifyResponse {

    private boolean verified;
    private String message;
}
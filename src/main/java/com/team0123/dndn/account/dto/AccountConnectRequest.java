package com.team0123.dndn.account.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class AccountConnectRequest {

    @NotNull
    private Long accountId;
}
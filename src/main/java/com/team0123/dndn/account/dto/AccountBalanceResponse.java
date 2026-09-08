package com.team0123.dndn.account.dto;

import com.team0123.dndn.account.entity.Account;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountBalanceResponse {

    private Long accountId;
    private Long balance;

    public static AccountBalanceResponse from(Account account) {
        return AccountBalanceResponse.builder()
                .accountId(account.getAccountId())
                .balance(account.getBalance())
                .build();
    }
}
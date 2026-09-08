package com.team0123.dndn.account.dto;

import com.team0123.dndn.account.entity.Account;
import com.team0123.dndn.account.entity.AccountStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountResponse {

    private Long accountId;
    private String bankCode;
    private String accountNumber;
    private String accountName;
    private Long balance;
    private Boolean isPrimary;
    private Boolean isRegistration;
    private AccountStatus status;

    public static AccountResponse from(Account account) {

        return AccountResponse.builder()
                .accountId(account.getAccountId())
                .bankCode(account.getBankCode())
                .accountNumber(account.getAccountNumber())
                .accountName(account.getAccountName())
                .balance(account.getBalance())
                .isPrimary(account.getIsPrimary())
                .isRegistration(account.getIsRegistration())
                .status(account.getStatus())
                .build();
    }
}
package com.team0123.dndn.account.service;

import com.team0123.dndn.account.dto.*;
import com.team0123.dndn.account.entity.Account;
import com.team0123.dndn.account.repository.AccountRepository;
import com.team0123.dndn.user.entity.User;
import com.team0123.dndn.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    /**
     * 계좌 인증
     */
    public AccountVerifyResponse verifyAccount(
            AccountVerifyRequest request
    ) {

        if (request.getAccountNumber() == null
                || request.getAccountNumber().isBlank()) {

            return AccountVerifyResponse.builder()
                    .verified(false)
                    .message("계좌번호가 올바르지 않습니다.")
                    .build();
        }

        if (request.getAccountHolder() == null
                || request.getAccountHolder().isBlank()) {

            return AccountVerifyResponse.builder()
                    .verified(false)
                    .message("예금주 정보가 올바르지 않습니다.")
                    .build();
        }

        return AccountVerifyResponse.builder()
                .verified(true)
                .message("계좌 인증이 완료되었습니다.")
                .build();
    }

    /**
     * 로그인한 사용자의 본인 미연동 계좌 조회
     *
     * 이름 + 생년월일이 일치하고
     * 아직 연동되지 않은 계좌만 조회합니다.
     */
    public List<AccountResponse> getAvailableAccounts(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "사용자를 찾을 수 없습니다."
                        )
                );

        return accountRepository
                .findAllByOwnerNameAndBirthDateAndUserIdIsNullAndIsRegistrationFalse(
                        user.getName(),
                        user.getBirthDate()
                )
                .stream()
                .map(AccountResponse::from)
                .toList();
    }

    /**
     * 기존 미연동 계좌를 로그인한 사용자에게 연동
     */
    @Transactional
    public AccountResponse connectAccount(
            Long userId,
            AccountConnectRequest request
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "사용자를 찾을 수 없습니다."
                        )
                );

        Account account = accountRepository
                .findById(request.getAccountId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "계좌를 찾을 수 없습니다."
                        )
                );

        /*
         * 이미 다른 사용자에게 연동된 계좌인지 확인
         */
        if (account.getUserId() != null
                || Boolean.TRUE.equals(account.getIsRegistration())) {

            throw new IllegalArgumentException(
                    "이미 연동된 계좌입니다."
            );
        }

        /*
         * 로그인한 사용자의 이름과 생년월일이
         * 계좌의 예금주 정보와 일치하는지 확인
         */
        if (!account.getOwnerName().equals(user.getName())
                || !account.getBirthDate().equals(user.getBirthDate())) {

            throw new IllegalArgumentException(
                    "본인 명의의 계좌만 연동할 수 있습니다."
            );
        }

        /*
         * 현재 사용자가 이미 연동한 계좌가 있는지 확인
         *
         * 첫 번째 계좌라면 대표 계좌로 설정합니다.
         */
        boolean isFirstAccount = accountRepository
                .findAllByUserId(userId)
                .isEmpty();

        account.registerToUser(userId);

        if (isFirstAccount) {
            account.makePrimary();
        }

        return AccountResponse.from(account);
    }

    /**
     * 대표 계좌 조회
     */
    public AccountResponse getMainAccount(Long userId) {

        Account account = accountRepository
                .findByUserIdAndIsPrimaryTrue(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "대표 계좌가 존재하지 않습니다."
                        )
                );

        return AccountResponse.from(account);
    }

    /**
     * 계좌 잔액 조회
     */
    public AccountBalanceResponse getAccountBalance(
            Long userId,
            Long accountId
    ) {

        Account account = accountRepository
                .findByAccountIdAndUserId(accountId, userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "조회할 수 없는 계좌입니다."
                        )
                );

        return AccountBalanceResponse.from(account);
    }
}
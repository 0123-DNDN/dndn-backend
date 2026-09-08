package com.team0123.dndn.transaction.service;

import com.team0123.dndn.account.entity.Account;
import com.team0123.dndn.account.repository.AccountRepository;
import com.team0123.dndn.fds.entity.FdsRiskAnalysis;
import com.team0123.dndn.fds.repository.FdsRiskAnalysisRepository;
import com.team0123.dndn.fds.type.RiskLevel;
import com.team0123.dndn.transaction.dto.TransactionResponse;
import com.team0123.dndn.transfer.entity.Transfer;
import com.team0123.dndn.transfer.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final FdsRiskAnalysisRepository fdsRiskAnalysisRepository;

    public List<TransactionResponse> getTransactions(Long userId) {

        List<Account> accounts =
                accountRepository.findAllByUserId(userId);

        return accounts.stream()
                .flatMap(account ->
                        transferRepository
                                .findAllBySenderAccountIdOrderByCreatedAtDesc(
                                        account.getAccountId()
                                )
                                .stream()
                )
                .sorted(Comparator.comparing(
                        Transfer::getCreatedAt
                ).reversed())
                .map(this::toResponse)
                .toList();
    }

    public TransactionResponse getTransaction(
            Long userId,
            Long transactionId
    ) {

        List<Account> accounts =
                accountRepository.findAllByUserId(userId);

        List<Long> accountIds = accounts.stream()
                .map(Account::getAccountId)
                .toList();

        Transfer transfer = transferRepository
                .findByTransactionId(transactionId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 거래입니다."
                        )
                );

        if (!accountIds.contains(transfer.getSenderAccountId())) {
            throw new IllegalArgumentException(
                    "조회할 수 없는 거래입니다."
            );
        }

        return toResponse(transfer);
    }

    private TransactionResponse toResponse(Transfer transfer) {

        RiskLevel riskLevel =
                fdsRiskAnalysisRepository
                        .findTopByTransactionIdOrderByAnalyzedAtDesc(
                                transfer.getTransactionId()
                        )
                        .map(FdsRiskAnalysis::getRiskLevel)
                        .orElse(null);

        return TransactionResponse.from(
                transfer,
                riskLevel
        );
    }
}
package com.team0123.dndn.recipient.repository;

import com.team0123.dndn.recipient.entity.RecipientAlias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecipientAliasRepository
        extends JpaRepository<RecipientAlias, Long> {

    // 사용자의 등록된 수취인 전체 조회
    List<RecipientAlias> findAllByUserId(Long userId);

    // 별칭(엄마, 아들, 철수 등)으로 검색
    List<RecipientAlias> findByUserIdAndAliasNameContainingIgnoreCase(
            Long userId,
            String aliasName
    );

    // 실제 예금주 이름으로 검색
    List<RecipientAlias> findByUserIdAndRecipientNameContainingIgnoreCase(
            Long userId,
            String recipientName
    );

    // 동일한 수취 계좌가 이미 등록되어 있는지 확인
    boolean existsByUserIdAndBankCodeAndAccountNumber(
            Long userId,
            String bankCode,
            String accountNumber
    );
}
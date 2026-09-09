package com.team0123.dndn.account.repository;

import com.team0123.dndn.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from Account a where a.accountId = :id")
    Optional<Account> findForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    /**
     * 사용자의 대표 계좌 조회
     */
    Optional<Account> findByUserIdAndIsPrimaryTrue(Long userId);

    /**
     * 사용자가 연동한 모든 계좌 조회
     */
    List<Account> findAllByUserId(Long userId);

    /**
     * 특정 사용자가 연동한 계좌 조회
     */
    Optional<Account> findByAccountIdAndUserId(
            Long accountId,
            Long userId
    );

    /**
     * 계좌번호 중복 여부 확인
     */
    boolean existsByAccountNumber(String accountNumber);

    /**
     * 아직 연동되지 않은 계좌 중
     * 예금주 이름과 생년월일이 일치하는 계좌 조회
     */
    List<Account> findAllByOwnerNameAndBirthDateAndUserIdIsNullAndIsRegistrationFalse(
            String ownerName,
            LocalDate birthDate
    );

    /**
     * 특정 계좌번호의 미연동 계좌 조회
     */
    Optional<Account> findByBankCodeAndAccountNumberAndUserIdIsNullAndIsRegistrationFalse(
            String bankCode,
            String accountNumber
    );

    /**
     * 은행코드 + 계좌번호로 수취인 계좌를 조회합니다.
     *
     * 계좌번호 직접 송금에서 사용합니다.
     */
    Optional<Account> findByBankCodeAndAccountNumber(
            String bankCode,
            String accountNumber
    );
}

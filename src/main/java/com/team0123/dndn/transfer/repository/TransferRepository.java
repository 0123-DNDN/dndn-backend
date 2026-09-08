package com.team0123.dndn.transfer.repository;

import com.team0123.dndn.transfer.entity.Transfer;
import com.team0123.dndn.transfer.entity.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransferRepository
        extends JpaRepository<Transfer, Long> {

    Optional<Transfer> findByTransactionId(Long transactionId);

    Optional<Transfer> findByTransactionIdAndSenderAccountId(
            Long transactionId,
            Long senderAccountId
    );

    List<Transfer> findAllBySenderAccountIdOrderByCreatedAtDesc(
            Long senderAccountId
    );

    /**
     * 특정 계좌의 특정 시각 이후 송금 내역을 조회합니다.
     *
     * FDS 거래 빈도 및 최근 송금 패턴 계산에 사용합니다.
     */
    List<Transfer> findAllBySenderAccountIdAndCreatedAtAfter(
            Long senderAccountId,
            LocalDateTime after
    );

    /**
     * 특정 계좌의 특정 시각 이후 특정 상태 송금 건수를 조회합니다.
     *
     * FDS 실패 송금 횟수 계산에 사용합니다.
     */
    long countBySenderAccountIdAndCreatedAtAfterAndStatus(
            Long senderAccountId,
            LocalDateTime after,
            TransferStatus status
    );

    /**
     * 특정 계좌의 과거 송금 평균 금액을 조회합니다.
     *
     * 현재 송금 금액이 평소 송금 금액보다 얼마나 큰지
     * amountRatioToAverage 계산에 사용합니다.
     *
     * 아직 송금 이력이 없는 경우 null이 반환될 수 있습니다.
     */
    @Query("""
            SELECT AVG(t.amount)
            FROM Transfer t
            WHERE t.senderAccountId = :senderAccountId
            AND t.status = :status
            """)
    Double findAverageAmountBySenderAccountIdAndStatus(
            @Param("senderAccountId") Long senderAccountId,
            @Param("status") TransferStatus status
    );
}
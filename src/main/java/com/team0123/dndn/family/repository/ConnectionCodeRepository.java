package com.team0123.dndn.family.repository;

import com.team0123.dndn.family.entity.ConnectionCode;
import com.team0123.dndn.family.entity.ConnectionCodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConnectionCodeRepository
        extends JpaRepository<ConnectionCode, Long> {

    Optional<ConnectionCode> findByCodeAndStatus(
            String code,
            ConnectionCodeStatus status
    );

    Optional<ConnectionCode> findTopBySeniorUserIdAndStatusOrderByCreatedAtDesc(
            Long seniorUserId,
            ConnectionCodeStatus status
    );

    boolean existsByCodeAndStatus(
            String code,
            ConnectionCodeStatus status
    );
}
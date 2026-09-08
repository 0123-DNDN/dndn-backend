package com.team0123.dndn.fds.repository;

import com.team0123.dndn.fds.entity.FdsRiskAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FdsRiskAnalysisRepository
        extends JpaRepository<FdsRiskAnalysis, Long> {

    Optional<FdsRiskAnalysis> findTopByTransactionIdOrderByAnalyzedAtDesc(
            Long transactionId
    );
}
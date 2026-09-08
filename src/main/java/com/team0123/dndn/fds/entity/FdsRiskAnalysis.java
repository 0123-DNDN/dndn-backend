package com.team0123.dndn.fds.entity;

import com.team0123.dndn.fds.type.RiskLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "fds_risk_analysis")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FdsRiskAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_id")
    private Long analysisId;

    /**
     * 분석 대상 송금 거래 ID
     */
    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    /**
     * 거래 자체 위험 점수
     */
    @Column(name = "transaction_score", nullable = false)
    private int transactionScore;

    /**
     * 수취인 위험 점수
     */
    @Column(name = "recipient_score", nullable = false)
    private int recipientScore;

    /**
     * 거래 빈도 위험 점수
     */
    @Column(name = "velocity_score", nullable = false)
    private int velocityScore;

    /**
     * 기기 위험 점수
     */
    @Column(name = "device_score", nullable = false)
    private int deviceScore;

    /**
     * 송금 과정 행동 위험 점수
     */
    @Column(name = "behavior_score", nullable = false)
    private int behaviorScore;

    /**
     * Context 위험 점수
     */
    @Column(name = "context_score", nullable = false)
    private int contextScore;

    /**
     * 음성/채팅 상태 변화 위험 점수
     */
    @Column(name = "condition_score", nullable = false)
    private int conditionScore;

    /**
     * 최종 위험 점수
     */
    @Column(name = "total_score", nullable = false)
    private int totalScore;

    /**
     * FDS 위험 등급
     *
     * LOW / CAUTION / HIGH / CRITICAL
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    /**
     * Hard Rule 발동 여부
     */
    @Column(name = "hard_rule_triggered", nullable = false)
    private boolean hardRuleTriggered;

    /**
     * Combination Rule 발동 여부
     */
    @Column(name = "combination_rule_triggered", nullable = false)
    private boolean combinationRuleTriggered;

    /**
     * FDS 분석 시간
     */
    @Column(name = "analyzed_at", nullable = false)
    private LocalDateTime analyzedAt;

    @PrePersist
    protected void onCreate() {
        if (analyzedAt == null) {
            analyzedAt = LocalDateTime.now();
        }
    }
}
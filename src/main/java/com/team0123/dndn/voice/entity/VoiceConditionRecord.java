package com.team0123.dndn.voice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "voice_condition_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@DynamicInsert
public class VoiceConditionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "voice_condition_id")
    private Long voiceConditionId;

    @Column(name = "message_id", nullable = false, unique = true)
    private Long messageId;

    @Column(name = "measured_at", nullable = false, updatable = false)
    private LocalDateTime measuredAt;

    @Column(name = "speech_duration_ms")
    private Long speechDurationMs;

    @Column(name = "speech_rate", precision = 6, scale = 2)
    private BigDecimal speechRate;

    @Column(name = "avg_pause_duration_ms")
    private Long avgPauseDurationMs;

    @ColumnDefault("0")
    @Column(name = "long_pause_count", nullable = false)
    private Integer longPauseCount;

    @Column(name = "pitch_mean", precision = 8, scale = 2)
    private BigDecimal pitchMean;

    @Column(name = "pitch_variation", precision = 8, scale = 2)
    private BigDecimal pitchVariation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.measuredAt == null) {
            this.measuredAt = now;
        }
        this.createdAt = now;
    }
}

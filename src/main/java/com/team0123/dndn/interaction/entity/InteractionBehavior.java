package com.team0123.dndn.interaction.entity;

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

import java.time.LocalDateTime;

@Entity
@Table(name = "interaction_behavior")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@DynamicInsert
public class InteractionBehavior {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "behavior_id")
    private Long behaviorId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "message_id", unique = true)
    private Long messageId;

    @Column(name = "response_delay_ms")
    private Long responseDelayMs;

    @Column(name = "typing_duration_ms")
    private Long typingDurationMs;

    @ColumnDefault("0")
    @Column(name = "edit_count", nullable = false)
    private Integer editCount;

    @ColumnDefault("0")
    @Column(name = "full_delete_count", nullable = false)
    private Integer fullDeleteCount;

    @ColumnDefault("0")
    @Column(name = "typing_pause_count", nullable = false)
    private Integer typingPauseCount;

    @ColumnDefault("0")
    @Column(name = "answer_reversal_count", nullable = false)
    private Integer answerReversalCount;

    @ColumnDefault("0")
    @Column(name = "confusion_count", nullable = false)
    private Integer confusionCount;

    @ColumnDefault("0")
    @Column(name = "reexplanation_count", nullable = false)
    private Integer reexplanationCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

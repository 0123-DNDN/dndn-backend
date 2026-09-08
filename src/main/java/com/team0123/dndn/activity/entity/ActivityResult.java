package com.team0123.dndn.activity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "activity_results",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_activity_results_user_activity_date",
                columnNames = {"senior_user_id", "activity_id", "activity_date"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ActivityResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_result_id")
    private Long activityResultId;

    @Column(name = "activity_id", nullable = false)
    private Long activityId;

    @Column(name = "senior_user_id", nullable = false)
    private Long seniorUserId;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ActivityStatus status;

    @Column(name = "score")
    private Integer score;

    @Column(name = "step_count")
    private Integer stepCount;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static ActivityResult create(
            Long activityId,
            Long seniorUserId,
            LocalDate activityDate,
            ActivityStatus status,
            Integer score,
            Integer stepCount,
            Long sessionId,
            LocalDateTime now
    ) {
        return ActivityResult.builder()
                .activityId(activityId)
                .seniorUserId(seniorUserId)
                .sessionId(sessionId)
                .activityDate(activityDate)
                .status(status)
                .score(score)
                .stepCount(stepCount)
                .startedAt(now)
                .completedAt(status == ActivityStatus.COMPLETED ? now : null)
                .build();
    }

    public void update(
            ActivityStatus status,
            Integer score,
            Integer stepCount,
            Long sessionId,
            LocalDateTime now
    ) {
        this.status = status;
        this.score = score;
        this.stepCount = stepCount;
        this.sessionId = sessionId;

        if (this.startedAt == null) {
            this.startedAt = now;
        }
        if (status == ActivityStatus.COMPLETED && this.completedAt == null) {
            this.completedAt = now;
        }
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.startedAt == null) {
            this.startedAt = now;
        }
        if (this.status == ActivityStatus.COMPLETED && this.completedAt == null) {
            this.completedAt = now;
        }
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

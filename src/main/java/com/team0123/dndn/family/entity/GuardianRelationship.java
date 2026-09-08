package com.team0123.dndn.family.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "guardian_relationship")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GuardianRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "relationship_id")
    private Long relationshipId;

    @Column(name = "senior_user_id", nullable = false)
    private Long seniorUserId;

    @Column(name = "guardian_user_id", nullable = false)
    private Long guardianUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GuardianRelationshipStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();

        if (this.status == null) {
            this.status = GuardianRelationshipStatus.PENDING;
        }
    }

    public void approve() {
        this.status = GuardianRelationshipStatus.ACTIVE;
        this.approvedAt = LocalDateTime.now();
    }
}
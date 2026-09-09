package com.team0123.dndn.family.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "connection_codes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ConnectionCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "connection_code_id")
    private Long connectionCodeId;

    @Column(name = "senior_user_id", nullable = false)
    private Long seniorUserId;

    @Column(name = "used_by_guardian_id")
    private Long usedByGuardianId;

    @Column(name = "code", nullable = false, length = 6)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ConnectionCodeStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();

        if (this.status == null) {
            this.status = ConnectionCodeStatus.ACTIVE;
        }
    }

    public void use(Long guardianUserId) {
        this.usedByGuardianId = guardianUserId;
        this.usedAt = LocalDateTime.now();
        this.status = ConnectionCodeStatus.USED;
    }

    public void expire() {
        this.status = ConnectionCodeStatus.EXPIRED;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
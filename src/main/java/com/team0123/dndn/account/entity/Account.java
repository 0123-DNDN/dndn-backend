package com.team0123.dndn.account.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_accounts_account_number",
                        columnNames = "account_number"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long accountId;

    /**
     * 계좌가 연동된 경우에만 사용자 ID가 존재합니다.
     * 미연동 계좌는 NULL입니다.
     */
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "owner_name", nullable = false, length = 50)
    private String ownerName;

    @Column(name = "bank_code", nullable = false, length = 10)
    private String bankCode;

    @Column(name = "account_number", nullable = false, unique = true, length = 30)
    private String accountNumber;

    @Column(name = "account_name", nullable = false, length = 100)
    private String accountName;

    @Column(name = "balance", nullable = false)
    private Long balance;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary;

    @Column(name = "is_registration", nullable = false)
    private Boolean isRegistration;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;

        if (this.isPrimary == null) {
            this.isPrimary = false;
        }

        if (this.isRegistration == null) {
            this.isRegistration = false;
        }

        if (this.status == null) {
            this.status = AccountStatus.ACTIVE;
        }

        if (this.balance == null) {
            this.balance = 0L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 미연동 계좌를 사용자 계좌로 연동합니다.
     */
    public void registerToUser(Long userId) {

        if (this.userId != null || Boolean.TRUE.equals(this.isRegistration)) {
            throw new IllegalArgumentException(
                    "이미 연동된 계좌입니다."
            );
        }

        this.userId = userId;
        this.isRegistration = true;
    }

    /**
     * 대표 계좌로 설정합니다.
     */
    public void makePrimary() {
        this.isPrimary = true;
    }

    /**
     * 계좌 잔액을 차감합니다.
     */
    public void withdraw(Long amount) {

        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException(
                    "출금 금액은 0보다 커야 합니다."
            );
        }

        if (balance < amount) {
            throw new IllegalArgumentException(
                    "계좌 잔액이 부족합니다."
            );
        }

        balance -= amount;
    }
}
package com.team0123.dndn.transfer.entity;

public enum TransferStatus {

    CREATED,

    RECIPIENT_CONFIRMED,

    AMOUNT_CONFIRMED,

    FDS_CHECKING,

    NORMAL,
    DELAY_CONFIRM,

    HIGH_RISK,

    WAITING_GUARDIAN,

    GUARDIAN_APPROVED,

    GUARDIAN_REJECTED,

    FINAL_CONFIRMED,

    COMPLETED,

    CANCELLED
}

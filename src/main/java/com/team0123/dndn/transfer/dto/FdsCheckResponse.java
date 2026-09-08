package com.team0123.dndn.transfer.dto;

import com.team0123.dndn.transfer.entity.TransferStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FdsCheckResponse {

    private Long transactionId;

    private TransferStatus status;

    private FdsResultResponse fds;

    public static FdsCheckResponse from(
            Long transactionId,
            TransferStatus status,
            FdsResultResponse fds
    ) {
        return FdsCheckResponse.builder()
                .transactionId(transactionId)
                .status(status)
                .fds(fds)
                .build();
    }
}
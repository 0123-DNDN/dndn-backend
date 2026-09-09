package com.team0123.dndn.family.dto;

import com.team0123.dndn.family.entity.ConnectionCode;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ConnectionCodeResponse {

    private String code;
    private LocalDateTime expiresAt;

    public static ConnectionCodeResponse from(ConnectionCode connectionCode) {
        return ConnectionCodeResponse.builder()
                .code(connectionCode.getCode())
                .expiresAt(connectionCode.getExpiresAt())
                .build();
    }
}
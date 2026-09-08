package com.team0123.dndn.common.exception;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {

    private int status;
    private String message;
}
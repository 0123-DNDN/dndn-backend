package com.team0123.dndn.family.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FamilyConnectRequest {

    @NotBlank(message = "가족 연결 코드를 입력해주세요.")
    @Pattern(
            regexp = "\\d{6}",
            message = "가족 연결 코드는 6자리 숫자여야 합니다."
    )
    private String code;
}
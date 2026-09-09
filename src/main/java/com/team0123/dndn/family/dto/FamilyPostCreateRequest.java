package com.team0123.dndn.family.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record FamilyPostCreateRequest(
        @NotNull(message = "가족 관계 ID가 필요합니다.")
        @Positive(message = "가족 관계 ID는 양수여야 합니다.")
        Long relationshipId,

        @Size(max = 500, message = "가족 소식 메시지는 500자 이하여야 합니다.")
        @Pattern(
                regexp = "(?s).*\\S.*",
                message = "가족 소식 메시지는 공백만 입력할 수 없습니다."
        )
        String message
) {
}

package com.team0123.dndn.ai.dto;

import com.team0123.dndn.ai.type.FollowUpQuestionCode;
import jakarta.validation.constraints.NotNull;

/**
 * 사용자가 추가 확인 질문에 답한 결과입니다.
 * 프론트는 B가 반환했던 질문 code와
 * 사용자가 선택한 예/아니오 값을 함께 전달합니다.
 */
public record FollowUpAnswer(

        // 어떤 추가 질문에 대한 답변인지 식별합니다.
        @NotNull(message = "추가 질문 코드는 필수입니다.")
        FollowUpQuestionCode code,

        /*
         * 예: true
         * 아니요: false
         *
         * Boolean을 사용하는 이유는 값이 누락된 경우와
         * false로 답한 경우를 구분하기 위해서입니다.
         */
        @NotNull(message = "추가 질문 답변은 필수입니다.")
        Boolean answer

) {

    /**
     * '예'라고 답한 질문인지 확인합니다.
     * Boolean.TRUE.equals()를 사용하면
     * answer가 null이어도 NullPointerException이 발생하지 않습니다.
     */
    public boolean answeredYes() {
        return Boolean.TRUE.equals(answer);
    }
}
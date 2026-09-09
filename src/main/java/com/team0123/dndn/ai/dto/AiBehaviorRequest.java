package com.team0123.dndn.ai.dto;

import jakarta.validation.constraints.PositiveOrZero;

/**
 * 프론트에서 측정한 AI 비서 사용자의 입력 행동 정보입니다.
 *
 * 프론트는 값을 측정해서 전달하고,
 * 백엔드는 해당 값을 메시지와 연결하여 저장합니다.
 */
public record AiBehaviorRequest(

        // AI 질문 후 사용자가 응답을 시작하기까지 걸린 시간
        @PositiveOrZero(
                message = "응답 지연시간은 0 이상이어야 합니다."
        )
        Long responseDelayMs,

        /*
         * 채팅 메시지 작성에 걸린 시간입니다.
         * 음성 입력에서는 null일 수 있습니다.
         */
        @PositiveOrZero(
                message = "입력 작성시간은 0 이상이어야 합니다."
        )
        Long typingDurationMs,

        // 전송 전에 입력 내용을 수정한 횟수
        @PositiveOrZero(
                message = "입력 수정 횟수는 0 이상이어야 합니다."
        )
        Integer editCount,

        // 입력 내용을 전체 삭제한 횟수
        @PositiveOrZero(
                message = "전체 삭제 횟수는 0 이상이어야 합니다."
        )
        Integer fullDeleteCount,

        // 메시지 작성 중 오래 멈춘 횟수
        @PositiveOrZero(
                message = "입력 중단 횟수는 0 이상이어야 합니다."
        )
        Integer typingPauseCount,

        // 동일 질문에 기존과 다른 답변을 한 횟수
        @PositiveOrZero(
                message = "답변 번복 횟수는 0 이상이어야 합니다."
        )
        Integer answerReversalCount,

        // 혼란 표현이 나타난 횟수
        @PositiveOrZero(
                message = "혼란 표현 횟수는 0 이상이어야 합니다."
        )
        Integer confusionCount,

        // 같은 내용을 다시 설명한 횟수
        @PositiveOrZero(
                message = "재설명 횟수는 0 이상이어야 합니다."
        )
        Integer reexplanationCount

) {

    /**
     * 횟수 값이 생략되면 DB 기본값과 동일하게
     * 0으로 사용할 수 있도록 보정합니다.
     */
    public int normalizedEditCount() {
        return editCount == null ? 0 : editCount;
    }

    public int normalizedFullDeleteCount() {
        return fullDeleteCount == null
                ? 0
                : fullDeleteCount;
    }

    public int normalizedTypingPauseCount() {
        return typingPauseCount == null
                ? 0
                : typingPauseCount;
    }

    public int normalizedAnswerReversalCount() {
        return answerReversalCount == null
                ? 0
                : answerReversalCount;
    }

    public int normalizedConfusionCount() {
        return confusionCount == null
                ? 0
                : confusionCount;
    }

    public int normalizedReexplanationCount() {
        return reexplanationCount == null
                ? 0
                : reexplanationCount;
    }
}
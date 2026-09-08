package com.team0123.dndn.ai.dto;

import com.team0123.dndn.ai.type.FollowUpQuestionCode;

/**
 * 프론트에 전달할 추가 확인 질문입니다.
 * 프론트는 questionText를 화면에 표시하고,
 * 사용자의 예/아니오 답변과 code를 서버에 다시 전달합니다.
 */
public record FollowUpQuestion(
        // 답변이 어떤 위험 신호에 대한 것인지 식별하는 코드
        FollowUpQuestionCode code,

        // 사용자에게 보여줄 질문 문구
        String questionText

) {

    /**
     * 질문 코드에 정의된 문구로 DTO를 생성합니다.
     * Service가 질문 문구를 중복 작성하지 않도록
     * 생성 로직을 이 메서드에 모읍니다.
     */
    public static FollowUpQuestion from(
            FollowUpQuestionCode code
    ) {
        if (code == null) {
            throw new IllegalArgumentException(
                    "추가 질문 코드는 null일 수 없습니다."
            );
        }

        return new FollowUpQuestion(
                code,
                code.questionText()
        );
    }
}
package com.team0123.dndn.ai.dto;

import com.team0123.dndn.interaction.entity.InteractionSession;

import java.time.LocalDateTime;

/**
 * AI 비서 대화 세션 생성 및 종료 결과입니다.
 */
public record AiSessionResponse(

        // AI 비서 대화 세션 ID
        Long sessionId,

        // 대화 시작 시각
        LocalDateTime startedAt,

        // 대화 종료 시각, 진행 중이면 null
        LocalDateTime endedAt

) {

    /**
     * InteractionSession Entity를 API 응답 DTO로 변환합니다.
     */
    public static AiSessionResponse from(
            InteractionSession session
    ) {
        if (session == null) {
            throw new IllegalArgumentException(
                    "대화 세션은 null일 수 없습니다."
            );
        }

        return new AiSessionResponse(
                session.getSessionId(),
                session.getStartedAt(),
                session.getEndedAt()
        );
    }
}
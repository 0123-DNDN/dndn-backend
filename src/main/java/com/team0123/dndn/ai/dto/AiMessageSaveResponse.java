package com.team0123.dndn.ai.dto;

import com.team0123.dndn.interaction.entity.InputType;
import com.team0123.dndn.interaction.entity.InteractionMessage;
import com.team0123.dndn.interaction.entity.SenderType;

import java.time.LocalDateTime;

/**
 * AI 비서 메시지 저장 결과입니다.
 */
public record AiMessageSaveResponse(

        // 메시지가 속한 AI 비서 대화 세션 ID
        Long sessionId,

        // 저장된 메시지 ID
        Long messageId,

        // 메시지 작성자: USER 또는 ASSISTANT
        SenderType senderType,

        /*
         * 사용자 입력 방식: CHAT 또는 VOICE
         * ASSISTANT 메시지인 경우 null입니다.
         */
        InputType inputType,

        // 저장된 메시지 내용
        String content,

        // 백엔드에서 기록한 메시지 생성 시각
        LocalDateTime createdAt

) {

    /**
     * 저장된 InteractionMessage Entity를
     * API 응답 DTO로 변환합니다.
     */
    public static AiMessageSaveResponse from(
            InteractionMessage message
    ) {
        if (message == null) {
            throw new IllegalArgumentException(
                    "대화 메시지는 null일 수 없습니다."
            );
        }

        return new AiMessageSaveResponse(
                message.getSessionId(),
                message.getMessageId(),
                message.getSenderType(),
                message.getInputType(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
package com.team0123.dndn.voice.dto;

import com.team0123.dndn.interaction.entity.InteractionMessage;

public record VoiceAnalysisSaveResponse(
        Long messageId,
        String status
) {

    public static VoiceAnalysisSaveResponse from(InteractionMessage message) {
        return new VoiceAnalysisSaveResponse(
                message.getMessageId(),
                "SAVED"
        );
    }
}

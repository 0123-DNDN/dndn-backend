package com.team0123.dndn.activity.dto;

public record VoiceTalkStartResponse(
        Long sessionId,
        String question,
        int totalQuestions
) {
}

package com.team0123.dndn.activity.dto;

public record VoiceTalkAnswerResponse(
        Long sessionId,
        String nextQuestion,
        String summary,
        int answeredCount,
        boolean completed
) {
}

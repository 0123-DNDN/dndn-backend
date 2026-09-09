package com.team0123.dndn.voice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VoiceDetailSample(
        LocalDateTime measuredAt,
        Long sessionId,
        BigDecimal speechRate,
        Long avgPauseDurationMs,
        String content
) {
}

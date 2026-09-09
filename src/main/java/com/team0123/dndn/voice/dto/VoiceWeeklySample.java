package com.team0123.dndn.voice.dto;

import java.math.BigDecimal;

public record VoiceWeeklySample(
        BigDecimal speechRate,
        Long avgPauseDurationMs,
        String content
) {
}

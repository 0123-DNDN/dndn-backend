package com.team0123.dndn.family.dto;

import java.math.BigDecimal;

public record VoiceWeeklySummary(
        int sampleCount,
        BigDecimal averageSpeechRate,
        String speechRateUnit,
        BigDecimal averagePauseDurationMs,
        BigDecimal averageUtteranceCharacters
) {
}

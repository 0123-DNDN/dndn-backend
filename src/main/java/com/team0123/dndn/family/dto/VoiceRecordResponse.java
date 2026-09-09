package com.team0123.dndn.family.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VoiceRecordResponse(
        LocalDateTime measuredAt,
        VoiceRecordSource source,
        BigDecimal speechRate,
        String speechRateUnit,
        Long avgPauseDurationMs,
        Long utteranceCharacters
) {
}

package com.team0123.dndn.family.dto;

import java.time.LocalDate;

public record CognitiveScorePoint(
        LocalDate date,
        Integer score
) {
}

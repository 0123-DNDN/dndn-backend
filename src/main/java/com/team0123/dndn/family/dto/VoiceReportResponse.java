package com.team0123.dndn.family.dto;

import java.time.LocalDate;
import java.util.List;

public record VoiceReportResponse(
        LocalDate startDate,
        LocalDate endDate,
        SeniorSummary senior,
        List<VoiceRecordResponse> records
) {
}

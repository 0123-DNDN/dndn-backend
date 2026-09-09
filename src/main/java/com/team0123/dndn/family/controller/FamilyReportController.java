package com.team0123.dndn.family.controller;

import com.team0123.dndn.family.dto.WeeklyFamilyReportResponse;
import com.team0123.dndn.family.dto.VoiceReportResponse;
import com.team0123.dndn.family.service.FamilyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/family/reports")
@RequiredArgsConstructor
public class FamilyReportController {

    private final FamilyReportService familyReportService;

    @GetMapping("/weekly")
    public ResponseEntity<WeeklyFamilyReportResponse> getWeeklyReport(
            @AuthenticationPrincipal Long guardianUserId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate weekStart
    ) {
        return ResponseEntity.ok(
                familyReportService.getWeeklyReport(
                        guardianUserId,
                        weekStart
                )
        );
    }

    @GetMapping("/voice")
    public ResponseEntity<VoiceReportResponse> getVoiceReport(
            @AuthenticationPrincipal Long guardianUserId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        return ResponseEntity.ok(
                familyReportService.getVoiceReport(
                        guardianUserId,
                        startDate,
                        endDate
                )
        );
    }
}

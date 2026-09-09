package com.team0123.dndn.family.service;

import com.team0123.dndn.activity.entity.Activity;
import com.team0123.dndn.activity.entity.ActivityResult;
import com.team0123.dndn.activity.entity.ActivityStatus;
import com.team0123.dndn.activity.entity.ActivityType;
import com.team0123.dndn.activity.repository.ActivityRepository;
import com.team0123.dndn.activity.repository.ActivityResultRepository;
import com.team0123.dndn.family.dto.WeeklyFamilyReportResponse;
import com.team0123.dndn.family.entity.GuardianRelationship;
import com.team0123.dndn.family.entity.GuardianRelationshipStatus;
import com.team0123.dndn.family.repository.GuardianRelationshipRepository;
import com.team0123.dndn.interaction.entity.SenderType;
import com.team0123.dndn.interaction.repository.InteractionMessageRepository;
import com.team0123.dndn.user.entity.Role;
import com.team0123.dndn.user.entity.User;
import com.team0123.dndn.user.repository.UserRepository;
import com.team0123.dndn.voice.dto.VoiceWeeklySample;
import com.team0123.dndn.voice.dto.VoiceDetailSample;
import com.team0123.dndn.voice.repository.VoiceConditionRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FamilyReportServiceTest {

    private static final Long GUARDIAN_ID = 1L;
    private static final Long SENIOR_ID = 6L;
    private static final LocalDate WEEK_START = LocalDate.of(2026, 9, 7);
    private static final LocalDate WEEK_END = LocalDate.of(2026, 9, 13);

    @Mock
    private GuardianRelationshipRepository guardianRelationshipRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActivityResultRepository activityResultRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private VoiceConditionRecordRepository voiceConditionRecordRepository;

    @Mock
    private InteractionMessageRepository interactionMessageRepository;

    private FamilyReportService familyReportService;

    @BeforeEach
    void setUp() {
        familyReportService = new FamilyReportService(
                guardianRelationshipRepository,
                userRepository,
                activityResultRepository,
                activityRepository,
                voiceConditionRecordRepository,
                interactionMessageRepository
        );
    }

    @Test
    void voiceDetailReturnsLinkedSeniorRecordsWithBoundariesSourcesAndNulls() {
        prepareGuardianAndSenior(GUARDIAN_ID, SENIOR_ID);
        LocalDate startDate = LocalDate.of(2026, 9, 1);
        LocalDate endDate = LocalDate.of(2026, 9, 9);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();
        List<VoiceDetailSample> samples = List.of(
                new VoiceDetailSample(
                        endDate.atTime(23, 59, 59),
                        20L,
                        new BigDecimal("3.50"),
                        null,
                        "abc de"
                ),
                new VoiceDetailSample(
                        start,
                        10L,
                        null,
                        1500L,
                        "안 녕"
                )
        );
        when(voiceConditionRecordRepository.findDetailSamplesByUserId(
                SENIOR_ID,
                start,
                endExclusive
        )).thenReturn(samples);
        when(activityResultRepository.findAllBySeniorUserIdAndSessionIdIn(
                SENIOR_ID,
                java.util.Set.of(10L, 20L)
        )).thenReturn(List.of(
                ActivityResult.builder()
                        .activityId(2L)
                        .seniorUserId(SENIOR_ID)
                        .sessionId(10L)
                        .build()
        ));
        when(activityRepository.findAllById(java.util.Set.of(2L)))
                .thenReturn(List.of(activity(2L, ActivityType.VOICE_TALK)));
        when(interactionMessageRepository.findSessionIdsContainingSenderType(
                java.util.Set.of(10L, 20L),
                SenderType.ASSISTANT
        )).thenReturn(List.of(20L));

        var response = familyReportService.getVoiceReport(
                GUARDIAN_ID,
                startDate,
                endDate
        );

        assertEquals(SENIOR_ID, response.senior().userId());
        assertEquals(2, response.records().size());
        assertEquals(start, response.records().get(0).measuredAt());
        assertEquals("VOICE_TALK", response.records().get(0).source().name());
        assertNull(response.records().get(0).speechRate());
        assertEquals(1500L, response.records().get(0).avgPauseDurationMs());
        assertEquals(2L, response.records().get(0).utteranceCharacters());
        assertEquals("AI_ASSISTANT", response.records().get(1).source().name());
        assertEquals(new BigDecimal("3.50"), response.records().get(1).speechRate());
        assertNull(response.records().get(1).avgPauseDurationMs());
        assertEquals(5L, response.records().get(1).utteranceCharacters());
        assertEquals(
                "CHARACTERS_PER_SECOND",
                response.records().get(1).speechRateUnit()
        );
    }

    @Test
    void voiceDetailWithNoRecordsReturnsEmptyArray() {
        prepareGuardianAndSenior(GUARDIAN_ID, SENIOR_ID);
        LocalDate startDate = LocalDate.of(2026, 9, 1);
        LocalDate endDate = LocalDate.of(2026, 9, 9);
        when(voiceConditionRecordRepository.findDetailSamplesByUserId(
                SENIOR_ID,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        )).thenReturn(List.of());

        var response = familyReportService.getVoiceReport(
                GUARDIAN_ID,
                startDate,
                endDate
        );

        assertEquals(List.of(), response.records());
        verify(activityResultRepository, never())
                .findAllBySeniorUserIdAndSessionIdIn(any(), any());
        verify(interactionMessageRepository, never())
                .findSessionIdsContainingSenderType(any(), any());
    }

    @Test
    void voiceDetailRejectsNonGuardianAndMissingActiveRelationship() {
        when(userRepository.findById(GUARDIAN_ID)).thenReturn(Optional.of(
                User.builder().userId(GUARDIAN_ID).role(Role.SENIOR).build()
        ));

        ResponseStatusException forbidden = assertThrows(
                ResponseStatusException.class,
                () -> familyReportService.getVoiceReport(
                        GUARDIAN_ID,
                        WEEK_START,
                        WEEK_END
                )
        );
        assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());

        long guardianWithoutRelationship = 2L;
        when(userRepository.findById(guardianWithoutRelationship)).thenReturn(Optional.of(
                User.builder()
                        .userId(guardianWithoutRelationship)
                        .role(Role.GUARDIAN)
                        .build()
        ));
        when(guardianRelationshipRepository.findByGuardianUserIdAndStatus(
                guardianWithoutRelationship,
                GuardianRelationshipStatus.ACTIVE
        )).thenReturn(Optional.empty());

        ResponseStatusException notFound = assertThrows(
                ResponseStatusException.class,
                () -> familyReportService.getVoiceReport(
                        guardianWithoutRelationship,
                        WEEK_START,
                        WEEK_END
                )
        );
        assertEquals(HttpStatus.NOT_FOUND, notFound.getStatusCode());
    }

    @Test
    void voiceDetailRejectsReversedDateRange() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> familyReportService.getVoiceReport(
                        GUARDIAN_ID,
                        WEEK_END,
                        WEEK_START
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void summarizesDistinctCompletedDaysAndActivities() {
        List<ActivityResult> results = List.of(
                result(1L, 1L, WEEK_START, ActivityStatus.COMPLETED, null, null),
                result(2L, 2L, WEEK_START, ActivityStatus.COMPLETED, null, null),
                result(3L, 3L, WEEK_START, ActivityStatus.COMPLETED, null, 3200),
                result(4L, 1L, WEEK_START.plusDays(1), ActivityStatus.COMPLETED, 80, null),
                result(5L, 2L, WEEK_START.plusDays(2), ActivityStatus.COMPLETED, null, null),
                result(6L, 3L, WEEK_START.plusDays(3), ActivityStatus.IN_PROGRESS, null, 1000)
        );
        prepareReport(results, activities(), List.of());

        WeeklyFamilyReportResponse response = familyReportService
                .getWeeklyReport(GUARDIAN_ID, WEEK_START);

        assertEquals(3, response.weeklyActivity().completedDays());
        assertEquals(3, response.weeklyActivity().days().size());
        assertEquals(WEEK_START, response.weeklyActivity().days().get(0).date());
        assertEquals(
                List.of(
                        ActivityType.COGNITIVE_GAME,
                        ActivityType.VOICE_TALK,
                        ActivityType.WALKING
                ),
                response.weeklyActivity().days().get(0).completedActivities()
        );
    }

    @Test
    void cognitiveUsesOnlyScoresAndReturnsLatestFiveInAscendingOrder() {
        List<ActivityResult> results = List.of(
                result(1L, 1L, WEEK_START, ActivityStatus.COMPLETED, 70, null),
                result(2L, 1L, WEEK_START.plusDays(1), ActivityStatus.COMPLETED, null, null),
                result(3L, 1L, WEEK_START.plusDays(2), ActivityStatus.COMPLETED, 80, null),
                result(4L, 1L, WEEK_START.plusDays(3), ActivityStatus.COMPLETED, 90, null),
                result(5L, 1L, WEEK_START.plusDays(4), ActivityStatus.COMPLETED, 100, null),
                result(6L, 1L, WEEK_START.plusDays(5), ActivityStatus.COMPLETED, 60, null),
                result(7L, 1L, WEEK_START.plusDays(6), ActivityStatus.COMPLETED, 50, null)
        );
        prepareReport(results, activities(), List.of());

        WeeklyFamilyReportResponse response = familyReportService
                .getWeeklyReport(GUARDIAN_ID, WEEK_START);

        assertEquals(6, response.cognitive().activityCount());
        assertEquals(new BigDecimal("75.00"), response.cognitive().averageScore());
        assertEquals(5, response.cognitive().recentScores().size());
        assertEquals(
                WEEK_START.plusDays(2),
                response.cognitive().recentScores().get(0).date()
        );
        assertEquals(
                WEEK_START.plusDays(6),
                response.cognitive().recentScores().get(4).date()
        );
    }

    @Test
    void walkingExcludesMissingStepsFromStatistics() {
        List<ActivityResult> results = List.of(
                result(1L, 3L, WEEK_START, ActivityStatus.COMPLETED, null, 3200),
                result(2L, 3L, WEEK_START.plusDays(1), ActivityStatus.COMPLETED, null, null),
                result(3L, 3L, WEEK_START.plusDays(2), ActivityStatus.IN_PROGRESS, null, 2800)
        );
        prepareReport(results, activities(), List.of());

        WeeklyFamilyReportResponse response = familyReportService
                .getWeeklyReport(GUARDIAN_ID, WEEK_START);

        assertEquals(2, response.walking().recordedDays());
        assertEquals(1, response.walking().completedDays());
        assertEquals(6000L, response.walking().totalStepCount());
        assertEquals(new BigDecimal("3000.00"), response.walking().averageStepCount());
    }

    @Test
    void voiceExcludesNullMetricsAndUsesSeoulWeekBoundary() {
        List<VoiceWeeklySample> samples = List.of(
                new VoiceWeeklySample(new BigDecimal("3.00"), 1000L, "가 나"),
                new VoiceWeeklySample(null, 2000L, "abc de"),
                new VoiceWeeklySample(new BigDecimal("5.00"), null, "  ")
        );
        prepareReport(List.of(), List.of(), samples);

        WeeklyFamilyReportResponse response = familyReportService
                .getWeeklyReport(GUARDIAN_ID, WEEK_START);

        assertEquals(3, response.voice().sampleCount());
        assertEquals(new BigDecimal("4.00"), response.voice().averageSpeechRate());
        assertEquals(new BigDecimal("1500.00"), response.voice().averagePauseDurationMs());
        assertEquals(new BigDecimal("2.33"), response.voice().averageUtteranceCharacters());
        assertEquals("CHARACTERS_PER_SECOND", response.voice().speechRateUnit());

        ArgumentCaptor<LocalDateTime> startCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);
        verify(voiceConditionRecordRepository).findWeeklySamplesByUserId(
                org.mockito.ArgumentMatchers.eq(SENIOR_ID),
                startCaptor.capture(),
                endCaptor.capture()
        );
        assertEquals(WEEK_START.atStartOfDay(), startCaptor.getValue());
        assertEquals(WEEK_START.plusWeeks(1).atStartOfDay(), endCaptor.getValue());
    }

    @Test
    void emptyDataReturnsZeroCountsAndNullAverages() {
        prepareReport(List.of(), List.of(), List.of());

        WeeklyFamilyReportResponse response = familyReportService
                .getWeeklyReport(GUARDIAN_ID, WEEK_START);

        assertEquals(0, response.weeklyActivity().completedDays());
        assertEquals(List.of(), response.weeklyActivity().days());
        assertEquals(0, response.voice().sampleCount());
        assertNull(response.voice().averageSpeechRate());
        assertNull(response.voice().averagePauseDurationMs());
        assertNull(response.voice().averageUtteranceCharacters());
        assertEquals(0, response.cognitive().activityCount());
        assertNull(response.cognitive().averageScore());
        assertEquals(List.of(), response.cognitive().recentScores());
        assertEquals(0, response.walking().recordedDays());
        assertEquals(0, response.walking().completedDays());
        assertNull(response.walking().totalStepCount());
        assertNull(response.walking().averageStepCount());
    }

    @Test
    void nonGuardianIsForbidden() {
        when(userRepository.findById(GUARDIAN_ID)).thenReturn(Optional.of(
                User.builder().userId(GUARDIAN_ID).role(Role.SENIOR).build()
        ));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> familyReportService.getWeeklyReport(GUARDIAN_ID, WEEK_START)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(guardianRelationshipRepository, never())
                .findByGuardianUserIdAndStatus(any(), any());
    }

    @Test
    void guardianWithoutActiveRelationshipIsNotFound() {
        when(userRepository.findById(GUARDIAN_ID)).thenReturn(Optional.of(
                User.builder().userId(GUARDIAN_ID).role(Role.GUARDIAN).build()
        ));
        when(guardianRelationshipRepository.findByGuardianUserIdAndStatus(
                GUARDIAN_ID,
                GuardianRelationshipStatus.ACTIVE
        )).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> familyReportService.getWeeklyReport(GUARDIAN_ID, WEEK_START)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(activityResultRepository, never())
                .findAllBySeniorUserIdAndActivityDateBetween(any(), any(), any());
    }

    @Test
    void usesOnlySeniorLinkedToAuthenticatedGuardian() {
        long anotherGuardianId = 2L;
        long linkedSeniorId = 9L;
        when(userRepository.findById(anotherGuardianId)).thenReturn(Optional.of(
                User.builder().userId(anotherGuardianId).role(Role.GUARDIAN).build()
        ));
        when(guardianRelationshipRepository.findByGuardianUserIdAndStatus(
                anotherGuardianId,
                GuardianRelationshipStatus.ACTIVE
        )).thenReturn(Optional.of(
                GuardianRelationship.builder()
                        .guardianUserId(anotherGuardianId)
                        .seniorUserId(linkedSeniorId)
                        .status(GuardianRelationshipStatus.ACTIVE)
                        .build()
        ));
        when(userRepository.findById(linkedSeniorId)).thenReturn(Optional.of(
                User.builder()
                        .userId(linkedSeniorId)
                        .name("연결 시니어")
                        .role(Role.SENIOR)
                        .build()
        ));
        when(activityResultRepository
                .findAllBySeniorUserIdAndActivityDateBetween(
                        linkedSeniorId,
                        WEEK_START,
                        WEEK_END
                )).thenReturn(List.of());
        when(activityRepository.findAllById(any())).thenReturn(List.of());
        when(voiceConditionRecordRepository.findWeeklySamplesByUserId(
                linkedSeniorId,
                WEEK_START.atStartOfDay(),
                WEEK_START.plusWeeks(1).atStartOfDay()
        )).thenReturn(List.of());

        WeeklyFamilyReportResponse response = familyReportService
                .getWeeklyReport(anotherGuardianId, WEEK_START);

        assertEquals(linkedSeniorId, response.senior().userId());
        verify(activityResultRepository, never())
                .findAllBySeniorUserIdAndActivityDateBetween(
                        SENIOR_ID,
                        WEEK_START,
                        WEEK_END
                );
    }

    @Test
    void rejectsDateThatIsNotMonday() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> familyReportService.getWeeklyReport(
                        GUARDIAN_ID,
                        WEEK_START.plusDays(1)
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(userRepository, never()).findById(any());
    }

    private void prepareReport(
            List<ActivityResult> results,
            List<Activity> activityList,
            List<VoiceWeeklySample> voiceSamples
    ) {
        prepareGuardianAndSenior(GUARDIAN_ID, SENIOR_ID);
        when(activityResultRepository
                .findAllBySeniorUserIdAndActivityDateBetween(
                        SENIOR_ID,
                        WEEK_START,
                        WEEK_END
                )).thenReturn(results);
        when(activityRepository.findAllById(any())).thenReturn(activityList);
        when(voiceConditionRecordRepository.findWeeklySamplesByUserId(
                SENIOR_ID,
                WEEK_START.atStartOfDay(),
                WEEK_START.plusWeeks(1).atStartOfDay()
        )).thenReturn(voiceSamples);
    }

    private void prepareGuardianAndSenior(
            Long guardianUserId,
            Long seniorUserId
    ) {
        when(userRepository.findById(guardianUserId)).thenReturn(Optional.of(
                User.builder().userId(guardianUserId).role(Role.GUARDIAN).build()
        ));
        when(guardianRelationshipRepository.findByGuardianUserIdAndStatus(
                guardianUserId,
                GuardianRelationshipStatus.ACTIVE
        )).thenReturn(Optional.of(
                GuardianRelationship.builder()
                        .guardianUserId(guardianUserId)
                        .seniorUserId(seniorUserId)
                        .status(GuardianRelationshipStatus.ACTIVE)
                        .build()
        ));
        when(userRepository.findById(seniorUserId)).thenReturn(Optional.of(
                User.builder()
                        .userId(seniorUserId)
                        .name("홍길동")
                        .role(Role.SENIOR)
                        .build()
        ));
    }

    private List<Activity> activities() {
        return List.of(
                activity(1L, ActivityType.COGNITIVE_GAME),
                activity(2L, ActivityType.VOICE_TALK),
                activity(3L, ActivityType.WALKING)
        );
    }

    private Activity activity(Long activityId, ActivityType activityType) {
        return Activity.builder()
                .activityId(activityId)
                .activityType(activityType)
                .build();
    }

    private ActivityResult result(
            Long resultId,
            Long activityId,
            LocalDate date,
            ActivityStatus status,
            Integer score,
            Integer stepCount
    ) {
        return ActivityResult.builder()
                .activityResultId(resultId)
                .activityId(activityId)
                .seniorUserId(SENIOR_ID)
                .activityDate(date)
                .status(status)
                .score(score)
                .stepCount(stepCount)
                .build();
    }
}

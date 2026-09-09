package com.team0123.dndn.family.service;

import com.team0123.dndn.activity.entity.Activity;
import com.team0123.dndn.activity.entity.ActivityResult;
import com.team0123.dndn.activity.entity.ActivityStatus;
import com.team0123.dndn.activity.entity.ActivityType;
import com.team0123.dndn.activity.repository.ActivityRepository;
import com.team0123.dndn.activity.repository.ActivityResultRepository;
import com.team0123.dndn.family.dto.CognitiveScorePoint;
import com.team0123.dndn.family.dto.CognitiveWeeklySummary;
import com.team0123.dndn.family.dto.SeniorSummary;
import com.team0123.dndn.family.dto.VoiceRecordResponse;
import com.team0123.dndn.family.dto.VoiceRecordSource;
import com.team0123.dndn.family.dto.VoiceReportResponse;
import com.team0123.dndn.family.dto.VoiceWeeklySummary;
import com.team0123.dndn.family.dto.WalkingWeeklySummary;
import com.team0123.dndn.family.dto.WeeklyActivityDayResponse;
import com.team0123.dndn.family.dto.WeeklyActivitySummary;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FamilyReportService {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final String SPEECH_RATE_UNIT = "CHARACTERS_PER_SECOND";
    private static final int RECENT_SCORE_LIMIT = 5;
    private static final int AVERAGE_SCALE = 2;

    private final GuardianRelationshipRepository guardianRelationshipRepository;
    private final UserRepository userRepository;
    private final ActivityResultRepository activityResultRepository;
    private final ActivityRepository activityRepository;
    private final VoiceConditionRecordRepository voiceConditionRecordRepository;
    private final InteractionMessageRepository interactionMessageRepository;

    public WeeklyFamilyReportResponse getWeeklyReport(
            Long guardianUserId,
            LocalDate weekStart
    ) {
        validateWeekStart(weekStart);
        User senior = findLinkedSenior(guardianUserId);

        LocalDate weekEnd = weekStart.plusDays(6);
        List<ActivityResult> activityResults = activityResultRepository
                .findAllBySeniorUserIdAndActivityDateBetween(
                        senior.getUserId(),
                        weekStart,
                        weekEnd
                );
        Map<Long, ActivityType> activityTypes = findActivityTypes(activityResults);

        LocalDateTime voiceStart = weekStart
                .atStartOfDay(SEOUL_ZONE)
                .toLocalDateTime();
        LocalDateTime nextWeekStart = weekStart
                .plusWeeks(1)
                .atStartOfDay(SEOUL_ZONE)
                .toLocalDateTime();
        List<VoiceWeeklySample> voiceSamples = voiceConditionRecordRepository
                .findWeeklySamplesByUserId(
                        senior.getUserId(),
                        voiceStart,
                        nextWeekStart
                );

        return new WeeklyFamilyReportResponse(
                weekStart,
                weekEnd,
                new SeniorSummary(senior.getUserId(), senior.getName()),
                summarizeWeeklyActivity(activityResults, activityTypes),
                summarizeVoice(voiceSamples),
                summarizeCognitive(activityResults, activityTypes),
                summarizeWalking(activityResults, activityTypes)
        );
    }

    public VoiceReportResponse getVoiceReport(
            Long guardianUserId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        validateDateRange(startDate, endDate);
        User senior = findLinkedSenior(guardianUserId);

        LocalDateTime start = startDate
                .atStartOfDay(SEOUL_ZONE)
                .toLocalDateTime();
        LocalDateTime endExclusive;
        try {
            endExclusive = endDate
                    .plusDays(1)
                    .atStartOfDay(SEOUL_ZONE)
                    .toLocalDateTime();
        } catch (DateTimeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "조회 종료일이 올바르지 않습니다."
            );
        }

        List<VoiceDetailSample> samples = voiceConditionRecordRepository
                .findDetailSamplesByUserId(
                        senior.getUserId(),
                        start,
                        endExclusive
                );
        Set<Long> sessionIds = samples.stream()
                .map(VoiceDetailSample::sessionId)
                .collect(Collectors.toSet());
        Set<Long> voiceTalkSessionIds = findVoiceTalkSessionIds(
                senior.getUserId(),
                sessionIds
        );
        Set<Long> aiAssistantSessionIds = sessionIds.isEmpty()
                ? Set.of()
                : new HashSet<>(interactionMessageRepository
                        .findSessionIdsContainingSenderType(
                                sessionIds,
                                SenderType.ASSISTANT
                        ));

        List<VoiceRecordResponse> records = samples.stream()
                .sorted(Comparator.comparing(VoiceDetailSample::measuredAt))
                .map(sample -> new VoiceRecordResponse(
                        sample.measuredAt(),
                        resolveSource(
                                sample.sessionId(),
                                voiceTalkSessionIds,
                                aiAssistantSessionIds
                        ),
                        sample.speechRate(),
                        SPEECH_RATE_UNIT,
                        sample.avgPauseDurationMs(),
                        sample.content() == null
                                ? null
                                : countNonWhitespaceCharacters(sample.content())
                ))
                .toList();

        return new VoiceReportResponse(
                startDate,
                endDate,
                new SeniorSummary(senior.getUserId(), senior.getName()),
                records
        );
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "조회 시작일과 종료일이 올바르지 않습니다."
            );
        }
    }

    private User findLinkedSenior(Long guardianUserId) {
        requireGuardian(guardianUserId);

        GuardianRelationship relationship = guardianRelationshipRepository
                .findByGuardianUserIdAndStatus(
                        guardianUserId,
                        GuardianRelationshipStatus.ACTIVE
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "활성 가족 관계를 찾을 수 없습니다."
                ));

        return userRepository.findById(relationship.getSeniorUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "연결된 시니어를 찾을 수 없습니다."
                ));
    }

    private Set<Long> findVoiceTalkSessionIds(
            Long seniorUserId,
            Set<Long> sessionIds
    ) {
        if (sessionIds.isEmpty()) {
            return Set.of();
        }

        List<ActivityResult> sessionResults = activityResultRepository
                .findAllBySeniorUserIdAndSessionIdIn(
                        seniorUserId,
                        sessionIds
                );
        Map<Long, ActivityType> activityTypes = findActivityTypes(sessionResults);

        return sessionResults.stream()
                .filter(result -> activityTypes.get(result.getActivityId())
                        == ActivityType.VOICE_TALK)
                .map(ActivityResult::getSessionId)
                .collect(Collectors.toSet());
    }

    private VoiceRecordSource resolveSource(
            Long sessionId,
            Set<Long> voiceTalkSessionIds,
            Set<Long> aiAssistantSessionIds
    ) {
        if (voiceTalkSessionIds.contains(sessionId)) {
            return VoiceRecordSource.VOICE_TALK;
        }
        if (aiAssistantSessionIds.contains(sessionId)) {
            return VoiceRecordSource.AI_ASSISTANT;
        }
        return null;
    }

    private void validateWeekStart(LocalDate weekStart) {
        if (weekStart == null || weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "weekStart는 월요일이어야 합니다."
            );
        }
    }

    private User requireGuardian(Long guardianUserId) {
        User user = guardianUserId == null
                ? null
                : userRepository.findById(guardianUserId).orElse(null);

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "현재 로그인 사용자를 찾을 수 없습니다."
            );
        }
        if (user.getRole() != Role.GUARDIAN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "보호자만 가족 리포트를 조회할 수 있습니다."
            );
        }
        return user;
    }

    private Map<Long, ActivityType> findActivityTypes(
            List<ActivityResult> activityResults
    ) {
        Set<Long> activityIds = activityResults.stream()
                .map(ActivityResult::getActivityId)
                .collect(Collectors.toSet());

        return activityRepository.findAllById(activityIds)
                .stream()
                .collect(Collectors.toMap(
                        Activity::getActivityId,
                        Activity::getActivityType
                ));
    }

    private WeeklyActivitySummary summarizeWeeklyActivity(
            List<ActivityResult> activityResults,
            Map<Long, ActivityType> activityTypes
    ) {
        Map<LocalDate, Set<ActivityType>> activitiesByDate = new TreeMap<>();

        activityResults.stream()
                .filter(result -> result.getStatus() == ActivityStatus.COMPLETED)
                .forEach(result -> {
                    ActivityType activityType = activityTypes.get(result.getActivityId());
                    if (activityType != null) {
                        activitiesByDate
                                .computeIfAbsent(
                                        result.getActivityDate(),
                                        ignored -> EnumSet.noneOf(ActivityType.class)
                                )
                                .add(activityType);
                    }
                });

        List<WeeklyActivityDayResponse> days = activitiesByDate.entrySet()
                .stream()
                .map(entry -> new WeeklyActivityDayResponse(
                        entry.getKey(),
                        List.copyOf(entry.getValue())
                ))
                .toList();

        return new WeeklyActivitySummary(days.size(), days);
    }

    private CognitiveWeeklySummary summarizeCognitive(
            List<ActivityResult> activityResults,
            Map<Long, ActivityType> activityTypes
    ) {
        List<ActivityResult> scoredResults = activityResults.stream()
                .filter(result -> activityTypes.get(result.getActivityId())
                        == ActivityType.COGNITIVE_GAME)
                .filter(result -> result.getScore() != null)
                .toList();

        List<CognitiveScorePoint> recentScores = scoredResults.stream()
                .sorted(Comparator
                        .comparing(ActivityResult::getActivityDate)
                        .reversed()
                        .thenComparing(
                                ActivityResult::getActivityResultId,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        ))
                .limit(RECENT_SCORE_LIMIT)
                .sorted(Comparator
                        .comparing(ActivityResult::getActivityDate)
                        .thenComparing(
                                ActivityResult::getActivityResultId,
                                Comparator.nullsLast(Comparator.naturalOrder())
                        ))
                .map(result -> new CognitiveScorePoint(
                        result.getActivityDate(),
                        result.getScore()
                ))
                .toList();

        return new CognitiveWeeklySummary(
                scoredResults.size(),
                averageIntegers(scoredResults.stream()
                        .map(ActivityResult::getScore)
                        .toList()),
                recentScores
        );
    }

    private VoiceWeeklySummary summarizeVoice(
            List<VoiceWeeklySample> voiceSamples
    ) {
        List<BigDecimal> speechRates = voiceSamples.stream()
                .map(VoiceWeeklySample::speechRate)
                .filter(value -> value != null)
                .toList();
        List<Long> pauseDurations = voiceSamples.stream()
                .map(VoiceWeeklySample::avgPauseDurationMs)
                .filter(value -> value != null)
                .toList();
        List<Long> utteranceCharacters = voiceSamples.stream()
                .map(VoiceWeeklySample::content)
                .filter(value -> value != null)
                .map(this::countNonWhitespaceCharacters)
                .toList();

        return new VoiceWeeklySummary(
                voiceSamples.size(),
                averageDecimals(speechRates),
                SPEECH_RATE_UNIT,
                averageLongs(pauseDurations),
                averageLongs(utteranceCharacters)
        );
    }

    private WalkingWeeklySummary summarizeWalking(
            List<ActivityResult> activityResults,
            Map<Long, ActivityType> activityTypes
    ) {
        List<ActivityResult> walkingResults = activityResults.stream()
                .filter(result -> activityTypes.get(result.getActivityId())
                        == ActivityType.WALKING)
                .filter(result -> result.getStepCount() != null)
                .toList();

        Set<LocalDate> recordedDates = new HashSet<>();
        Set<LocalDate> completedDates = new HashSet<>();
        long totalStepCount = 0L;

        for (ActivityResult result : walkingResults) {
            recordedDates.add(result.getActivityDate());
            totalStepCount += result.getStepCount();
            if (result.getStatus() == ActivityStatus.COMPLETED) {
                completedDates.add(result.getActivityDate());
            }
        }

        if (recordedDates.isEmpty()) {
            return new WalkingWeeklySummary(0, 0, null, null);
        }

        return new WalkingWeeklySummary(
                recordedDates.size(),
                completedDates.size(),
                totalStepCount,
                divide(BigDecimal.valueOf(totalStepCount), recordedDates.size())
        );
    }

    private long countNonWhitespaceCharacters(String content) {
        return content.codePoints()
                .filter(codePoint -> !Character.isWhitespace(codePoint))
                .count();
    }

    private BigDecimal averageIntegers(List<Integer> values) {
        if (values.isEmpty()) {
            return null;
        }

        long sum = values.stream()
                .mapToLong(Integer::longValue)
                .sum();
        return divide(BigDecimal.valueOf(sum), values.size());
    }

    private BigDecimal averageLongs(List<Long> values) {
        if (values.isEmpty()) {
            return null;
        }

        BigDecimal sum = values.stream()
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return divide(sum, values.size());
    }

    private BigDecimal averageDecimals(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return null;
        }

        BigDecimal sum = values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return divide(sum, values.size());
    }

    private BigDecimal divide(BigDecimal sum, int count) {
        return sum.divide(
                BigDecimal.valueOf(count),
                AVERAGE_SCALE,
                RoundingMode.HALF_UP
        );
    }
}

package com.team0123.dndn.activity.service;

import com.team0123.dndn.activity.dto.ActivityResultResponse;
import com.team0123.dndn.activity.dto.ActivityResultSaveRequest;
import com.team0123.dndn.activity.dto.TodayActivityResponse;
import com.team0123.dndn.activity.entity.Activity;
import com.team0123.dndn.activity.entity.ActivityResult;
import com.team0123.dndn.activity.entity.ActivityStatus;
import com.team0123.dndn.activity.repository.ActivityRepository;
import com.team0123.dndn.activity.repository.ActivityResultRepository;
import com.team0123.dndn.interaction.entity.InteractionSession;
import com.team0123.dndn.interaction.repository.InteractionSessionRepository;
import com.team0123.dndn.user.entity.Role;
import com.team0123.dndn.user.entity.User;
import com.team0123.dndn.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityService {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final ActivityRepository activityRepository;
    private final ActivityResultRepository activityResultRepository;
    private final UserRepository userRepository;
    private final InteractionSessionRepository interactionSessionRepository;

    public List<TodayActivityResponse> getTodayActivities(Long userId) {
        requireSenior(userId);
        LocalDate today = LocalDate.now(SEOUL_ZONE);

        Map<Long, ActivityResult> resultsByActivityId = activityResultRepository
                .findAllBySeniorUserIdAndActivityDate(userId, today)
                .stream()
                .collect(Collectors.toMap(
                        ActivityResult::getActivityId,
                        Function.identity(),
                        (first, ignored) -> first
                ));

        return activityRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .filter(activity -> Boolean.TRUE.equals(activity.getIsActive()))
                .map(activity -> TodayActivityResponse.from(
                        activity,
                        resultsByActivityId.get(activity.getActivityId())
                ))
                .toList();
    }

    @Transactional
    public ActivityResultResponse saveResult(
            Long userId,
            Long activityId,
            ActivityResultSaveRequest request
    ) {
        requireSenior(userId);
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 챌린지입니다."
                ));

        if (!Boolean.TRUE.equals(activity.getIsActive())) {
            throw new IllegalArgumentException("비활성화된 챌린지입니다.");
        }

        ValidatedResultValues values = validateAndResolveValues(
                activity,
                userId,
                request
        );
        ZonedDateTime seoulNow = ZonedDateTime.now(SEOUL_ZONE);
        LocalDate today = seoulNow.toLocalDate();
        LocalDateTime now = seoulNow.toLocalDateTime();

        ActivityResult result = activityResultRepository
                .findBySeniorUserIdAndActivityIdAndActivityDate(
                        userId,
                        activityId,
                        today
                )
                .map(existing -> {
                    existing.update(
                            values.status(),
                            values.score(),
                            values.stepCount(),
                            values.sessionId(),
                            now
                    );
                    return existing;
                })
                .orElseGet(() -> ActivityResult.create(
                        activityId,
                        userId,
                        today,
                        values.status(),
                        values.score(),
                        values.stepCount(),
                        values.sessionId(),
                        now
                ));

        ActivityResult savedResult = activityResultRepository.save(result);
        return ActivityResultResponse.from(savedResult, activity.getActivityType());
    }

    private User requireSenior(Long userId) {
        User user = userId == null
                ? null
                : userRepository.findById(userId).orElse(null);

        if (user == null) {
            throw new IllegalArgumentException("현재 로그인 사용자를 찾을 수 없습니다.");
        }
        if (user.getRole() != Role.SENIOR) {
            throw new IllegalArgumentException("시니어 사용자만 챌린지를 수행할 수 있습니다.");
        }
        return user;
    }

    private ValidatedResultValues validateAndResolveValues(
            Activity activity,
            Long userId,
            ActivityResultSaveRequest request
    ) {
        return switch (activity.getActivityType()) {
            case COGNITIVE_GAME -> validateCognitiveGame(request);
            case WALKING -> validateWalking(activity, request);
            case VOICE_TALK -> validateVoiceTalk(userId, request);
        };
    }

    private ValidatedResultValues validateCognitiveGame(
            ActivityResultSaveRequest request
    ) {
        if (request.score() == null) {
            throw new IllegalArgumentException("인지 게임 점수가 필요합니다.");
        }
        if (request.stepCount() != null || request.sessionId() != null) {
            throw new IllegalArgumentException(
                    "인지 게임에는 걸음 수나 대화 세션을 저장할 수 없습니다."
            );
        }

        ActivityStatus status = request.status() == null
                ? ActivityStatus.COMPLETED
                : request.status();
        return new ValidatedResultValues(status, request.score(), null, null);
    }

    private ValidatedResultValues validateWalking(
            Activity activity,
            ActivityResultSaveRequest request
    ) {
        if (request.stepCount() == null) {
            throw new IllegalArgumentException("걸음 수가 필요합니다.");
        }
        if (request.score() != null || request.sessionId() != null) {
            throw new IllegalArgumentException(
                    "걷기 챌린지에는 점수나 대화 세션을 저장할 수 없습니다."
            );
        }

        ActivityStatus status = activity.getTargetValue() != null
                && request.stepCount() >= activity.getTargetValue()
                ? ActivityStatus.COMPLETED
                : ActivityStatus.IN_PROGRESS;
        return new ValidatedResultValues(status, null, request.stepCount(), null);
    }

    private ValidatedResultValues validateVoiceTalk(
            Long userId,
            ActivityResultSaveRequest request
    ) {
        if (request.score() != null || request.stepCount() != null) {
            throw new IllegalArgumentException(
                    "음성 대화에는 점수나 걸음 수를 저장할 수 없습니다."
            );
        }

        if (request.sessionId() != null) {
            InteractionSession session = interactionSessionRepository
                    .findById(request.sessionId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "존재하지 않는 대화 세션입니다."
                    ));
            if (!userId.equals(session.getUserId())) {
                throw new IllegalArgumentException(
                        "다른 사용자의 대화 세션을 연결할 수 없습니다."
                );
            }
        }

        ActivityStatus status = request.status() == null
                ? ActivityStatus.IN_PROGRESS
                : request.status();
        return new ValidatedResultValues(status, null, null, request.sessionId());
    }

    private record ValidatedResultValues(
            ActivityStatus status,
            Integer score,
            Integer stepCount,
            Long sessionId
    ) {
    }
}

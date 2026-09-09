package com.team0123.dndn.activity.service;

import com.team0123.dndn.activity.dto.ActivityResultResponse;
import com.team0123.dndn.activity.dto.ActivityResultSaveRequest;
import com.team0123.dndn.activity.dto.TodayActivityResponse;
import com.team0123.dndn.activity.entity.Activity;
import com.team0123.dndn.activity.entity.ActivityResult;
import com.team0123.dndn.activity.entity.ActivityStatus;
import com.team0123.dndn.activity.entity.ActivityType;
import com.team0123.dndn.activity.repository.ActivityRepository;
import com.team0123.dndn.activity.repository.ActivityResultRepository;
import com.team0123.dndn.interaction.entity.InteractionSession;
import com.team0123.dndn.interaction.repository.InteractionSessionRepository;
import com.team0123.dndn.user.entity.Role;
import com.team0123.dndn.user.entity.User;
import com.team0123.dndn.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private ActivityResultRepository activityResultRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InteractionSessionRepository interactionSessionRepository;

    private ActivityService activityService;

    @BeforeEach
    void setUp() {
        activityService = new ActivityService(
                activityRepository,
                activityResultRepository,
                userRepository,
                interactionSessionRepository
        );
    }

    @Test
    void todayReturnsThreeActiveActivitiesWithCurrentResults() {
        mockSenior(1L);
        LocalDate today = LocalDate.now(SEOUL_ZONE);
        List<Activity> activities = List.of(
                activity(1L, ActivityType.COGNITIVE_GAME, 1, null, true),
                activity(2L, ActivityType.VOICE_TALK, 2, null, true),
                activity(3L, ActivityType.WALKING, 3, 3000, true),
                activity(4L, ActivityType.WALKING, 4, 5000, false)
        );
        ActivityResult completedVoice = ActivityResult.builder()
                .activityResultId(20L)
                .activityId(2L)
                .seniorUserId(1L)
                .activityDate(today)
                .status(ActivityStatus.COMPLETED)
                .build();
        when(activityRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc())
                .thenReturn(activities);
        when(activityResultRepository.findAllBySeniorUserIdAndActivityDate(1L, today))
                .thenReturn(List.of(completedVoice));

        List<TodayActivityResponse> response =
                activityService.getTodayActivities(1L);

        assertEquals(3, response.size());
        assertNull(response.get(0).status());
        assertFalse(response.get(0).completed());
        assertEquals("COMPLETED", response.get(1).status());
        assertTrue(response.get(1).completed());
        assertEquals(ActivityType.WALKING, response.get(2).activityType());
    }

    @Test
    void cognitiveGameCreatesCompletedScoreForCurrentSenior() {
        mockSenior(1L);
        Activity cognitive = activity(
                1L, ActivityType.COGNITIVE_GAME, 1, null, true
        );
        when(activityRepository.findById(1L)).thenReturn(Optional.of(cognitive));
        when(activityResultRepository
                .findBySeniorUserIdAndActivityIdAndActivityDate(anyLong(), anyLong(), any()))
                .thenReturn(Optional.empty());
        when(activityResultRepository.save(any(ActivityResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        activityService.saveResult(
                1L,
                1L,
                new ActivityResultSaveRequest(80, null, null, null)
        );

        ArgumentCaptor<ActivityResult> captor =
                ArgumentCaptor.forClass(ActivityResult.class);
        verify(activityResultRepository).save(captor.capture());
        ActivityResult result = captor.getValue();
        assertEquals(1L, result.getSeniorUserId());
        assertEquals(1L, result.getActivityId());
        assertEquals(80, result.getScore());
        assertNull(result.getStepCount());
        assertNull(result.getSessionId());
        assertEquals(LocalDate.now(SEOUL_ZONE), result.getActivityDate());
        assertEquals(ActivityStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getStartedAt());
        assertNotNull(result.getCompletedAt());
    }

    @Test
    void walkingUpdatesSameResultAndCompletesAtTarget() {
        mockSenior(1L);
        Activity walking = activity(3L, ActivityType.WALKING, 3, 3000, true);
        when(activityRepository.findById(3L)).thenReturn(Optional.of(walking));

        AtomicReference<ActivityResult> stored = new AtomicReference<>();
        when(activityResultRepository
                .findBySeniorUserIdAndActivityIdAndActivityDate(anyLong(), anyLong(), any()))
                .thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        when(activityResultRepository.save(any(ActivityResult.class)))
                .thenAnswer(invocation -> {
                    ActivityResult result = invocation.getArgument(0);
                    stored.set(result);
                    return result;
                });

        ActivityResultResponse morning = activityService.saveResult(
                1L,
                3L,
                new ActivityResultSaveRequest(null, 1800, null, null)
        );
        ActivityResultResponse evening = activityService.saveResult(
                1L,
                3L,
                new ActivityResultSaveRequest(null, 3200, null, ActivityStatus.IN_PROGRESS)
        );

        assertEquals("IN_PROGRESS", morning.status());
        assertEquals(1800, morning.stepCount());
        assertNull(morning.completedAt());
        assertEquals("COMPLETED", evening.status());
        assertEquals(3200, evening.stepCount());
        assertNotNull(evening.completedAt());

        ArgumentCaptor<ActivityResult> captor =
                ArgumentCaptor.forClass(ActivityResult.class);
        verify(activityResultRepository, times(2)).save(captor.capture());
        assertSame(captor.getAllValues().get(0), captor.getAllValues().get(1));

        var firstCompletedAt = stored.get().getCompletedAt();
        activityService.saveResult(
                1L,
                3L,
                new ActivityResultSaveRequest(null, 3500, null, null)
        );
        assertEquals(firstCompletedAt, stored.get().getCompletedAt());
    }

    @Test
    void walkingKeepsLargestStepCountAndCompletedStatus() {
        mockSenior(1L);
        Activity walking = activity(3L, ActivityType.WALKING, 3, 3000, true);
        when(activityRepository.findById(3L)).thenReturn(Optional.of(walking));

        ActivityResult existing = ActivityResult.builder()
                .activityResultId(30L)
                .activityId(3L)
                .seniorUserId(1L)
                .activityDate(LocalDate.now(SEOUL_ZONE))
                .status(ActivityStatus.IN_PROGRESS)
                .stepCount(2200)
                .build();
        when(activityResultRepository
                .findBySeniorUserIdAndActivityIdAndActivityDate(anyLong(), anyLong(), any()))
                .thenReturn(Optional.of(existing));
        when(activityResultRepository.save(existing)).thenReturn(existing);

        ActivityResultResponse decreased = activityService.saveResult(
                1L,
                3L,
                new ActivityResultSaveRequest(null, 1800, null, null)
        );
        ActivityResultResponse completed = activityService.saveResult(
                1L,
                3L,
                new ActivityResultSaveRequest(null, 3000, null, null)
        );
        var completedAt = existing.getCompletedAt();
        ActivityResultResponse afterCompletion = activityService.saveResult(
                1L,
                3L,
                new ActivityResultSaveRequest(null, 2500, null, null)
        );

        assertEquals(2200, decreased.stepCount());
        assertEquals("IN_PROGRESS", decreased.status());
        assertEquals(3000, completed.stepCount());
        assertEquals("COMPLETED", completed.status());
        assertNotNull(completedAt);
        assertEquals(3000, afterCompletion.stepCount());
        assertEquals("COMPLETED", afterCompletion.status());
        assertEquals(completedAt, existing.getCompletedAt());
        verify(activityResultRepository, times(3)).save(existing);
    }

    @Test
    void walkingRejectsMissingStepCount() {
        mockSenior(1L);
        when(activityRepository.findById(3L)).thenReturn(Optional.of(
                activity(3L, ActivityType.WALKING, 3, 3000, true)
        ));

        assertThrows(
                IllegalArgumentException.class,
                () -> activityService.saveResult(
                        1L,
                        3L,
                        new ActivityResultSaveRequest(null, null, null, null)
                )
        );
        verify(activityResultRepository, never()).save(any());
    }

    @Test
    void voiceTalkConnectsOwnedSession() {
        mockSenior(1L);
        Activity voice = activity(2L, ActivityType.VOICE_TALK, 2, null, true);
        when(activityRepository.findById(2L)).thenReturn(Optional.of(voice));
        when(interactionSessionRepository.findById(10L)).thenReturn(Optional.of(
                InteractionSession.builder().sessionId(10L).userId(1L).build()
        ));
        when(activityResultRepository
                .findBySeniorUserIdAndActivityIdAndActivityDate(anyLong(), anyLong(), any()))
                .thenReturn(Optional.empty());
        when(activityResultRepository.save(any(ActivityResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ActivityResultResponse response = activityService.saveResult(
                1L,
                2L,
                new ActivityResultSaveRequest(
                        null, null, 10L, ActivityStatus.COMPLETED
                )
        );

        assertEquals(10L, response.sessionId());
        assertEquals("COMPLETED", response.status());
    }

    @Test
    void voiceTalkRejectsMissingSession() {
        mockSenior(1L);
        when(activityRepository.findById(2L)).thenReturn(Optional.of(
                activity(2L, ActivityType.VOICE_TALK, 2, null, true)
        ));
        when(interactionSessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> activityService.saveResult(
                        1L,
                        2L,
                        new ActivityResultSaveRequest(null, null, 99L, null)
                )
        );
        verify(activityResultRepository, never()).save(any());
    }

    @Test
    void voiceTalkRejectsAnotherUsersSession() {
        mockSenior(1L);
        when(activityRepository.findById(2L)).thenReturn(Optional.of(
                activity(2L, ActivityType.VOICE_TALK, 2, null, true)
        ));
        when(interactionSessionRepository.findById(10L)).thenReturn(Optional.of(
                InteractionSession.builder().sessionId(10L).userId(2L).build()
        ));

        assertThrows(
                IllegalArgumentException.class,
                () -> activityService.saveResult(
                        1L,
                        2L,
                        new ActivityResultSaveRequest(null, null, 10L, null)
                )
        );
        verify(activityResultRepository, never()).save(any());
    }

    @Test
    void missingActivityFails() {
        mockSenior(1L);
        when(activityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> activityService.saveResult(
                        1L,
                        99L,
                        new ActivityResultSaveRequest(80, null, null, null)
                )
        );
    }

    @Test
    void inactiveActivityFails() {
        mockSenior(1L);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(
                activity(1L, ActivityType.COGNITIVE_GAME, 1, null, false)
        ));

        assertThrows(
                IllegalArgumentException.class,
                () -> activityService.saveResult(
                        1L,
                        1L,
                        new ActivityResultSaveRequest(80, null, null, null)
                )
        );
    }

    @Test
    void guardianCannotSaveActivityResult() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(
                User.builder().userId(2L).role(Role.GUARDIAN).build()
        ));

        assertThrows(
                IllegalArgumentException.class,
                () -> activityService.saveResult(
                        2L,
                        1L,
                        new ActivityResultSaveRequest(80, null, null, null)
                )
        );
        verify(activityRepository, never()).findById(anyLong());
    }

    @Test
    void missingCurrentUserCannotSaveActivityResult() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> activityService.saveResult(
                        99L,
                        1L,
                        new ActivityResultSaveRequest(80, null, null, null)
                )
        );
        verify(activityRepository, never()).findById(anyLong());
    }

    @Test
    void cognitiveGameRejectsWalkingValue() {
        mockSenior(1L);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(
                activity(1L, ActivityType.COGNITIVE_GAME, 1, null, true)
        ));

        assertThrows(
                IllegalArgumentException.class,
                () -> activityService.saveResult(
                        1L,
                        1L,
                        new ActivityResultSaveRequest(80, 1000, null, null)
                )
        );
        verify(activityResultRepository, never()).save(any());
    }

    private void mockSenior(Long userId) {
        when(userRepository.findById(userId)).thenReturn(Optional.of(
                User.builder().userId(userId).role(Role.SENIOR).build()
        ));
    }

    private Activity activity(
            Long id,
            ActivityType type,
            int displayOrder,
            Integer targetValue,
            boolean active
    ) {
        return Activity.builder()
                .activityId(id)
                .activityType(type)
                .title(type.name())
                .description(type.name())
                .targetValue(targetValue)
                .displayOrder(displayOrder)
                .isActive(active)
                .build();
    }
}

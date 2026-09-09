package com.team0123.dndn.family.service;

import com.team0123.dndn.activity.entity.Activity;
import com.team0123.dndn.activity.entity.ActivityResult;
import com.team0123.dndn.activity.entity.ActivityStatus;
import com.team0123.dndn.activity.entity.ActivityType;
import com.team0123.dndn.activity.repository.ActivityRepository;
import com.team0123.dndn.activity.repository.ActivityResultRepository;
import com.team0123.dndn.family.dto.FamilyPostCreateRequest;
import com.team0123.dndn.family.dto.FamilyPostResponse;
import com.team0123.dndn.family.dto.TodayFamilyPostsResponse;
import com.team0123.dndn.family.entity.FamilyPost;
import com.team0123.dndn.family.entity.GuardianRelationship;
import com.team0123.dndn.family.entity.GuardianRelationshipStatus;
import com.team0123.dndn.family.repository.FamilyPostRepository;
import com.team0123.dndn.family.repository.GuardianRelationshipRepository;
import com.team0123.dndn.user.entity.Role;
import com.team0123.dndn.user.entity.User;
import com.team0123.dndn.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FamilyPostServiceTest {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    @Mock
    private FamilyPostRepository familyPostRepository;

    @Mock
    private GuardianRelationshipRepository guardianRelationshipRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private ActivityResultRepository activityResultRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FamilyImageStorage familyImageStorage;

    private FamilyPostService familyPostService;

    @BeforeEach
    void setUp() {
        familyPostService = new FamilyPostService(
                familyPostRepository,
                guardianRelationshipRepository,
                activityRepository,
                activityResultRepository,
                userRepository,
                familyImageStorage
        );
    }

    @Test
    void guardianCreatesPostForOwnedActiveRelationship() {
        mockUser(2L, Role.GUARDIAN);
        when(guardianRelationshipRepository.findById(10L))
                .thenReturn(Optional.of(relationship(10L, 1L, 2L)));
        when(familyImageStorage.store(any()))
                .thenReturn("/uploads/family/generated.jpg");
        when(familyPostRepository.saveAndFlush(any(FamilyPost.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        LocalDate targetDate = LocalDate.now(SEOUL_ZONE);

        FamilyPostResponse response = familyPostService.create(
                2L,
                new FamilyPostCreateRequest(
                        10L,
                        "오늘 손주랑 공원 다녀왔어요!"
                ),
                image()
        );

        ArgumentCaptor<FamilyPost> captor =
                ArgumentCaptor.forClass(FamilyPost.class);
        verify(familyPostRepository).saveAndFlush(captor.capture());
        FamilyPost saved = captor.getValue();
        assertEquals(10L, saved.getRelationshipId());
        assertEquals(targetDate, saved.getTargetDate());
        assertEquals("/uploads/family/generated.jpg", saved.getImageUrl());
        assertEquals("오늘 손주랑 공원 다녀왔어요!", saved.getMessage());
        assertEquals(saved.getImageUrl(), response.imageUrl());
        assertEquals(saved.getMessage(), response.message());
        assertEquals(10L, response.relationshipId());
    }

    @Test
    void guardianCanCreatePostWithNullMessage() {
        mockUser(2L, Role.GUARDIAN);
        when(guardianRelationshipRepository.findById(10L))
                .thenReturn(Optional.of(relationship(10L, 1L, 2L)));
        when(familyImageStorage.store(any()))
                .thenReturn("/uploads/family/generated.jpg");
        when(familyPostRepository.saveAndFlush(any(FamilyPost.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FamilyPostResponse response = familyPostService.create(
                2L,
                new FamilyPostCreateRequest(
                        10L,
                        null
                ),
                image()
        );

        ArgumentCaptor<FamilyPost> captor =
                ArgumentCaptor.forClass(FamilyPost.class);
        verify(familyPostRepository).saveAndFlush(captor.capture());
        assertNull(captor.getValue().getMessage());
        assertNull(response.message());
    }

    @Test
    void anotherGuardianCannotCreatePostForRelationship() {
        mockUser(3L, Role.GUARDIAN);
        when(guardianRelationshipRepository.findById(10L))
                .thenReturn(Optional.of(relationship(10L, 1L, 2L)));

        assertThrows(
                IllegalArgumentException.class,
                () -> familyPostService.create(3L, request(10L), image())
        );
        verify(familyImageStorage, never()).store(any());
        verify(familyPostRepository, never()).saveAndFlush(any());
    }

    @Test
    void seniorCannotCreatePost() {
        mockUser(1L, Role.SENIOR);

        assertThrows(
                IllegalArgumentException.class,
                () -> familyPostService.create(1L, request(10L), image())
        );
        verify(guardianRelationshipRepository, never()).findById(any());
        verify(familyPostRepository, never()).saveAndFlush(any());
    }

    @Test
    void missingRelationshipCannotReceivePost() {
        mockUser(2L, Role.GUARDIAN);
        when(guardianRelationshipRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> familyPostService.create(2L, request(99L), image())
        );
        verify(familyImageStorage, never()).store(any());
        verify(familyPostRepository, never()).saveAndFlush(any());
    }

    @Test
    void storageFailureDoesNotSaveDatabaseRow() {
        mockUser(2L, Role.GUARDIAN);
        when(guardianRelationshipRepository.findById(10L))
                .thenReturn(Optional.of(relationship(10L, 1L, 2L)));
        when(familyImageStorage.store(any()))
                .thenThrow(new IllegalArgumentException("저장 실패"));

        assertThrows(
                IllegalArgumentException.class,
                () -> familyPostService.create(2L, request(10L), image())
        );

        verify(familyPostRepository, never()).saveAndFlush(any());
    }

    @Test
    void databaseFailureDeletesStoredImage() {
        mockUser(2L, Role.GUARDIAN);
        when(guardianRelationshipRepository.findById(10L))
                .thenReturn(Optional.of(relationship(10L, 1L, 2L)));
        when(familyImageStorage.store(any()))
                .thenReturn("/uploads/family/generated.jpg");
        when(familyPostRepository.saveAndFlush(any(FamilyPost.class)))
                .thenThrow(new IllegalStateException("DB failure"));

        assertThrows(
                IllegalStateException.class,
                () -> familyPostService.create(2L, request(10L), image())
        );

        verify(familyImageStorage).deleteIfExists(
                "/uploads/family/generated.jpg"
        );
    }

    @Test
    void incompleteActivitiesKeepAllPostContentHidden() {
        TodayLookup fixture = prepareTodayLookup();
        LocalDate today = fixture.today();
        when(activityResultRepository.findAllBySeniorUserIdAndActivityDate(1L, today))
                .thenReturn(List.of(result(1L, ActivityStatus.COMPLETED)));

        TodayFamilyPostsResponse response = familyPostService.getToday(1L);

        assertFalse(response.unlocked());
        assertEquals(2, response.posts().size());
        response.posts().forEach(post -> {
            assertNull(post.imageUrl());
            assertNull(post.message());
        });
        fixture.posts().forEach(post -> assertNull(post.getViewedAt()));
    }

    @Test
    void allActiveActivitiesCompletedUnlockEveryPost() {
        TodayLookup fixture = prepareTodayLookup();
        LocalDate today = fixture.today();
        when(activityResultRepository.findAllBySeniorUserIdAndActivityDate(1L, today))
                .thenReturn(List.of(
                        result(1L, ActivityStatus.COMPLETED),
                        result(2L, ActivityStatus.COMPLETED),
                        result(3L, ActivityStatus.COMPLETED)
                ));

        TodayFamilyPostsResponse response = familyPostService.getToday(1L);

        assertTrue(response.unlocked());
        assertEquals(2, response.posts().size());
        assertEquals("https://example.com/1.jpg", response.posts().get(0).imageUrl());
        assertEquals("첫 번째 소식", response.posts().get(0).message());
        assertEquals("https://example.com/2.jpg", response.posts().get(1).imageUrl());
        assertEquals("두 번째 소식", response.posts().get(1).message());
        fixture.posts().forEach(post -> assertNotNull(post.getViewedAt()));
    }

    @Test
    void secondUnlockedLookupDoesNotOverwriteViewedAt() {
        TodayLookup fixture = prepareTodayLookup();
        LocalDate today = fixture.today();
        when(activityResultRepository.findAllBySeniorUserIdAndActivityDate(1L, today))
                .thenReturn(List.of(
                        result(1L, ActivityStatus.COMPLETED),
                        result(2L, ActivityStatus.COMPLETED),
                        result(3L, ActivityStatus.COMPLETED)
                ));

        familyPostService.getToday(1L);
        List<LocalDateTime> firstViewedTimes = fixture.posts().stream()
                .map(FamilyPost::getViewedAt)
                .toList();
        familyPostService.getToday(1L);

        assertEquals(
                firstViewedTimes,
                fixture.posts().stream().map(FamilyPost::getViewedAt).toList()
        );
    }

    @Test
    void inactiveCompletedActivityDoesNotAffectUnlockCalculation() {
        TodayLookup fixture = prepareTodayLookup();
        LocalDate today = fixture.today();
        when(activityResultRepository.findAllBySeniorUserIdAndActivityDate(1L, today))
                .thenReturn(List.of(
                        result(1L, ActivityStatus.COMPLETED),
                        result(2L, ActivityStatus.COMPLETED),
                        result(3L, ActivityStatus.COMPLETED),
                        result(99L, ActivityStatus.COMPLETED)
                ));

        assertTrue(familyPostService.getToday(1L).unlocked());
    }

    @Test
    void noActiveActivityNeverUnlocksPosts() {
        mockTodaySeniorAndRelationship();
        LocalDate today = LocalDate.now(SEOUL_ZONE);
        when(familyPostRepository
                .findAllByRelationshipIdAndTargetDateOrderByCreatedAtAscFamilyPostIdAsc(
                        10L, today
                ))
                .thenReturn(List.of(post(1L, today, "1")));
        when(activityRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc())
                .thenReturn(List.of());
        when(activityResultRepository.findAllBySeniorUserIdAndActivityDate(1L, today))
                .thenReturn(List.of());

        TodayFamilyPostsResponse response = familyPostService.getToday(1L);

        assertFalse(response.unlocked());
        assertNull(response.posts().get(0).imageUrl());
    }

    @Test
    void todayLookupUsesOnlyAuthenticatedSeniorsRelationshipAndSeoulDate() {
        mockTodaySeniorAndRelationship();
        LocalDate today = LocalDate.now(SEOUL_ZONE);
        when(familyPostRepository
                .findAllByRelationshipIdAndTargetDateOrderByCreatedAtAscFamilyPostIdAsc(
                        10L, today
                ))
                .thenReturn(List.of());
        when(activityRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc())
                .thenReturn(List.of());
        when(activityResultRepository.findAllBySeniorUserIdAndActivityDate(1L, today))
                .thenReturn(List.of());

        familyPostService.getToday(1L);

        verify(guardianRelationshipRepository).findBySeniorUserIdAndStatus(
                1L,
                GuardianRelationshipStatus.ACTIVE
        );
        verify(familyPostRepository)
                .findAllByRelationshipIdAndTargetDateOrderByCreatedAtAscFamilyPostIdAsc(
                        10L, today
                );
    }

    @Test
    void guardianCannotUseSeniorTodayEndpoint() {
        mockUser(2L, Role.GUARDIAN);

        assertThrows(
                IllegalArgumentException.class,
                () -> familyPostService.getToday(2L)
        );
        verify(guardianRelationshipRepository, never())
                .findBySeniorUserIdAndStatus(any(), any());
    }

    private TodayLookup prepareTodayLookup() {
        mockTodaySeniorAndRelationship();
        LocalDate today = LocalDate.now(SEOUL_ZONE);
        List<FamilyPost> posts = List.of(
                post(1L, today, "1"),
                post(2L, today, "2")
        );
        when(familyPostRepository
                .findAllByRelationshipIdAndTargetDateOrderByCreatedAtAscFamilyPostIdAsc(
                        10L, today
                ))
                .thenReturn(posts);
        when(activityRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc())
                .thenReturn(List.of(
                        activity(1L, ActivityType.COGNITIVE_GAME),
                        activity(2L, ActivityType.VOICE_TALK),
                        activity(3L, ActivityType.WALKING)
                ));
        return new TodayLookup(today, posts);
    }

    private void mockTodaySeniorAndRelationship() {
        mockUser(1L, Role.SENIOR);
        when(guardianRelationshipRepository.findBySeniorUserIdAndStatus(
                1L,
                GuardianRelationshipStatus.ACTIVE
        )).thenReturn(Optional.of(relationship(10L, 1L, 2L)));
    }

    private void mockUser(Long userId, Role role) {
        when(userRepository.findById(userId)).thenReturn(Optional.of(
                User.builder().userId(userId).role(role).build()
        ));
    }

    private GuardianRelationship relationship(
            Long relationshipId,
            Long seniorUserId,
            Long guardianUserId
    ) {
        return GuardianRelationship.builder()
                .relationshipId(relationshipId)
                .seniorUserId(seniorUserId)
                .guardianUserId(guardianUserId)
                .status(GuardianRelationshipStatus.ACTIVE)
                .build();
    }

    private FamilyPostCreateRequest request(Long relationshipId) {
        return new FamilyPostCreateRequest(
                relationshipId,
                "가족 소식"
        );
    }

    private MockMultipartFile image() {
        return new MockMultipartFile(
                "image",
                "original.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );
    }

    private FamilyPost post(Long id, LocalDate targetDate, String suffix) {
        return FamilyPost.builder()
                .familyPostId(id)
                .relationshipId(10L)
                .targetDate(targetDate)
                .imageUrl("https://example.com/" + suffix + ".jpg")
                .message(suffix.equals("1") ? "첫 번째 소식" : "두 번째 소식")
                .build();
    }

    private Activity activity(Long id, ActivityType type) {
        return Activity.builder()
                .activityId(id)
                .activityType(type)
                .title(type.name())
                .displayOrder(id.intValue())
                .isActive(true)
                .build();
    }

    private ActivityResult result(Long activityId, ActivityStatus status) {
        return ActivityResult.builder()
                .activityId(activityId)
                .seniorUserId(1L)
                .activityDate(LocalDate.now(SEOUL_ZONE))
                .status(status)
                .build();
    }

    private record TodayLookup(
            LocalDate today,
            List<FamilyPost> posts
    ) {
    }
}

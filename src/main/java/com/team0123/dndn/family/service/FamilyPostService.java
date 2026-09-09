package com.team0123.dndn.family.service;

import com.team0123.dndn.activity.entity.Activity;
import com.team0123.dndn.activity.entity.ActivityResult;
import com.team0123.dndn.activity.entity.ActivityStatus;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FamilyPostService {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final FamilyPostRepository familyPostRepository;
    private final GuardianRelationshipRepository guardianRelationshipRepository;
    private final ActivityRepository activityRepository;
    private final ActivityResultRepository activityResultRepository;
    private final UserRepository userRepository;
    private final FamilyImageStorage familyImageStorage;

    @Transactional
    public FamilyPostResponse create(
            Long userId,
            FamilyPostCreateRequest request,
            MultipartFile image
    ) {
        requireRole(userId, Role.GUARDIAN);

        GuardianRelationship relationship = guardianRelationshipRepository
                .findById(request.relationshipId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 가족 관계입니다."
                ));

        if (relationship.getStatus() != GuardianRelationshipStatus.ACTIVE) {
            throw new IllegalArgumentException("활성화된 가족 관계가 아닙니다.");
        }
        if (!userId.equals(relationship.getGuardianUserId())) {
            throw new IllegalArgumentException(
                    "다른 보호자의 가족 관계에는 소식을 등록할 수 없습니다."
            );
        }

        String imageUrl = familyImageStorage.store(image);

        try {
            FamilyPost familyPost = FamilyPost.builder()
                    .relationshipId(relationship.getRelationshipId())
                    .targetDate(LocalDate.now(SEOUL_ZONE))
                    .imageUrl(imageUrl)
                    .message(request.message())
                    .build();

            return FamilyPostResponse.from(
                    familyPostRepository.saveAndFlush(familyPost)
            );
        } catch (RuntimeException exception) {
            familyImageStorage.deleteIfExists(imageUrl);
            throw exception;
        }
    }

    @Transactional
    public TodayFamilyPostsResponse getToday(Long userId) {
        requireRole(userId, Role.SENIOR);

        GuardianRelationship relationship = guardianRelationshipRepository
                .findBySeniorUserIdAndStatus(
                        userId,
                        GuardianRelationshipStatus.ACTIVE
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "연결된 보호자가 없습니다."
                ));

        ZonedDateTime seoulNow = ZonedDateTime.now(SEOUL_ZONE);
        LocalDate today = seoulNow.toLocalDate();
        List<FamilyPost> posts = familyPostRepository
                .findAllByRelationshipIdAndTargetDateOrderByCreatedAtAscFamilyPostIdAsc(
                        relationship.getRelationshipId(),
                        today
                );

        List<Activity> activeActivities = activityRepository
                .findAllByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .filter(activity -> Boolean.TRUE.equals(activity.getIsActive()))
                .toList();

        Set<Long> completedActivityIds = activityResultRepository
                .findAllBySeniorUserIdAndActivityDate(userId, today)
                .stream()
                .filter(result -> result.getStatus() == ActivityStatus.COMPLETED)
                .map(ActivityResult::getActivityId)
                .collect(Collectors.toSet());

        boolean unlocked = !activeActivities.isEmpty()
                && activeActivities.stream()
                .map(Activity::getActivityId)
                .allMatch(completedActivityIds::contains);

        if (unlocked) {
            LocalDateTime viewedAt = seoulNow.toLocalDateTime();
            posts.forEach(post -> post.markViewed(viewedAt));
        }

        List<FamilyPostResponse> postResponses = posts.stream()
                .map(post -> FamilyPostResponse.from(post, unlocked))
                .toList();

        return new TodayFamilyPostsResponse(unlocked, postResponses);
    }

    private User requireRole(Long userId, Role role) {
        User user = userId == null
                ? null
                : userRepository.findById(userId).orElse(null);

        if (user == null) {
            throw new IllegalArgumentException(
                    "현재 로그인 사용자를 찾을 수 없습니다."
            );
        }
        if (user.getRole() != role) {
            throw new IllegalArgumentException(
                    role == Role.GUARDIAN
                            ? "보호자만 가족 소식을 등록할 수 있습니다."
                            : "시니어만 오늘의 가족 소식을 조회할 수 있습니다."
            );
        }
        return user;
    }
}

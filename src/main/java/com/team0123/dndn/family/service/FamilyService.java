package com.team0123.dndn.family.service;

import com.team0123.dndn.family.dto.FamilyRelationResponse;
import com.team0123.dndn.family.entity.ConnectionCode;
import com.team0123.dndn.family.entity.ConnectionCodeStatus;
import com.team0123.dndn.family.entity.GuardianRelationship;
import com.team0123.dndn.family.entity.GuardianRelationshipStatus;
import com.team0123.dndn.family.repository.ConnectionCodeRepository;
import com.team0123.dndn.family.repository.GuardianRelationshipRepository;
import com.team0123.dndn.user.entity.Role;
import com.team0123.dndn.user.entity.User;
import com.team0123.dndn.user.repository.UserRepository;
import com.team0123.dndn.notification.entity.NotificationType;
import com.team0123.dndn.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FamilyService {

    private final ConnectionCodeRepository connectionCodeRepository;
    private final GuardianRelationshipRepository guardianRelationshipRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 시니어가 가족 연결 코드 생성
     */
    @Transactional
    public ConnectionCode createConnectionCode(Long seniorUserId) {

        User senior = getUser(seniorUserId);

        if (senior.getRole() != Role.SENIOR) {
            throw new IllegalArgumentException("시니어만 가족 연결 코드를 생성할 수 있습니다.");
        }

        // 이미 연결된 가족이 있다면 새 코드 생성 불가
        guardianRelationshipRepository
                .findBySeniorUserIdAndStatus(
                        seniorUserId,
                        GuardianRelationshipStatus.ACTIVE
                )
                .ifPresent(relationship -> {
                    throw new IllegalArgumentException("이미 연결된 보호자가 있습니다.");
                });

        // 기존 ACTIVE 코드가 있다면 만료 처리
        connectionCodeRepository
                .findTopBySeniorUserIdAndStatusOrderByCreatedAtDesc(
                        seniorUserId,
                        ConnectionCodeStatus.ACTIVE
                )
                .ifPresent(code -> {
                    code.expire();
                });

        String code = generateConnectionCode();

        ConnectionCode connectionCode = ConnectionCode.builder()
                .seniorUserId(seniorUserId)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .status(ConnectionCodeStatus.ACTIVE)
                .build();

        return connectionCodeRepository.save(connectionCode);
    }

    /**
     * 보호자가 연결 코드로 가족 연결
     */
    @Transactional
    public GuardianRelationship connectFamily(
            Long guardianUserId,
            String code
    ) {

        User guardian = getUser(guardianUserId);

        if (guardian.getRole() != Role.GUARDIAN) {
            throw new IllegalArgumentException("보호자만 가족 연결을 할 수 있습니다.");
        }

        ConnectionCode connectionCode = connectionCodeRepository
                .findByCodeAndStatus(code, ConnectionCodeStatus.ACTIVE)
                .orElseThrow(() ->
                        new IllegalArgumentException("유효하지 않은 가족 연결 코드입니다.")
                );

        // 만료 여부 확인
        if (connectionCode.isExpired()) {
            connectionCode.expire();
            throw new IllegalArgumentException("만료된 가족 연결 코드입니다.");
        }

        Long seniorUserId = connectionCode.getSeniorUserId();

        // 자기 자신과 연결 방지
        if (seniorUserId.equals(guardianUserId)) {
            throw new IllegalArgumentException("본인과 가족 연결을 할 수 없습니다.");
        }

        // 이미 연결된 보호자가 있는지 확인
        guardianRelationshipRepository
                .findBySeniorUserIdAndStatus(
                        seniorUserId,
                        GuardianRelationshipStatus.ACTIVE
                )
                .ifPresent(relationship -> {
                    throw new IllegalArgumentException("이미 연결된 보호자가 있습니다.");
                });

        // 해당 보호자가 이미 연결되어 있는지 확인
        if (guardianRelationshipRepository
                .existsBySeniorUserIdAndGuardianUserId(
                        seniorUserId,
                        guardianUserId
                )) {

            throw new IllegalArgumentException("이미 가족 연결된 사용자입니다.");
        }

        GuardianRelationship relationship = GuardianRelationship.builder()
                .seniorUserId(seniorUserId)
                .guardianUserId(guardianUserId)
                .status(GuardianRelationshipStatus.ACTIVE)
                .build();

        GuardianRelationship savedRelationship =
                guardianRelationshipRepository.save(relationship);

        // 사용된 코드 처리
        connectionCode.use(guardianUserId);

        // 시니어에게 알림
        notificationService.createNotification(
                seniorUserId,
                NotificationType.FAMILY_CONNECTED,
                "가족 연결 완료",
                "보호자와 가족 연결이 완료되었습니다."
        );

        // 보호자에게 알림
        notificationService.createNotification(
                guardianUserId,
                NotificationType.FAMILY_CONNECTED,
                "가족 연결 완료",
                "시니어와 가족 연결이 완료되었습니다."
        );

        return savedRelationship;
    }

    /**
     * 가족 관계 조회
     */
    public GuardianRelationship getRelation(Long userId) {

        User user = getUser(userId);

        if (user.getRole() == Role.SENIOR) {

            return guardianRelationshipRepository
                    .findBySeniorUserIdAndStatus(
                            userId,
                            GuardianRelationshipStatus.ACTIVE
                    )
                    .orElseThrow(() ->
                            new IllegalArgumentException("연결된 보호자가 없습니다.")
                    );
        }

        if (user.getRole() == Role.GUARDIAN) {

            return guardianRelationshipRepository
                    .findByGuardianUserIdAndStatus(
                            userId,
                            GuardianRelationshipStatus.ACTIVE
                    )
                    .orElseThrow(() ->
                            new IllegalArgumentException("연결된 시니어가 없습니다.")
                    );
        }

        throw new IllegalArgumentException("잘못된 사용자 역할입니다.");
    }

    public FamilyRelationResponse toResponse(GuardianRelationship relationship) {
        return FamilyRelationResponse.from(
                relationship,
                getUser(relationship.getSeniorUserId()).getName(),
                getUser(relationship.getGuardianUserId()).getName()
        );
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("사용자를 찾을 수 없습니다.")
                );
    }

    /**
     * 000000 ~ 999999 형태의 6자리 숫자 코드 생성
     */
    private String generateConnectionCode() {

        String code;

        do {
            code = String.format(
                    "%06d",
                    secureRandom.nextInt(1_000_000)
            );
        } while (
                connectionCodeRepository.existsByCodeAndStatus(
                        code,
                        ConnectionCodeStatus.ACTIVE
                )
        );

        return code;
    }
}
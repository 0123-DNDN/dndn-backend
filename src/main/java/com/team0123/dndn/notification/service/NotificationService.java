package com.team0123.dndn.notification.service;

import com.team0123.dndn.user.entity.User;
import com.team0123.dndn.user.repository.UserRepository;
import com.team0123.dndn.notification.dto.NotificationResponse;
import com.team0123.dndn.notification.entity.Notification;
import com.team0123.dndn.notification.entity.NotificationType;
import com.team0123.dndn.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final PushNotificationService pushNotificationService;

    @Transactional
    public NotificationResponse createNotification(
            Long userId,
            NotificationType type,
            String title,
            String content
    ) {
        return createNotification(userId, type, title, content, null);
    }

    @Transactional
    public NotificationResponse createNotification(
            Long userId, NotificationType type, String title, String content, Long transactionId
    ) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .content(content)
                .relatedTransactionId(transactionId)
                .build();

        notificationRepository.save(notification);

        sendPushNotification(
                userId,
                title,
                content
        );

        return NotificationResponse.from(notification);
    }

    private void sendPushNotification(
            Long userId,
            String title,
            String content
    ) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "사용자를 찾을 수 없습니다."
                            )
                    );

            pushNotificationService.sendPushNotification(
                    user.getExpoPushToken(),
                    title,
                    content
            );

        } catch (Exception e) {
            log.error(
                    "Push notification failed. userId={}",
                    userId,
                    e
            );
        }
    }

    public List<NotificationResponse> getNotifications(Long userId) {
        return notificationRepository
                .findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository
                .countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(
            Long userId,
            Long notificationId
    ) {
        Notification notification =
                notificationRepository
                        .findByNotificationIdAndUserId(
                                notificationId,
                                userId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "알림을 찾을 수 없습니다."
                                )
                        );

        notification.markAsRead();

        return NotificationResponse.from(notification);
    }
}

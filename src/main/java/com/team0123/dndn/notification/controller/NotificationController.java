package com.team0123.dndn.notification.controller;

import com.team0123.dndn.notification.dto.NotificationResponse;
import com.team0123.dndn.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(
                notificationService.getNotifications(userId)
        );
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(
                notificationService.getUnreadCount(userId)
        );
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long notificationId
    ) {
        return ResponseEntity.ok(
                notificationService.markAsRead(
                        userId,
                        notificationId
                )
        );
    }
}
package com.team0123.dndn.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private static final String EXPO_PUSH_URL =
            "https://exp.host/--/api/v2/push/send";

    private final RestClient restClient = RestClient.builder()
            .baseUrl(EXPO_PUSH_URL)
            .build();

    public void sendPushNotification(
            String expoPushToken,
            String title,
            String body
    ) {
        if (expoPushToken == null || expoPushToken.isBlank()) {
            return;
        }

        Map<String, Object> request = Map.of(
                "to", expoPushToken,
                "title", title,
                "body", body,
                "sound", "default"
        );

        restClient.post()
                .header("Content-Type", "application/json")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}
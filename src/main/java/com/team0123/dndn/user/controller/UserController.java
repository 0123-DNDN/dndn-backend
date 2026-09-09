package com.team0123.dndn.user.controller;

import com.team0123.dndn.user.dto.UserResponse;
import com.team0123.dndn.user.service.UserService;
import com.team0123.dndn.auth.dto.PushTokenRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    // 내 정보 조회
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyInfo(
            @AuthenticationPrincipal Long userId
    ) {
        UserResponse response = userService.getMyInfo(userId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/push-token")
    public ResponseEntity<Void> updatePushToken(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PushTokenRequest request
    ) {
        userService.updateExpoPushToken(
                userId,
                request.getExpoPushToken()
        );

        return ResponseEntity.ok().build();
    }
}
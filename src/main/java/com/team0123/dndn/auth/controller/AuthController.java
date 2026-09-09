package com.team0123.dndn.auth.controller;

import com.team0123.dndn.user.dto.UserLoginRequest;
import com.team0123.dndn.user.dto.UserLoginResponse;
import com.team0123.dndn.user.dto.UserSignupRequest;
import com.team0123.dndn.user.service.UserService;
import com.team0123.dndn.auth.dto.PhoneVerificationSendRequest;
import com.team0123.dndn.auth.dto.PhoneVerificationVerifyRequest;
import com.team0123.dndn.auth.service.PhoneVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final PhoneVerificationService phoneVerificationService;

    // 전화번호 인증번호 발송
    @PostMapping("/phone/send-code")
    public ResponseEntity<String> sendVerificationCode(
            @Valid @RequestBody PhoneVerificationSendRequest request
    ) {
        String code =
                phoneVerificationService.sendCode(request.getPhone());

        return ResponseEntity.ok(code);
    }

    // 전화번호 인증번호 확인
    @PostMapping("/phone/verify")
    public ResponseEntity<Void> verifyVerificationCode(
            @Valid @RequestBody PhoneVerificationVerifyRequest request
    ) {
        phoneVerificationService.verifyCode(
                request.getPhone(),
                request.getCode()
        );

        return ResponseEntity.ok().build();
    }

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<Void> signup(
            @Valid @RequestBody UserSignupRequest request
    ) {
        userService.signup(request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<UserLoginResponse> login(
            @Valid @RequestBody UserLoginRequest request
    ) {
        UserLoginResponse response = userService.login(request);

        return ResponseEntity.ok(response);
    }
}
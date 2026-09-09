package com.team0123.dndn.user.service;

import com.team0123.dndn.auth.JwtTokenProvider;
import com.team0123.dndn.auth.service.PhoneVerificationService;
import com.team0123.dndn.user.dto.UserLoginRequest;
import com.team0123.dndn.user.dto.UserLoginResponse;
import com.team0123.dndn.user.dto.UserResponse;
import com.team0123.dndn.user.dto.UserSignupRequest;
import com.team0123.dndn.user.entity.User;
import com.team0123.dndn.user.entity.UserStatus;
import com.team0123.dndn.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PhoneVerificationService phoneVerificationService;

    // 회원가입
    @Transactional
    public void signup(UserSignupRequest request) {

        // 1. 전화번호 인증 여부 확인
        if (!phoneVerificationService.isVerified(request.getPhone())) {
            throw new IllegalArgumentException(
                    "전화번호 인증이 필요합니다."
            );
        }

        // 2. 전화번호 중복 확인
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("이미 가입된 전화번호입니다.");
        }

        // 3. 비밀번호 암호화
        String encodedPassword =
                passwordEncoder.encode(request.getPassword());

        // 4. User 엔티티 생성
        User user = User.builder()
                .name(request.getName())
                .password(encodedPassword)
                .role(request.getRole())
                .phone(request.getPhone())
                .birthDate(request.getBirthDate())
                .build();

        // 5. 회원 저장
        userRepository.save(user);
    }

    // 로그인
    @Transactional(readOnly = true)
    public UserLoginResponse login(UserLoginRequest request) {

        // 1. 전화번호로 사용자 조회
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new IllegalArgumentException("전화번호 또는 비밀번호가 올바르지 않습니다."));

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("전화번호 또는 비밀번호가 올바르지 않습니다.");
        }

        // 3. 사용자 상태 확인
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("사용할 수 없는 계정입니다.");
        }

        // 4. JWT 발급
        String accessToken = jwtTokenProvider.createAccessToken(user);

        // 5. 응답
        return UserLoginResponse.builder()
                .accessToken(accessToken)
                .userId(user.getUserId())
                .name(user.getName())
                .role(user.getRole())
                .build();
    }

    // 내 정보 조회
    public UserResponse getMyInfo(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 사용자입니다.")
                );

        return UserResponse.from(user);
    }
}
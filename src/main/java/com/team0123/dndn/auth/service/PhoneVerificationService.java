package com.team0123.dndn.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PhoneVerificationService {

    private final Map<String, String> verificationCodes =
            new ConcurrentHashMap<>();

    private final Map<String, Boolean> verifiedPhones =
            new ConcurrentHashMap<>();

    /**
     * 전화번호 인증번호를 생성합니다.
     *
     * 현재는 실제 SMS를 보내지 않고
     * 생성된 인증번호를 반환하는 Mock 방식입니다.
     */
    public String sendCode(String phone) {

        String code = String.format(
                "%06d",
                (int) (Math.random() * 1_000_000)
        );

        verificationCodes.put(phone, code);
        verifiedPhones.remove(phone);

        return code;
    }

    /**
     * 인증번호를 확인합니다.
     */
    public void verifyCode(String phone, String code) {

        String savedCode = verificationCodes.get(phone);

        if (savedCode == null) {
            throw new IllegalArgumentException(
                    "인증번호를 먼저 요청해주세요."
            );
        }

        if (!savedCode.equals(code)) {
            throw new IllegalArgumentException(
                    "인증번호가 올바르지 않습니다."
            );
        }

        verifiedPhones.put(phone, true);
        verificationCodes.remove(phone);
    }

    /**
     * 전화번호 인증 완료 여부를 확인합니다.
     */
    public boolean isVerified(String phone) {
        return Boolean.TRUE.equals(
                verifiedPhones.get(phone)
        );
    }
}
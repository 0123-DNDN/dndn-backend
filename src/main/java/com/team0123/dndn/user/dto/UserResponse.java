package com.team0123.dndn.user.dto;

import com.team0123.dndn.user.entity.Role;
import com.team0123.dndn.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class UserResponse {

    private Long userId;
    private String name;
    private Role role;
    private String phone;
    private LocalDate birthDate;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .role(user.getRole())
                .phone(user.getPhone())
                .birthDate(user.getBirthDate())
                .build();
    }
}
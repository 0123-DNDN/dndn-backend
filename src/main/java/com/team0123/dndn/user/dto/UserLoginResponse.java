package com.team0123.dndn.user.dto;

import com.team0123.dndn.user.entity.Role;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserLoginResponse {

    private String accessToken;
    private Long userId;
    private String name;
    private Role role;
}
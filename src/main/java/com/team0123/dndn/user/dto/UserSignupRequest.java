package com.team0123.dndn.user.dto;

import com.team0123.dndn.user.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class UserSignupRequest {

    @NotBlank
    @Size(max = 50)
    private String name;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    @NotNull
    private Role role;

    @NotBlank
    @Pattern(regexp = "^01[0-9]{8,9}$")
    private String phone;

    @NotNull
    @Past
    private LocalDate birthDate;
}
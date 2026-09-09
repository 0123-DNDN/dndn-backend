package com.team0123.dndn.family.controller;

import com.team0123.dndn.family.dto.ConnectionCodeResponse;
import com.team0123.dndn.family.dto.FamilyConnectRequest;
import com.team0123.dndn.family.dto.FamilyRelationResponse;
import com.team0123.dndn.family.entity.ConnectionCode;
import com.team0123.dndn.family.entity.GuardianRelationship;
import com.team0123.dndn.family.service.FamilyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/family")
@RequiredArgsConstructor
public class FamilyController {

    private final FamilyService familyService;

    /**
     * 시니어 - 가족 연결 코드 생성
     */
    @PostMapping("/code")
    public ResponseEntity<ConnectionCodeResponse> createConnectionCode(
            @AuthenticationPrincipal Long userId
    ) {
        ConnectionCode connectionCode =
                familyService.createConnectionCode(userId);

        return ResponseEntity.ok(
                ConnectionCodeResponse.from(connectionCode)
        );
    }

    /**
     * 보호자 - 가족 연결 코드 입력
     */
    @PostMapping("/connect")
    public ResponseEntity<FamilyRelationResponse> connectFamily(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody FamilyConnectRequest request
    ) {
        GuardianRelationship relationship =
                familyService.connectFamily(
                        userId,
                        request.getCode()
                );

        return ResponseEntity.ok(
                FamilyRelationResponse.from(relationship)
        );
    }

    /**
     * 가족 관계 조회
     */
    @GetMapping("/relation")
    public ResponseEntity<FamilyRelationResponse> getRelation(
            @AuthenticationPrincipal Long userId
    ) {
        GuardianRelationship relationship =
                familyService.getRelation(userId);

        return ResponseEntity.ok(
                FamilyRelationResponse.from(relationship)
        );
    }
}
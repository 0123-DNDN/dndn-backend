package com.team0123.dndn.family.controller;

import com.team0123.dndn.family.dto.FamilyPostCreateRequest;
import com.team0123.dndn.family.dto.FamilyPostResponse;
import com.team0123.dndn.family.dto.TodayFamilyPostsResponse;
import com.team0123.dndn.family.service.FamilyPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/family-posts")
public class FamilyPostController {

    private final FamilyPostService familyPostService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FamilyPostResponse> create(
            @AuthenticationPrincipal Long userId,
            @Valid @ModelAttribute FamilyPostCreateRequest request,
            @RequestPart("image") MultipartFile image
    ) {
        return ResponseEntity.ok(
                familyPostService.create(userId, request, image)
        );
    }

    @GetMapping("/today")
    public ResponseEntity<TodayFamilyPostsResponse> getToday(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(familyPostService.getToday(userId));
    }
}

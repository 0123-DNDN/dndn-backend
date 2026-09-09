package com.team0123.dndn.activity.controller;

import com.team0123.dndn.activity.dto.ActivityResultResponse;
import com.team0123.dndn.activity.dto.ActivityResultSaveRequest;
import com.team0123.dndn.activity.dto.TodayActivityResponse;
import com.team0123.dndn.activity.service.ActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping("/guardian/today")
    public ResponseEntity<List<TodayActivityResponse>> getGuardianTodayActivities(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(activityService.getGuardianTodayActivities(userId));
    }

    @GetMapping("/today")
    public ResponseEntity<List<TodayActivityResponse>> getTodayActivities(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(activityService.getTodayActivities(userId));
    }

    @PostMapping("/{activityId}/results")
    public ResponseEntity<ActivityResultResponse> saveResult(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long activityId,
            @Valid @RequestBody ActivityResultSaveRequest request
    ) {
        return ResponseEntity.ok(
                activityService.saveResult(userId, activityId, request)
        );
    }
}

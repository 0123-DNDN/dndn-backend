package com.team0123.dndn.fds.controller;

import com.team0123.dndn.fds.dto.FdsAnalyzeRequest;
import com.team0123.dndn.fds.dto.FdsAnalyzeResponse;
import com.team0123.dndn.fds.service.FdsAnalyzeService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 최종 FDS 위험 분석 요청을 처리하는 Controller입니다.
 * Controller는 요청과 응답 전달만 담당합니다.
 * 점수 계산과 위험 등급 판단은 FdsAnalyzeService가 수행합니다.
 */
@RestController
@RequestMapping("/api/fds")
public class FdsAnalyzeController {

    private final FdsAnalyzeService fdsAnalyzeService;

    /**
     * 생성자 주입을 통해 FdsAnalyzeService를 전달받습니다.
     */
    public FdsAnalyzeController(
            FdsAnalyzeService fdsAnalyzeService
    ) {
        this.fdsAnalyzeService = fdsAnalyzeService;
    }

    /**
     * 거래·수취인·기기·행동·Context 정보를 종합하여
     * 최종 FDS 위험 등급과 권장 대응을 반환합니다.
     * POST /api/fds/analyze
     */
    @PostMapping(
            value = "/analyze",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public ResponseEntity<FdsAnalyzeResponse> analyze(
            @Valid @RequestBody FdsAnalyzeRequest request
    ) {
        // 실제 분석은 Service에 위임합니다.
        FdsAnalyzeResponse response =
                fdsAnalyzeService.analyze(request);

        // 분석 결과를 HTTP 200 OK로 반환합니다.
        return ResponseEntity.ok(response);
    }
}
package com.team0123.dndn.transfer.dto;

import com.team0123.dndn.fds.dto.FdsAnalyzeResponse;
import com.team0123.dndn.fds.type.RecommendedAction;
import com.team0123.dndn.fds.type.RiskLevel;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FdsResultResponse {

    private RecommendedAction recommendedAction;

    private RiskLevel riskLevel;

    private int riskScore;

    private List<String> reasons;

    public static FdsResultResponse from(FdsAnalyzeResponse response) {
        return FdsResultResponse.builder()
                .recommendedAction(response.recommendedAction())
                .riskLevel(response.riskLevel())
                .riskScore(response.riskScore())
                .reasons(response.reasons())
                .build();
    }
}
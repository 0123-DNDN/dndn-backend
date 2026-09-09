package com.team0123.dndn.transfer.dto;

import com.team0123.dndn.ai.dto.FollowUpAnswer;
import com.team0123.dndn.ai.type.ContextRiskSignal;
import jakarta.validation.Valid;

import java.util.List;

public record FdsCheckRequest(
        List<ContextRiskSignal> detectedSignals,
        @Valid List<FollowUpAnswer> followUpAnswers
) {
    public FdsCheckRequest {
        detectedSignals = detectedSignals == null
                ? List.of()
                : List.copyOf(detectedSignals);
        followUpAnswers = followUpAnswers == null
                ? List.of()
                : List.copyOf(followUpAnswers);
    }

    public static FdsCheckRequest empty() {
        return new FdsCheckRequest(List.of(), List.of());
    }
}

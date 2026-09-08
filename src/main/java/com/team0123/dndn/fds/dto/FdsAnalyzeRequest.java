package com.team0123.dndn.fds.dto;

import com.team0123.dndn.fds.dto.FdsRiskScoreRequest.BehaviorRiskInput;
import com.team0123.dndn.fds.dto.FdsRiskScoreRequest.ConditionRiskInput;
import com.team0123.dndn.fds.dto.FdsRiskScoreRequest.DeviceRiskInput;
import com.team0123.dndn.fds.dto.FdsRiskScoreRequest.RecipientRiskInput;
import com.team0123.dndn.fds.dto.FdsRiskScoreRequest.TransactionRiskInput;
import com.team0123.dndn.fds.dto.FdsRiskScoreRequest.VelocityRiskInput;
import jakarta.validation.constraints.Size;

/**
 * POST /api/fds/analyze 요청 DTO입니다.
 * A와 C가 수집한 정보를 전달하며,
 * B가 DB를 직접 조회하거나 거래 상태를 변경하지 않습니다.
 */
public record FdsAnalyzeRequest(

        /*
         * 사용자가 설명한 송금 목적입니다.
         *
         * 빈 문자열이면 B-2에서
         * UNCLEAR_TRANSFER_PURPOSE로 처리할 수 있습니다.
         */
        @Size(
                max = 500,
                message = "송금 목적은 500자 이하로 입력해 주세요."
        )
        String purposeText,

        // A가 전달하는 거래 자체 위험 정보
        TransactionRiskInput transaction,

        // A가 전달하는 수취인 위험 정보
        RecipientRiskInput recipient,

        // A가 전달하는 거래 빈도 정보
        VelocityRiskInput velocity,

        // A 또는 앱이 전달하는 접속 환경 정보
        DeviceRiskInput device,

        // 앱에서 수집한 송금 과정의 행동 정보
        BehaviorRiskInput behavior,

        // C가 전달하는 음성 또는 채팅 상태 정보
        ConditionRiskInput condition

) {
}
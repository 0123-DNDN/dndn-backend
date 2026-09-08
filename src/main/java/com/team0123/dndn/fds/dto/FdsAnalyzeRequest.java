package com.team0123.dndn.fds.dto;

import com.team0123.dndn.ai.dto.FollowUpAnswer;
import com.team0123.dndn.fds.dto.FdsRiskScoreRequest.BehaviorRiskInput;
import com.team0123.dndn.fds.dto.FdsRiskScoreRequest.ConditionRiskInput;
import com.team0123.dndn.fds.dto.FdsRiskScoreRequest.DeviceRiskInput;
import com.team0123.dndn.fds.dto.FdsRiskScoreRequest.RecipientRiskInput;
import com.team0123.dndn.fds.dto.FdsRiskScoreRequest.TransactionRiskInput;
import com.team0123.dndn.fds.dto.FdsRiskScoreRequest.VelocityRiskInput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * POST /api/fds/analyze 요청 DTO입니다.
 * A와 C가 수집한 정보를 전달하며,
 * B가 DB를 직접 조회하거나 거래 상태를 변경하지 않습니다.
 */
public record FdsAnalyzeRequest(

        /*
         * 사용자가 설명한 최초 송금 목적입니다.
         *
         * 예:
         * "김 검사가 보내래."
         *
         * 빈 문자열이면 B-2에서
         * UNCLEAR_TRANSFER_PURPOSE로 처리할 수 있습니다.
         */
        @Size(
                max = 500,
                message = "송금 목적은 500자 이하로 입력해 주세요."
        )
        String purposeText,

        /*
         * 추가 확인 질문에 대한 사용자의 답변입니다.
         *
         * 프론트에서 예/아니오 답변을 받은 뒤
         * A가 최종 FDS 요청에 포함하여 전달합니다.
         *
         * 추가 질문이 없으면 빈 배열을 전달하거나
         * 필드 자체를 생략할 수 있습니다.
         */
        @Valid
        List<FollowUpAnswer> followUpAnswers,

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

        /**
         * followUpAnswers가 생략되거나 null로 전달돼도
         * Service에서는 항상 빈 List로 안전하게 처리할 수 있게 합니다.
         */
        public FdsAnalyzeRequest {
                followUpAnswers =
                        followUpAnswers == null
                                ? List.of()
                                : List.copyOf(followUpAnswers);
        }

        /**
         * 기존 코드와 테스트가 바로 깨지지 않도록 제공하는 생성자입니다.
         *
         * 추가 답변이 없는 기존 요청은
         * followUpAnswers를 빈 배열로 처리합니다.
         */
        public FdsAnalyzeRequest(
                String purposeText,
                TransactionRiskInput transaction,
                RecipientRiskInput recipient,
                VelocityRiskInput velocity,
                DeviceRiskInput device,
                BehaviorRiskInput behavior,
                ConditionRiskInput condition
        ) {
                this(
                        purposeText,
                        List.of(),
                        transaction,
                        recipient,
                        velocity,
                        device,
                        behavior,
                        condition
                );
        }
}
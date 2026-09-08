package com.team0123.dndn.ai.dto;

import com.team0123.dndn.ai.type.FinancialIntent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 금융 발화 Intent 분석 API의 요청 DTO입니다.
 * 일반 AI 비서에서는 intentHint를 생략하고,
 * 특정 금융 기능에서 진입한 경우에는 해당 기능을 힌트로 전달합니다.
 */
public record IntentAnalyzeRequest(

        // null, 빈 문자열, 공백만 있는 문자열을 허용하지 않습니다.
        @NotBlank(message = "분석할 문장을 입력해 주세요.")

        // 지나치게 긴 입력으로 인한 비용 및 오용을 제한합니다.
        @Size(
                max = 500,
                message = "문장은 500자 이하로 입력해 주세요."
        )
        String text,

        /*
         * 사용자가 어떤 기능 화면에서 진입했는지 나타내는 선택값입니다.
         *
         * 예:
         * 홈의 돈 보내기 버튼
         * → TRANSFER
         *
         * 일반 AI 비서
         * → null 또는 필드 생략
         *
         * 이 값은 실제 금융 기능을 실행하는 권한이 아니라
         * AI의 분석 범위를 제한하기 위한 힌트입니다.
         */
        FinancialIntent intentHint

) {
        /**
         * 기존 코드 및 테스트와의 호환성을 위한 생성자입니다.
         * intentHint 없이 생성하면 일반 AI 비서 요청으로 처리합니다.
         */
        public IntentAnalyzeRequest(String text) {
                this(text, null);
        }
}
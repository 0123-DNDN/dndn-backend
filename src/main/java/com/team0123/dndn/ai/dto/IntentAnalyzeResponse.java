package com.team0123.dndn.ai.dto;

import com.team0123.dndn.ai.type.FinancialIntent;

/**
 * 금융 발화 Intent 분석 API의 최종 응답 DTO입니다.
 * 프론트엔드는 이 결과를 사용해
 * 잔액 조회, 송금, 거래내역 조회 화면으로 이동할 수 있습니다.
 */
public record IntentAnalyzeResponse(
        // 서버에서 검증된 금융 Intent
        FinancialIntent intent,

        // 사용자가 말한 수취인 이름 또는 별칭
        // 송금 요청이 아니거나 확인되지 않았다면 null
        String recipientKeyword,

        // 원 단위 송금액
        // 송금 요청이 아니거나 확인되지 않았다면 null
        Long amount
) {

    /**
     * Gemini 호출 또는 분석에 실패했을 때 사용할 기본 응답입니다.
     * 서버 오류로 종료하는 대신 UNKNOWN을 반환하여
     * 프론트에서 "다시 말씀해 주세요"라고 안내할 수 있게 합니다.
     */
    public static IntentAnalyzeResponse unknown() {
        return new IntentAnalyzeResponse(
                FinancialIntent.UNKNOWN,
                null,
                null
        );
    }
}
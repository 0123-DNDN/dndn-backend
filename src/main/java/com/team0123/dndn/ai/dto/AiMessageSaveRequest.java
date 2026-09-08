package com.team0123.dndn.ai.dto;

import com.team0123.dndn.interaction.entity.InputType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * AI 비서의 사용자 메시지와 측정 정보를
 * 한 번에 저장하기 위한 요청 DTO입니다.
 * 메시지·행동·음성 상태를 하나의 트랜잭션으로 저장하여
 * 일부 데이터만 저장되는 상황을 방지합니다.
 */
public record AiMessageSaveRequest(

        // 사용자의 입력 방식: CHAT 또는 VOICE
        @NotNull(message = "입력 방식은 필수입니다.")
        InputType inputType,

        /*
         * CHAT이면 사용자가 입력한 원문,
         * VOICE이면 STT로 변환된 텍스트입니다.
         */
        @NotBlank(message = "메시지 내용을 입력해 주세요.")
        @Size(
                max = 2000,
                message = "메시지는 2000자 이하로 입력해 주세요."
        )
        String content,

        /*
         * 프론트에서 측정한 사용자 행동 정보입니다.
         *
         * 측정값이 없는 경우 null로 전달할 수 있습니다.
         */
        @Valid
        AiBehaviorRequest behavior,

        /*
         * 음성 상태 정보입니다.
         *
         * VOICE 입력이면 필수이고,
         * CHAT 입력이면 null이어야 합니다.
         *
         * 이 조건은 DTO annotation만으로 처리하기 복잡하므로
         * Service에서 입력 방식에 따라 검증합니다.
         */
        @Valid
        AiVoiceConditionRequest voiceCondition

) {
}
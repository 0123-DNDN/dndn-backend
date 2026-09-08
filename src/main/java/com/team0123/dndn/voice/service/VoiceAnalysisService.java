package com.team0123.dndn.voice.service;

import com.team0123.dndn.interaction.entity.InputType;
import com.team0123.dndn.interaction.entity.InteractionBehavior;
import com.team0123.dndn.interaction.entity.InteractionMessage;
import com.team0123.dndn.interaction.entity.InteractionSession;
import com.team0123.dndn.interaction.entity.SenderType;
import com.team0123.dndn.interaction.repository.InteractionBehaviorRepository;
import com.team0123.dndn.interaction.repository.InteractionMessageRepository;
import com.team0123.dndn.interaction.repository.InteractionSessionRepository;
import com.team0123.dndn.user.repository.UserRepository;
import com.team0123.dndn.voice.dto.VoiceAnalysisSaveRequest;
import com.team0123.dndn.voice.dto.VoiceAnalysisSaveResponse;
import com.team0123.dndn.voice.entity.VoiceConditionRecord;
import com.team0123.dndn.voice.repository.VoiceConditionRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoiceAnalysisService {

    private final InteractionSessionRepository interactionSessionRepository;
    private final InteractionMessageRepository interactionMessageRepository;
    private final VoiceConditionRecordRepository voiceConditionRecordRepository;
    private final InteractionBehaviorRepository interactionBehaviorRepository;
    private final UserRepository userRepository;

    @Transactional
    public VoiceAnalysisSaveResponse save(
            Long userId,
            VoiceAnalysisSaveRequest request
    ) {
        if (userId == null || !userRepository.existsById(userId)) {
            throw new IllegalArgumentException("현재 로그인 사용자를 찾을 수 없습니다.");
        }

        // TODO: MVP 임시 처리. 세션 관리 기능 도입 시 활성 세션을 조회해 재사용한다.
        InteractionSession session = interactionSessionRepository.save(
                InteractionSession.builder()
                        .userId(userId)
                        .build()
        );

        InteractionMessage message = interactionMessageRepository.save(
                InteractionMessage.builder()
                        .sessionId(session.getSessionId())
                        .senderType(SenderType.USER)
                        .inputType(InputType.VOICE)
                        .content(request.text())
                        .build()
        );

        voiceConditionRecordRepository.save(
                VoiceConditionRecord.builder()
                        .messageId(message.getMessageId())
                        .speechDurationMs(secondsToMilliseconds(
                                request.recordingDuration()
                        ))
                        .speechRate(toBigDecimal(request.speechRate()))
                        .build()
        );

        interactionBehaviorRepository.save(
                InteractionBehavior.builder()
                        .sessionId(session.getSessionId())
                        .messageId(message.getMessageId())
                        .responseDelayMs(secondsToMilliseconds(
                                request.responseTime()
                        ))
                        .build()
        );

        return VoiceAnalysisSaveResponse.from(message);
    }

    private Long secondsToMilliseconds(Double seconds) {
        if (seconds == null) {
            return null;
        }

        try {
            return BigDecimal.valueOf(seconds)
                    .movePointRight(3)
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            throw new IllegalArgumentException("시간 값이 저장 가능한 범위를 벗어났습니다.");
        }
    }

    private BigDecimal toBigDecimal(Double value) {
        if (value == null) {
            return null;
        }

        try {
            return BigDecimal.valueOf(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("발화 속도 값이 올바르지 않습니다.");
        }
    }
}

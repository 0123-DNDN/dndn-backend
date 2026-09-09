package com.team0123.dndn.ai.service;

import com.team0123.dndn.ai.dto.AiBehaviorRequest;
import com.team0123.dndn.ai.dto.AiMessageSaveRequest;
import com.team0123.dndn.ai.dto.AiMessageSaveResponse;
import com.team0123.dndn.ai.dto.AiSessionResponse;
import com.team0123.dndn.ai.dto.AiVoiceConditionRequest;
import com.team0123.dndn.interaction.entity.InputType;
import com.team0123.dndn.interaction.entity.InteractionBehavior;
import com.team0123.dndn.interaction.entity.InteractionMessage;
import com.team0123.dndn.interaction.entity.InteractionSession;
import com.team0123.dndn.interaction.entity.SenderType;
import com.team0123.dndn.interaction.repository.InteractionBehaviorRepository;
import com.team0123.dndn.interaction.repository.InteractionMessageRepository;
import com.team0123.dndn.interaction.repository.InteractionSessionRepository;
import com.team0123.dndn.user.repository.UserRepository;
import com.team0123.dndn.voice.entity.VoiceConditionRecord;
import com.team0123.dndn.voice.repository.VoiceConditionRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.team0123.dndn.ai.dto.AiAssistantMessageSaveRequest;

/**
 * AI 비서 대화 세션과 메시지 기록을 관리합니다.
 *
 * 메시지, 행동 기록, 음성 상태 기록은 하나의 트랜잭션에서
 * 함께 저장하여 일부 데이터만 저장되는 상황을 방지합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiConversationService {

    private final InteractionSessionRepository
            interactionSessionRepository;

    private final InteractionMessageRepository
            interactionMessageRepository;

    private final InteractionBehaviorRepository
            interactionBehaviorRepository;

    private final VoiceConditionRecordRepository
            voiceConditionRecordRepository;

    private final UserRepository userRepository;

    /**
     * 로그인 사용자의 새로운 AI 비서 대화 세션을 생성합니다.
     */
    @Transactional
    public AiSessionResponse createSession(
            Long userId
    ) {
        validateUser(userId);

        InteractionSession session =
                InteractionSession.builder()
                        .userId(userId)
                        .build();

        InteractionSession savedSession =
                interactionSessionRepository.save(session);

        return AiSessionResponse.from(savedSession);
    }

    /**
     * 로그인 사용자의 AI 비서 대화 세션을 종료합니다.
     */
    @Transactional
    public AiSessionResponse endSession(
            Long userId,
            Long sessionId
    ) {
        validateUser(userId);

        InteractionSession session =
                findOwnedSession(userId, sessionId);

        /*
         * 이미 종료된 세션이라면 최초 종료 시각을 유지합니다.
         */
        session.end();

        return AiSessionResponse.from(session);
    }

    /**
     * 사용자 메시지와 측정 정보를 저장합니다.
     *
     * 저장 순서:
     * 1. 세션 및 소유권 확인
     * 2. 사용자 메시지 저장
     * 3. 행동 기록 저장
     * 4. VOICE이면 음성 상태 저장
     */
    @Transactional
    public AiMessageSaveResponse saveUserMessage(
            Long userId,
            Long sessionId,
            AiMessageSaveRequest request
    ) {
        validateUser(userId);

        if (request == null) {
            throw new IllegalArgumentException(
                    "메시지 저장 요청은 필수입니다."
            );
        }

        InteractionSession session =
                findOwnedSession(userId, sessionId);

        /*
         * 종료된 세션에는 새로운 메시지를 추가할 수 없습니다.
         */
        if (!session.isActive()) {
            throw new IllegalArgumentException(
                    "이미 종료된 대화 세션입니다."
            );
        }

        validateMessageRequest(request);

        /*
         * CHAT이면 사용자가 작성한 텍스트,
         * VOICE이면 STT 변환 결과를 저장합니다.
         */
        InteractionMessage message =
                interactionMessageRepository.save(
                        InteractionMessage.builder()
                                .sessionId(sessionId)
                                .senderType(SenderType.USER)
                                .inputType(request.inputType())
                                .content(request.content().trim())
                                .build()
                );

        /*
         * 행동 정보가 전달되지 않은 경우에도
         * 메시지별 행동 기록을 0 또는 null 값으로 남깁니다.
         */
        saveBehavior(
                sessionId,
                message.getMessageId(),
                request.behavior()
        );

        /*
         * VOICE 입력에만 음성 상태 기록을 저장합니다.
         */
        if (request.inputType() == InputType.VOICE) {
            saveVoiceCondition(
                    message.getMessageId(),
                    request.voiceCondition()
            );
        }

        return AiMessageSaveResponse.from(message);
    }
    /**
     * 실제 사용자에게 표시된 AI 비서 응답을 저장합니다.
     * AI 응답은 입력 행동이나 음성 상태를 저장하지 않고,
     * senderType=ASSISTANT, inputType=null로 저장합니다.
     */
    @Transactional
    public AiMessageSaveResponse saveAssistantMessage(
            Long userId,
            Long sessionId,
            AiAssistantMessageSaveRequest request
    ) {
        validateUser(userId);

        if (request == null
                || request.content() == null
                || request.content().isBlank()) {
            throw new IllegalArgumentException(
                    "AI 응답 내용은 필수입니다."
            );
        }

        InteractionSession session =
                findOwnedSession(userId, sessionId);

        /*
         * 종료된 대화 세션에는 AI 응답도
         * 추가로 저장할 수 없습니다.
         */
        if (!session.isActive()) {
            throw new IllegalArgumentException(
                    "이미 종료된 대화 세션입니다."
            );
        }

        /*
         * senderType과 inputType은 프론트가 지정하지 않습니다.
         *
         * 백엔드에서 ASSISTANT와 null로 고정하여
         * 메시지 유형이 임의로 변경되지 않도록 합니다.
         */
        InteractionMessage message =
                interactionMessageRepository.save(
                        InteractionMessage.builder()
                                .sessionId(sessionId)
                                .senderType(SenderType.ASSISTANT)
                                .inputType(null)
                                .content(request.content().trim())
                                .build()
                );

        /*
         * AI 응답에는 사용자 행동 기록과
         * 음성 상태 기록을 생성하지 않습니다.
         */
        return AiMessageSaveResponse.from(message);
    }
    /**
     * CHAT/VOICE 입력 방식과 음성 상태 요청의 관계를 검증합니다.
     */
    private void validateMessageRequest(
            AiMessageSaveRequest request
    ) {
        if (request.inputType() == null) {
            throw new IllegalArgumentException(
                    "입력 방식은 필수입니다."
            );
        }

        if (request.content() == null
                || request.content().isBlank()) {
            throw new IllegalArgumentException(
                    "메시지 내용은 필수입니다."
            );
        }

        /*
         * CHAT 메시지에는 음성 상태가 존재하면 안 됩니다.
         */
        if (request.inputType() == InputType.CHAT
                && request.voiceCondition() != null) {
            throw new IllegalArgumentException(
                    "채팅 메시지에는 음성 상태를 저장할 수 없습니다."
            );
        }

        /*
         * VOICE 메시지는 프론트에서 측정한
         * 음성 상태 정보가 필요합니다.
         */
        if (request.inputType() == InputType.VOICE
                && request.voiceCondition() == null) {
            throw new IllegalArgumentException(
                    "음성 메시지에는 음성 상태가 필요합니다."
            );
        }
    }

    /**
     * 사용자 메시지의 행동 측정값을 저장합니다.
     */
    private void saveBehavior(
            Long sessionId,
            Long messageId,
            AiBehaviorRequest request
    ) {
        /*
         * 프론트에서 행동 측정값을 보내지 않았으면
         * 모든 횟수는 0, 시간값은 null로 저장합니다.
         */
        Long responseDelayMs =
                request == null
                        ? null
                        : request.responseDelayMs();

        Long typingDurationMs =
                request == null
                        ? null
                        : request.typingDurationMs();

        int editCount =
                request == null
                        ? 0
                        : request.normalizedEditCount();

        int fullDeleteCount =
                request == null
                        ? 0
                        : request.normalizedFullDeleteCount();

        int typingPauseCount =
                request == null
                        ? 0
                        : request.normalizedTypingPauseCount();

        int answerReversalCount =
                request == null
                        ? 0
                        : request.normalizedAnswerReversalCount();

        int confusionCount =
                request == null
                        ? 0
                        : request.normalizedConfusionCount();

        int reexplanationCount =
                request == null
                        ? 0
                        : request.normalizedReexplanationCount();

        interactionBehaviorRepository.save(
                InteractionBehavior.builder()
                        .sessionId(sessionId)
                        .messageId(messageId)
                        .responseDelayMs(responseDelayMs)
                        .typingDurationMs(typingDurationMs)
                        .editCount(editCount)
                        .fullDeleteCount(fullDeleteCount)
                        .typingPauseCount(typingPauseCount)
                        .answerReversalCount(
                                answerReversalCount
                        )
                        .confusionCount(confusionCount)
                        .reexplanationCount(
                                reexplanationCount
                        )
                        .build()
        );
    }

    /**
     * VOICE 입력의 음성 상태를 저장합니다.
     *
     * pitchMean과 pitchVariation은 값을 설정하지 않으므로
     * DB에 null로 저장됩니다.
     */
    private void saveVoiceCondition(
            Long messageId,
            AiVoiceConditionRequest request
    ) {
        voiceConditionRecordRepository.save(
                VoiceConditionRecord.builder()
                        .messageId(messageId)
                        .speechDurationMs(
                                request.speechDurationMs()
                        )
                        .speechRate(request.speechRate())
                        .avgPauseDurationMs(
                                request.avgPauseDurationMs()
                        )
                        .longPauseCount(
                                request.normalizedLongPauseCount()
                        )

                        /*
                         * 이번 MVP에서는 Pitch를 측정하지 않습니다.
                         * 아래 두 필드는 Builder에서 생략되어
                         * null로 저장됩니다.
                         *
                         * pitchMean
                         * pitchVariation
                         */

                        .build()
        );
    }

    /**
     * 세션을 조회하고 로그인 사용자의 소유인지 확인합니다.
     */
    private InteractionSession findOwnedSession(
            Long userId,
            Long sessionId
    ) {
        if (sessionId == null) {
            throw new IllegalArgumentException(
                    "대화 세션 ID는 필수입니다."
            );
        }

        InteractionSession session =
                interactionSessionRepository
                        .findById(sessionId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "존재하지 않는 대화 세션입니다."
                                )
                        );

        if (!session.getUserId().equals(userId)) {
            throw new IllegalArgumentException(
                    "해당 대화 세션에 접근할 수 없습니다."
            );
        }

        return session;
    }

    /**
     * JWT에서 얻은 사용자 ID가 유효한지 확인합니다.
     */
    private void validateUser(
            Long userId
    ) {
        if (userId == null
                || !userRepository.existsById(userId)) {
            throw new IllegalArgumentException(
                    "현재 로그인 사용자를 찾을 수 없습니다."
            );
        }
    }
}
package com.team0123.dndn.ai.service;

import com.team0123.dndn.ai.dto.AiAssistantMessageSaveRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AI 비서의 대화 세션과 메시지 저장 기능을 검증합니다.
 *
 * 실제 DB와 연결하지 않고 Repository를 Mock으로 사용합니다.
 */
class AiConversationServiceTest {

    private InteractionSessionRepository
            interactionSessionRepository;

    private InteractionMessageRepository
            interactionMessageRepository;

    private InteractionBehaviorRepository
            interactionBehaviorRepository;

    private VoiceConditionRecordRepository
            voiceConditionRecordRepository;

    private UserRepository userRepository;

    private AiConversationService aiConversationService;

    /**
     * 각 테스트를 실행하기 전에 Mock 저장소와
     * 테스트 대상 Service를 새로 생성합니다.
     */
    @BeforeEach
    void setUp() {
        interactionSessionRepository =
                mock(InteractionSessionRepository.class);

        interactionMessageRepository =
                mock(InteractionMessageRepository.class);

        interactionBehaviorRepository =
                mock(InteractionBehaviorRepository.class);

        voiceConditionRecordRepository =
                mock(VoiceConditionRecordRepository.class);

        userRepository =
                mock(UserRepository.class);

        /*
         * AiConversationService 생성자에 선언된 순서와
         * 동일한 순서로 다섯 개의 Repository를 전달합니다.
         */
        aiConversationService =
                new AiConversationService(
                        interactionSessionRepository,
                        interactionMessageRepository,
                        interactionBehaviorRepository,
                        voiceConditionRecordRepository,
                        userRepository
                );
    }

    /**
     * 로그인 사용자는 새로운 AI 비서 대화 세션을
     * 생성할 수 있어야 합니다.
     */
    @Test
    void createsSessionForAuthenticatedUser() {
        // given
        Long userId = 1L;

        LocalDateTime startedAt =
                LocalDateTime.of(
                        2026,
                        9,
                        7,
                        19,
                        30
                );

        when(userRepository.existsById(userId))
                .thenReturn(true);

        /*
         * 실제 DB에서는 ID와 시간이 자동으로 생성됩니다.
         * 단위 테스트에서는 저장 결과를 직접 준비합니다.
         */
        InteractionSession savedSession =
                InteractionSession.builder()
                        .sessionId(10L)
                        .userId(userId)
                        .startedAt(startedAt)
                        .createdAt(startedAt)
                        .build();

        when(interactionSessionRepository.save(any()))
                .thenReturn(savedSession);

        // when
        AiSessionResponse response =
                aiConversationService.createSession(userId);

        // then
        assertEquals(10L, response.sessionId());
        assertEquals(startedAt, response.startedAt());
        assertNull(response.endedAt());

        /*
         * 저장하려고 만든 세션에 로그인 사용자의 ID가
         * 정확하게 들어갔는지 확인합니다.
         */
        ArgumentCaptor<InteractionSession> captor =
                ArgumentCaptor.forClass(
                        InteractionSession.class
                );

        verify(interactionSessionRepository)
                .save(captor.capture());

        assertEquals(
                userId,
                captor.getValue().getUserId()
        );
    }

    /**
     * 로그인 사용자는 자신이 만든 세션을
     * 종료할 수 있어야 합니다.
     */
    @Test
    void endsOwnedSession() {
        // given
        Long userId = 1L;
        Long sessionId = 10L;

        when(userRepository.existsById(userId))
                .thenReturn(true);

        InteractionSession session =
                createActiveSession(
                        sessionId,
                        userId
                );

        when(interactionSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        // when
        AiSessionResponse response =
                aiConversationService.endSession(
                        userId,
                        sessionId
                );

        // then
        assertEquals(sessionId, response.sessionId());
        assertNotNull(response.endedAt());
        assertFalse(session.isActive());
    }

    /**
     * 다른 사용자가 소유한 세션에는
     * 접근할 수 없어야 합니다.
     */
    @Test
    void rejectsAnotherUsersSession() {
        // given
        Long loginUserId = 1L;
        Long ownerUserId = 2L;
        Long sessionId = 10L;

        when(userRepository.existsById(loginUserId))
                .thenReturn(true);

        InteractionSession anotherUsersSession =
                createActiveSession(
                        sessionId,
                        ownerUserId
                );

        when(interactionSessionRepository.findById(sessionId))
                .thenReturn(
                        Optional.of(anotherUsersSession)
                );

        // when & then
        assertThrows(
                IllegalArgumentException.class,
                () -> aiConversationService.endSession(
                        loginUserId,
                        sessionId
                )
        );
    }

    /**
     * 존재하지 않는 세션을 종료하려 하면
     * 예외가 발생해야 합니다.
     */
    @Test
    void rejectsMissingSession() {
        // given
        Long userId = 1L;
        Long sessionId = 999L;

        when(userRepository.existsById(userId))
                .thenReturn(true);

        when(interactionSessionRepository.findById(sessionId))
                .thenReturn(Optional.empty());

        // when & then
        assertThrows(
                IllegalArgumentException.class,
                () -> aiConversationService.endSession(
                        userId,
                        sessionId
                )
        );
    }

    /**
     * 로그인 사용자를 찾을 수 없으면
     * 세션을 생성할 수 없어야 합니다.
     */
    @Test
    void rejectsMissingAuthenticatedUser() {
        // given
        Long userId = 999L;

        when(userRepository.existsById(userId))
                .thenReturn(false);

        // when & then
        assertThrows(
                IllegalArgumentException.class,
                () -> aiConversationService.createSession(
                        userId
                )
        );
    }

    /**
     * 채팅 입력을 저장하면 사용자 메시지와 행동 기록이
     * 각각 한 번씩 저장되어야 합니다.
     *
     * 채팅 입력에는 음성 상태를 저장하지 않습니다.
     */
    @Test
    void savesChatMessageAndBehavior() {
        // given
        Long userId = 1L;
        Long sessionId = 10L;
        Long messageId = 100L;

        prepareActiveOwnedSession(
                userId,
                sessionId
        );

        AiBehaviorRequest behaviorRequest =
                new AiBehaviorRequest(
                        1500L,
                        4000L,
                        2,
                        1,
                        1,
                        0,
                        1,
                        0
                );

        AiMessageSaveRequest request =
                new AiMessageSaveRequest(
                        InputType.CHAT,
                        "  아들에게 생활비를 보내려고 해요.  ",
                        behaviorRequest,
                        null
                );

        InteractionMessage savedMessage =
                createSavedMessage(
                        messageId,
                        sessionId,
                        SenderType.USER,
                        InputType.CHAT,
                        "아들에게 생활비를 보내려고 해요."
                );

        when(interactionMessageRepository.save(any()))
                .thenReturn(savedMessage);

        // when
        AiMessageSaveResponse response =
                aiConversationService.saveUserMessage(
                        userId,
                        sessionId,
                        request
                );

        // then
        assertEquals(messageId, response.messageId());
        assertEquals(sessionId, response.sessionId());
        assertEquals(SenderType.USER, response.senderType());
        assertEquals(InputType.CHAT, response.inputType());

        /*
         * 메시지 앞뒤의 불필요한 공백이 제거되어
         * 저장되는지 확인합니다.
         */
        ArgumentCaptor<InteractionMessage> messageCaptor =
                ArgumentCaptor.forClass(
                        InteractionMessage.class
                );

        verify(interactionMessageRepository)
                .save(messageCaptor.capture());

        InteractionMessage capturedMessage =
                messageCaptor.getValue();

        assertEquals(
                sessionId,
                capturedMessage.getSessionId()
        );

        assertEquals(
                SenderType.USER,
                capturedMessage.getSenderType()
        );

        assertEquals(
                InputType.CHAT,
                capturedMessage.getInputType()
        );

        assertEquals(
                "아들에게 생활비를 보내려고 해요.",
                capturedMessage.getContent()
        );

        /*
         * 프론트에서 전달한 행동 측정값이
         * 메시지와 연결되어 저장되는지 확인합니다.
         */
        ArgumentCaptor<InteractionBehavior> behaviorCaptor =
                ArgumentCaptor.forClass(
                        InteractionBehavior.class
                );

        verify(interactionBehaviorRepository)
                .save(behaviorCaptor.capture());

        InteractionBehavior capturedBehavior =
                behaviorCaptor.getValue();

        assertEquals(
                sessionId,
                capturedBehavior.getSessionId()
        );

        assertEquals(
                messageId,
                capturedBehavior.getMessageId()
        );

        assertEquals(
                1500L,
                capturedBehavior.getResponseDelayMs()
        );

        assertEquals(
                4000L,
                capturedBehavior.getTypingDurationMs()
        );

        assertEquals(
                2,
                capturedBehavior.getEditCount()
        );

        assertEquals(
                1,
                capturedBehavior.getFullDeleteCount()
        );

        assertEquals(
                1,
                capturedBehavior.getTypingPauseCount()
        );

        assertEquals(
                0,
                capturedBehavior.getAnswerReversalCount()
        );

        assertEquals(
                1,
                capturedBehavior.getConfusionCount()
        );

        assertEquals(
                0,
                capturedBehavior.getReexplanationCount()
        );

        /*
         * 채팅 메시지이므로 음성 상태 저장소는
         * 호출되지 않아야 합니다.
         */
        verify(
                voiceConditionRecordRepository,
                never()
        ).save(any());
    }

    /**
     * 음성 입력을 저장하면 사용자 메시지, 행동 기록,
     * 음성 상태가 모두 저장되어야 합니다.
     */
    @Test
    void savesVoiceMessageBehaviorAndVoiceCondition() {
        // given
        Long userId = 1L;
        Long sessionId = 10L;
        Long messageId = 101L;

        prepareActiveOwnedSession(
                userId,
                sessionId
        );

        /*
         * 음성 입력에서는 채팅 작성시간이 없을 수 있으므로
         * typingDurationMs를 null로 전달합니다.
         */
        AiBehaviorRequest behaviorRequest =
                new AiBehaviorRequest(
                        2000L,
                        null,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0
                );

        AiVoiceConditionRequest voiceRequest =
                new AiVoiceConditionRequest(
                        3500L,
                        new BigDecimal("3.25"),
                        700L,
                        2
                );

        AiMessageSaveRequest request =
                new AiMessageSaveRequest(
                        InputType.VOICE,
                        "김 검사가 보내라고 했어요.",
                        behaviorRequest,
                        voiceRequest
                );

        InteractionMessage savedMessage =
                createSavedMessage(
                        messageId,
                        sessionId,
                        SenderType.USER,
                        InputType.VOICE,
                        "김 검사가 보내라고 했어요."
                );

        when(interactionMessageRepository.save(any()))
                .thenReturn(savedMessage);

        // when
        AiMessageSaveResponse response =
                aiConversationService.saveUserMessage(
                        userId,
                        sessionId,
                        request
                );

        // then
        assertEquals(messageId, response.messageId());
        assertEquals(InputType.VOICE, response.inputType());

        verify(interactionBehaviorRepository)
                .save(any(InteractionBehavior.class));

        /*
         * 음성 상태가 해당 메시지 ID와 연결되고,
         * 프론트 측정값이 그대로 저장되는지 확인합니다.
         */
        ArgumentCaptor<VoiceConditionRecord> voiceCaptor =
                ArgumentCaptor.forClass(
                        VoiceConditionRecord.class
                );

        verify(voiceConditionRecordRepository)
                .save(voiceCaptor.capture());

        VoiceConditionRecord capturedVoice =
                voiceCaptor.getValue();

        assertEquals(
                messageId,
                capturedVoice.getMessageId()
        );

        assertEquals(
                3500L,
                capturedVoice.getSpeechDurationMs()
        );

        assertEquals(
                new BigDecimal("3.25"),
                capturedVoice.getSpeechRate()
        );

        assertEquals(
                700L,
                capturedVoice.getAvgPauseDurationMs()
        );

        assertEquals(
                2,
                capturedVoice.getLongPauseCount()
        );

        /*
         * 이번 MVP에서는 Pitch를 측정하지 않으므로
         * 두 필드는 null이어야 합니다.
         */
        assertNull(capturedVoice.getPitchMean());
        assertNull(capturedVoice.getPitchVariation());
    }

    /**
     * 행동 정보가 생략돼도 행동 기록은 생성되고,
     * 횟수 값은 모두 0으로 저장되어야 합니다.
     */
    @Test
    void savesDefaultBehaviorWhenBehaviorIsMissing() {
        // given
        Long userId = 1L;
        Long sessionId = 10L;
        Long messageId = 102L;

        prepareActiveOwnedSession(
                userId,
                sessionId
        );

        AiMessageSaveRequest request =
                new AiMessageSaveRequest(
                        InputType.CHAT,
                        "잔액을 알려줘.",
                        null,
                        null
                );

        InteractionMessage savedMessage =
                createSavedMessage(
                        messageId,
                        sessionId,
                        SenderType.USER,
                        InputType.CHAT,
                        "잔액을 알려줘."
                );

        when(interactionMessageRepository.save(any()))
                .thenReturn(savedMessage);

        // when
        aiConversationService.saveUserMessage(
                userId,
                sessionId,
                request
        );

        // then
        ArgumentCaptor<InteractionBehavior> captor =
                ArgumentCaptor.forClass(
                        InteractionBehavior.class
                );

        verify(interactionBehaviorRepository)
                .save(captor.capture());

        InteractionBehavior behavior =
                captor.getValue();

        assertNull(behavior.getResponseDelayMs());
        assertNull(behavior.getTypingDurationMs());
        assertEquals(0, behavior.getEditCount());
        assertEquals(0, behavior.getFullDeleteCount());
        assertEquals(0, behavior.getTypingPauseCount());
        assertEquals(0, behavior.getAnswerReversalCount());
        assertEquals(0, behavior.getConfusionCount());
        assertEquals(0, behavior.getReexplanationCount());
    }

    /**
     * 음성 입력에는 음성 상태 정보가 반드시
     * 포함되어야 합니다.
     */
    @Test
    void rejectsVoiceMessageWithoutVoiceCondition() {
        // given
        Long userId = 1L;
        Long sessionId = 10L;

        prepareActiveOwnedSession(
                userId,
                sessionId
        );

        AiMessageSaveRequest request =
                new AiMessageSaveRequest(
                        InputType.VOICE,
                        "아들에게 돈을 보내줘.",
                        null,
                        null
                );

        // when & then
        assertThrows(
                IllegalArgumentException.class,
                () -> aiConversationService.saveUserMessage(
                        userId,
                        sessionId,
                        request
                )
        );

        /*
         * 검증에 실패했으므로 어떠한 메시지나 측정값도
         * 저장되어서는 안 됩니다.
         */
        verify(
                interactionMessageRepository,
                never()
        ).save(any());

        verify(
                interactionBehaviorRepository,
                never()
        ).save(any());

        verify(
                voiceConditionRecordRepository,
                never()
        ).save(any());
    }

    /**
     * 채팅 입력에는 음성 상태 정보를
     * 포함할 수 없어야 합니다.
     */
    @Test
    void rejectsChatMessageWithVoiceCondition() {
        // given
        Long userId = 1L;
        Long sessionId = 10L;

        prepareActiveOwnedSession(
                userId,
                sessionId
        );

        AiVoiceConditionRequest voiceRequest =
                new AiVoiceConditionRequest(
                        3000L,
                        new BigDecimal("3.10"),
                        600L,
                        1
                );

        AiMessageSaveRequest request =
                new AiMessageSaveRequest(
                        InputType.CHAT,
                        "아들에게 돈을 보내줘.",
                        null,
                        voiceRequest
                );

        // when & then
        assertThrows(
                IllegalArgumentException.class,
                () -> aiConversationService.saveUserMessage(
                        userId,
                        sessionId,
                        request
                )
        );

        verify(
                interactionMessageRepository,
                never()
        ).save(any());
    }

    /**
     * 종료된 세션에는 새로운 사용자 메시지를
     * 저장할 수 없어야 합니다.
     */
    @Test
    void rejectsMessageForEndedSession() {
        // given
        Long userId = 1L;
        Long sessionId = 10L;

        when(userRepository.existsById(userId))
                .thenReturn(true);

        InteractionSession endedSession =
                createActiveSession(
                        sessionId,
                        userId
                );

        /*
         * 테스트용 세션을 미리 종료합니다.
         */
        endedSession.end();

        when(interactionSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(endedSession));

        AiMessageSaveRequest request =
                new AiMessageSaveRequest(
                        InputType.CHAT,
                        "잔액을 알려줘.",
                        null,
                        null
                );

        // when & then
        assertThrows(
                IllegalArgumentException.class,
                () -> aiConversationService.saveUserMessage(
                        userId,
                        sessionId,
                        request
                )
        );

        verify(
                interactionMessageRepository,
                never()
        ).save(any());
    }

    /**
     * AI 응답은 ASSISTANT 메시지로 저장하고
     * 입력 방식은 null이어야 합니다.
     *
     * AI 응답에는 행동 기록이나 음성 상태를
     * 생성하지 않습니다.
     */
    @Test
    void savesAssistantMessageWithoutMeasurements() {
        // given
        Long userId = 1L;
        Long sessionId = 10L;
        Long messageId = 103L;

        prepareActiveOwnedSession(
                userId,
                sessionId
        );

        AiAssistantMessageSaveRequest request =
                new AiAssistantMessageSaveRequest(
                        "  누구에게 얼마를 보내실 건가요?  "
                );

        InteractionMessage savedMessage =
                createSavedMessage(
                        messageId,
                        sessionId,
                        SenderType.ASSISTANT,
                        null,
                        "누구에게 얼마를 보내실 건가요?"
                );

        when(interactionMessageRepository.save(any()))
                .thenReturn(savedMessage);

        // when
        AiMessageSaveResponse response =
                aiConversationService.saveAssistantMessage(
                        userId,
                        sessionId,
                        request
                );

        // then
        assertEquals(messageId, response.messageId());
        assertEquals(SenderType.ASSISTANT, response.senderType());
        assertNull(response.inputType());

        ArgumentCaptor<InteractionMessage> messageCaptor =
                ArgumentCaptor.forClass(
                        InteractionMessage.class
                );

        verify(interactionMessageRepository)
                .save(messageCaptor.capture());

        InteractionMessage capturedMessage =
                messageCaptor.getValue();

        assertEquals(
                SenderType.ASSISTANT,
                capturedMessage.getSenderType()
        );

        assertNull(capturedMessage.getInputType());

        assertEquals(
                "누구에게 얼마를 보내실 건가요?",
                capturedMessage.getContent()
        );

        verify(
                interactionBehaviorRepository,
                never()
        ).save(any());

        verify(
                voiceConditionRecordRepository,
                never()
        ).save(any());
    }

    /**
     * 활성 상태이고 로그인 사용자가 소유한
     * 세션 조회 조건을 준비합니다.
     */
    private void prepareActiveOwnedSession(
            Long userId,
            Long sessionId
    ) {
        when(userRepository.existsById(userId))
                .thenReturn(true);

        InteractionSession session =
                createActiveSession(
                        sessionId,
                        userId
                );

        when(interactionSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));
    }

    /**
     * 아직 종료되지 않은 테스트용 세션을 생성합니다.
     */
    private InteractionSession createActiveSession(
            Long sessionId,
            Long userId
    ) {
        LocalDateTime startedAt =
                LocalDateTime.of(
                        2026,
                        9,
                        7,
                        19,
                        30
                );

        return InteractionSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .startedAt(startedAt)
                .createdAt(startedAt)
                .build();
    }

    /**
     * Repository가 저장 후 반환하는 테스트용
     * 메시지 Entity를 생성합니다.
     */
    private InteractionMessage createSavedMessage(
            Long messageId,
            Long sessionId,
            SenderType senderType,
            InputType inputType,
            String content
    ) {
        return InteractionMessage.builder()
                .messageId(messageId)
                .sessionId(sessionId)
                .senderType(senderType)
                .inputType(inputType)
                .content(content)
                .createdAt(
                        LocalDateTime.of(
                                2026,
                                9,
                                7,
                                19,
                                31
                        )
                )
                .build();
    }
}
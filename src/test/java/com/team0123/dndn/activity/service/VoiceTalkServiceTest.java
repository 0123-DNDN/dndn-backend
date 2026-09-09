package com.team0123.dndn.activity.service;

import com.team0123.dndn.activity.dto.ActivityResultSaveRequest;
import com.team0123.dndn.activity.dto.VoiceTalkAnswerRequest;
import com.team0123.dndn.activity.dto.VoiceTalkAnswerResponse;
import com.team0123.dndn.activity.dto.VoiceTalkStartResponse;
import com.team0123.dndn.activity.entity.Activity;
import com.team0123.dndn.activity.entity.ActivityStatus;
import com.team0123.dndn.activity.entity.ActivityType;
import com.team0123.dndn.activity.repository.ActivityRepository;
import com.team0123.dndn.activity.support.VoiceTalkOpeningQuestions;
import com.team0123.dndn.ai.client.GeminiDailyTalkClient;
import com.team0123.dndn.interaction.entity.InputType;
import com.team0123.dndn.interaction.entity.InteractionMessage;
import com.team0123.dndn.interaction.entity.InteractionSession;
import com.team0123.dndn.interaction.entity.SenderType;
import com.team0123.dndn.interaction.repository.InteractionMessageRepository;
import com.team0123.dndn.interaction.repository.InteractionSessionRepository;
import com.team0123.dndn.user.entity.Role;
import com.team0123.dndn.user.entity.User;
import com.team0123.dndn.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class VoiceTalkServiceTest {

    @Mock
    private InteractionSessionRepository interactionSessionRepository;

    @Mock
    private InteractionMessageRepository interactionMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private ActivityService activityService;

    @Mock
    private VoiceTalkOpeningQuestions openingQuestions;

    @Mock
    private GeminiDailyTalkClient geminiDailyTalkClient;

    @Captor
    private ArgumentCaptor<List<InteractionMessage>> contextCaptor;

    private VoiceTalkService voiceTalkService;

    @BeforeEach
    void setUp() {
        voiceTalkService = new VoiceTalkService(
                interactionSessionRepository,
                interactionMessageRepository,
                userRepository,
                activityRepository,
                activityService,
                openingQuestions,
                geminiDailyTalkClient
        );
    }

    @Test
    void startCreatesSessionAndStoresRandomAssistantQuestion() {
        mockSenior(1L);
        mockVoiceTalkActivity();
        when(interactionSessionRepository.save(any(InteractionSession.class)))
                .thenReturn(activeSession(10L, 1L));
        when(openingQuestions.randomQuestion())
                .thenReturn("오늘 가장 기억에 남는 일이 있었나요?");

        VoiceTalkStartResponse response = voiceTalkService.start(1L);

        ArgumentCaptor<InteractionSession> sessionCaptor =
                ArgumentCaptor.forClass(InteractionSession.class);
        verify(interactionSessionRepository).save(sessionCaptor.capture());
        assertEquals(1L, sessionCaptor.getValue().getUserId());

        ArgumentCaptor<InteractionMessage> messageCaptor =
                ArgumentCaptor.forClass(InteractionMessage.class);
        verify(interactionMessageRepository).save(messageCaptor.capture());
        InteractionMessage question = messageCaptor.getValue();
        assertEquals(10L, question.getSessionId());
        assertEquals(SenderType.ASSISTANT, question.getSenderType());
        assertNull(question.getInputType());
        assertEquals(response.question(), question.getContent());
        assertEquals(10L, response.sessionId());
        assertEquals(4, response.totalQuestions());
    }

    @Test
    void firstThroughThirdAnswersUseOrderedContextAndContinue() {
        mockSenior(1L);
        InteractionSession session = activeSession(10L, 1L);
        when(interactionSessionRepository.findById(10L))
                .thenReturn(Optional.of(session));
        when(openingQuestions.contains("질문 1")).thenReturn(true);
        when(interactionMessageRepository
                .findAllBySessionIdOrderByCreatedAtAscMessageIdAsc(10L))
                .thenReturn(
                        conversationAfterAnswer(1),
                        conversationAfterAnswer(2),
                        conversationAfterAnswer(3)
                );
        when(geminiDailyTalkClient.generateNextQuestion(any()))
                .thenReturn(
                        Optional.of("질문 2"),
                        Optional.of("질문 3"),
                        Optional.of("질문 4")
                );
        when(interactionMessageRepository.save(any(InteractionMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        for (int answerNumber = 1; answerNumber <= 3; answerNumber++) {
            VoiceTalkAnswerResponse response = voiceTalkService.answer(
                    1L,
                    10L,
                    new VoiceTalkAnswerRequest("답변 " + answerNumber)
            );

            assertEquals(answerNumber, response.answeredCount());
            assertEquals("질문 " + (answerNumber + 1), response.nextQuestion());
            assertNull(response.summary());
            assertFalse(response.completed());
        }

        verify(geminiDailyTalkClient, times(3))
                .generateNextQuestion(contextCaptor.capture());

        List<InteractionMessage> thirdContext =
                contextCaptor.getAllValues().get(2);
        assertEquals(6, thirdContext.size());
        assertEquals("질문 1", thirdContext.get(0).getContent());
        assertEquals("답변 1", thirdContext.get(1).getContent());
        assertEquals("질문 3", thirdContext.get(4).getContent());
        assertEquals("답변 3", thirdContext.get(5).getContent());

        ArgumentCaptor<InteractionMessage> savedMessageCaptor =
                ArgumentCaptor.forClass(InteractionMessage.class);
        verify(interactionMessageRepository, times(6))
                .save(savedMessageCaptor.capture());
        List<InteractionMessage> savedMessages =
                savedMessageCaptor.getAllValues();
        assertEquals(SenderType.USER, savedMessages.get(0).getSenderType());
        assertEquals(InputType.VOICE, savedMessages.get(0).getInputType());
        assertEquals("답변 1", savedMessages.get(0).getContent());
        assertEquals(SenderType.ASSISTANT, savedMessages.get(1).getSenderType());
        assertEquals("질문 2", savedMessages.get(1).getContent());
        verify(activityService, never()).saveResult(anyLong(), anyLong(), any());
    }

    @Test
    void fourthAnswerStoresClosingSummaryAndCompletesActivity() {
        mockSenior(1L);
        mockVoiceTalkActivity();
        InteractionSession session = activeSession(10L, 1L);
        when(interactionSessionRepository.findById(10L))
                .thenReturn(Optional.of(session));
        when(openingQuestions.contains("질문 1")).thenReturn(true);
        when(interactionMessageRepository
                .findAllBySessionIdOrderByCreatedAtAscMessageIdAsc(10L))
                .thenReturn(conversationAfterAnswer(4));
        when(interactionMessageRepository.save(any(InteractionMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(geminiDailyTalkClient.generateClosingSummary(any()))
                .thenReturn(Optional.of(
                        "오늘은 따님과 식사하고 산책하셨다고 말씀해주셨어요. "
                                + "오늘 이야기 들려주셔서 고마워요."
                ));

        VoiceTalkAnswerResponse response = voiceTalkService.answer(
                1L,
                10L,
                new VoiceTalkAnswerRequest("답변 4")
        );

        assertEquals(4, response.answeredCount());
        assertNull(response.nextQuestion());
        assertEquals(
                "오늘은 따님과 식사하고 산책하셨다고 말씀해주셨어요. "
                        + "오늘 이야기 들려주셔서 고마워요.",
                response.summary()
        );
        assertTrue(response.completed());
        assertFalse(session.isActive());
        verify(geminiDailyTalkClient, never()).generateNextQuestion(any());
        verify(geminiDailyTalkClient).generateClosingSummary(contextCaptor.capture());
        assertEquals(8, contextCaptor.getValue().size());

        ArgumentCaptor<InteractionMessage> messageCaptor =
                ArgumentCaptor.forClass(InteractionMessage.class);
        verify(interactionMessageRepository, times(2))
                .save(messageCaptor.capture());
        InteractionMessage closingMessage =
                messageCaptor.getAllValues().get(1);
        assertEquals(SenderType.ASSISTANT, closingMessage.getSenderType());
        assertNull(closingMessage.getInputType());
        assertEquals(response.summary(), closingMessage.getContent());

        ArgumentCaptor<ActivityResultSaveRequest> resultCaptor =
                ArgumentCaptor.forClass(ActivityResultSaveRequest.class);
        verify(activityService).saveResult(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(2L),
                resultCaptor.capture()
        );
        ActivityResultSaveRequest resultRequest = resultCaptor.getValue();
        assertEquals(10L, resultRequest.sessionId());
        assertEquals(ActivityStatus.COMPLETED, resultRequest.status());
        assertNull(resultRequest.score());
        assertNull(resultRequest.stepCount());
    }

    @Test
    void rejectsAnotherUsersSession() {
        mockSenior(1L);
        when(interactionSessionRepository.findById(10L))
                .thenReturn(Optional.of(activeSession(10L, 2L)));

        assertThrows(
                IllegalArgumentException.class,
                () -> voiceTalkService.answer(
                        1L,
                        10L,
                        new VoiceTalkAnswerRequest("답변")
                )
        );
        verify(interactionMessageRepository, never()).save(any());
    }

    @Test
    void rejectsMissingSession() {
        mockSenior(1L);
        when(interactionSessionRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> voiceTalkService.answer(
                        1L,
                        99L,
                        new VoiceTalkAnswerRequest("답변")
                )
        );
        verify(interactionMessageRepository, never()).save(any());
    }

    @Test
    void rejectsAdditionalAnswerForCompletedSession() {
        mockSenior(1L);
        InteractionSession session = activeSession(10L, 1L);
        session.end();
        when(interactionSessionRepository.findById(10L))
                .thenReturn(Optional.of(session));

        assertThrows(
                IllegalArgumentException.class,
                () -> voiceTalkService.answer(
                        1L,
                        10L,
                        new VoiceTalkAnswerRequest("추가 답변")
                )
        );
        verify(interactionMessageRepository, never()).save(any());
    }

    @Test
    void geminiFailureDoesNotStoreAssistantMessage() {
        mockSenior(1L);
        when(interactionSessionRepository.findById(10L))
                .thenReturn(Optional.of(activeSession(10L, 1L)));
        when(openingQuestions.contains("질문 1")).thenReturn(true);
        when(interactionMessageRepository
                .findAllBySessionIdOrderByCreatedAtAscMessageIdAsc(10L))
                .thenReturn(conversationAfterAnswer(1));
        when(interactionMessageRepository.save(any(InteractionMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(geminiDailyTalkClient.generateNextQuestion(any()))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> voiceTalkService.answer(
                        1L,
                        10L,
                        new VoiceTalkAnswerRequest("답변 1")
                )
        );

        ArgumentCaptor<InteractionMessage> messageCaptor =
                ArgumentCaptor.forClass(InteractionMessage.class);
        verify(interactionMessageRepository).save(messageCaptor.capture());
        assertEquals(SenderType.USER, messageCaptor.getValue().getSenderType());
    }

    @Test
    void missingVoiceTalkActivityPreventsStart() {
        mockSenior(1L);
        when(activityRepository
                .findFirstByActivityTypeAndIsActiveTrue(ActivityType.VOICE_TALK))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> voiceTalkService.start(1L)
        );
        verify(interactionSessionRepository, never()).save(any());
    }

    @Test
    void guardianCannotStartVoiceTalk() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(
                User.builder().userId(2L).role(Role.GUARDIAN).build()
        ));

        assertThrows(
                IllegalArgumentException.class,
                () -> voiceTalkService.start(2L)
        );
        verify(activityRepository, never())
                .findFirstByActivityTypeAndIsActiveTrue(any());
        verify(interactionSessionRepository, never()).save(any());
    }

    @Test
    void activityResultFailureLeavesSessionOpen() {
        mockSenior(1L);
        mockVoiceTalkActivity();
        InteractionSession session = activeSession(10L, 1L);
        when(interactionSessionRepository.findById(10L))
                .thenReturn(Optional.of(session));
        when(openingQuestions.contains("질문 1")).thenReturn(true);
        when(interactionMessageRepository
                .findAllBySessionIdOrderByCreatedAtAscMessageIdAsc(10L))
                .thenReturn(conversationAfterAnswer(4));
        when(interactionMessageRepository.save(any(InteractionMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(geminiDailyTalkClient.generateClosingSummary(any()))
                .thenReturn(Optional.of("오늘 이야기 들려주셔서 고마워요."));
        when(activityService.saveResult(anyLong(), anyLong(), any()))
                .thenThrow(new IllegalArgumentException("결과 연결 실패"));

        assertThrows(
                IllegalArgumentException.class,
                () -> voiceTalkService.answer(
                        1L,
                        10L,
                        new VoiceTalkAnswerRequest("답변 4")
                )
        );
        assertTrue(session.isActive());
    }

    @Test
    void closingSummaryFailureDoesNotCompleteActivityOrEndSession() {
        mockSenior(1L);
        InteractionSession session = activeSession(10L, 1L);
        when(interactionSessionRepository.findById(10L))
                .thenReturn(Optional.of(session));
        when(openingQuestions.contains("질문 1")).thenReturn(true);
        when(interactionMessageRepository
                .findAllBySessionIdOrderByCreatedAtAscMessageIdAsc(10L))
                .thenReturn(conversationAfterAnswer(4));
        when(interactionMessageRepository.save(any(InteractionMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(geminiDailyTalkClient.generateClosingSummary(any()))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> voiceTalkService.answer(
                        1L,
                        10L,
                        new VoiceTalkAnswerRequest("답변 4")
                )
        );

        assertTrue(session.isActive());
        verify(geminiDailyTalkClient, never()).generateNextQuestion(any());
        verify(activityService, never()).saveResult(anyLong(), anyLong(), any());
        verify(activityRepository, never())
                .findFirstByActivityTypeAndIsActiveTrue(any());

        ArgumentCaptor<InteractionMessage> messageCaptor =
                ArgumentCaptor.forClass(InteractionMessage.class);
        verify(interactionMessageRepository).save(messageCaptor.capture());
        assertEquals(SenderType.USER, messageCaptor.getValue().getSenderType());
    }

    @Test
    void closingMessageSaveFailureDoesNotCompleteActivityOrEndSession() {
        mockSenior(1L);
        InteractionSession session = activeSession(10L, 1L);
        when(interactionSessionRepository.findById(10L))
                .thenReturn(Optional.of(session));
        when(openingQuestions.contains("질문 1")).thenReturn(true);
        when(interactionMessageRepository
                .findAllBySessionIdOrderByCreatedAtAscMessageIdAsc(10L))
                .thenReturn(conversationAfterAnswer(4));
        when(interactionMessageRepository.save(any(InteractionMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0))
                .thenThrow(new IllegalStateException("메시지 저장 실패"));
        when(geminiDailyTalkClient.generateClosingSummary(any()))
                .thenReturn(Optional.of("오늘 이야기 들려주셔서 고마워요."));

        assertThrows(
                IllegalStateException.class,
                () -> voiceTalkService.answer(
                        1L,
                        10L,
                        new VoiceTalkAnswerRequest("답변 4")
                )
        );

        assertTrue(session.isActive());
        verify(activityService, never()).saveResult(anyLong(), anyLong(), any());
        verify(activityRepository, never())
                .findFirstByActivityTypeAndIsActiveTrue(any());
    }

    private void mockSenior(Long userId) {
        when(userRepository.findById(userId)).thenReturn(Optional.of(
                User.builder().userId(userId).role(Role.SENIOR).build()
        ));
    }

    private void mockVoiceTalkActivity() {
        when(activityRepository
                .findFirstByActivityTypeAndIsActiveTrue(ActivityType.VOICE_TALK))
                .thenReturn(Optional.of(
                        Activity.builder()
                                .activityId(2L)
                                .activityType(ActivityType.VOICE_TALK)
                                .isActive(true)
                                .build()
                ));
    }

    private InteractionSession activeSession(Long sessionId, Long userId) {
        return InteractionSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .startedAt(LocalDateTime.of(2026, 9, 8, 10, 0))
                .createdAt(LocalDateTime.of(2026, 9, 8, 10, 0))
                .build();
    }

    private List<InteractionMessage> conversationAfterAnswer(int answerCount) {
        List<InteractionMessage> messages = new ArrayList<>();
        for (int number = 1; number <= answerCount; number++) {
            messages.add(message(
                    number * 2L - 1,
                    SenderType.ASSISTANT,
                    null,
                    "질문 " + number
            ));
            messages.add(message(
                    number * 2L,
                    SenderType.USER,
                    InputType.VOICE,
                    "답변 " + number
            ));
        }
        return messages;
    }

    private InteractionMessage message(
            Long messageId,
            SenderType senderType,
            InputType inputType,
            String content
    ) {
        return InteractionMessage.builder()
                .messageId(messageId)
                .sessionId(10L)
                .senderType(senderType)
                .inputType(inputType)
                .content(content)
                .createdAt(LocalDateTime.of(2026, 9, 8, 10, 0)
                        .plusMinutes(messageId))
                .build();
    }
}

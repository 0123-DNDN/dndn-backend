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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoiceAnalysisServiceTest {

    @Mock
    private InteractionSessionRepository interactionSessionRepository;

    @Mock
    private InteractionMessageRepository interactionMessageRepository;

    @Mock
    private VoiceConditionRecordRepository voiceConditionRecordRepository;

    @Mock
    private InteractionBehaviorRepository interactionBehaviorRepository;

    @Mock
    private UserRepository userRepository;

    private VoiceAnalysisService voiceAnalysisService;

    @BeforeEach
    void setUp() {
        voiceAnalysisService = new VoiceAnalysisService(
                interactionSessionRepository,
                interactionMessageRepository,
                voiceConditionRecordRepository,
                interactionBehaviorRepository,
                userRepository
        );
    }

    @Test
    void validRequestSavesErStructureForCurrentUser() {
        VoiceAnalysisSaveRequest request = new VoiceAnalysisSaveRequest(
                "어제 친구랑 밥 먹었어요.", 5.4, 2.1, 4.3
        );
        mockSavedSessionAndMessage(request.text());

        VoiceAnalysisSaveResponse response = voiceAnalysisService.save(1L, request);

        ArgumentCaptor<InteractionSession> sessionCaptor =
                ArgumentCaptor.forClass(InteractionSession.class);
        verify(interactionSessionRepository).save(sessionCaptor.capture());
        assertEquals(1L, sessionCaptor.getValue().getUserId());

        ArgumentCaptor<InteractionMessage> messageCaptor =
                ArgumentCaptor.forClass(InteractionMessage.class);
        verify(interactionMessageRepository).save(messageCaptor.capture());
        InteractionMessage message = messageCaptor.getValue();
        assertEquals(20L, message.getSessionId());
        assertEquals(SenderType.USER, message.getSenderType());
        assertEquals(InputType.VOICE, message.getInputType());
        assertEquals(request.text(), message.getContent());

        ArgumentCaptor<VoiceConditionRecord> conditionCaptor =
                ArgumentCaptor.forClass(VoiceConditionRecord.class);
        verify(voiceConditionRecordRepository).save(conditionCaptor.capture());
        VoiceConditionRecord condition = conditionCaptor.getValue();
        assertEquals(30L, condition.getMessageId());
        assertEquals(5400L, condition.getSpeechDurationMs());
        assertEquals(0, new BigDecimal("4.3").compareTo(condition.getSpeechRate()));
        assertNull(condition.getAvgPauseDurationMs());
        assertNull(condition.getPitchMean());
        assertNull(condition.getPitchVariation());

        ArgumentCaptor<InteractionBehavior> behaviorCaptor =
                ArgumentCaptor.forClass(InteractionBehavior.class);
        verify(interactionBehaviorRepository).save(behaviorCaptor.capture());
        InteractionBehavior behavior = behaviorCaptor.getValue();
        assertEquals(20L, behavior.getSessionId());
        assertEquals(30L, behavior.getMessageId());
        assertEquals(2100L, behavior.getResponseDelayMs());
        assertNull(behavior.getTypingDurationMs());

        assertNull(condition.getLongPauseCount());
        assertNull(behavior.getEditCount());
        assertNull(behavior.getFullDeleteCount());
        assertNull(behavior.getTypingPauseCount());
        assertNull(behavior.getAnswerReversalCount());
        assertNull(behavior.getConfusionCount());
        assertNull(behavior.getReexplanationCount());

        assertEquals(30L, response.messageId());
        assertEquals("SAVED", response.status());
    }

    @Test
    void textOnlyRequestSavesNullableMetadata() {
        VoiceAnalysisSaveRequest request = new VoiceAnalysisSaveRequest(
                "오늘 산책했어요.", null, null, null
        );
        mockSavedSessionAndMessage(request.text());

        voiceAnalysisService.save(1L, request);

        ArgumentCaptor<VoiceConditionRecord> conditionCaptor =
                ArgumentCaptor.forClass(VoiceConditionRecord.class);
        verify(voiceConditionRecordRepository).save(conditionCaptor.capture());
        assertNull(conditionCaptor.getValue().getSpeechDurationMs());
        assertNull(conditionCaptor.getValue().getSpeechRate());

        ArgumentCaptor<InteractionBehavior> behaviorCaptor =
                ArgumentCaptor.forClass(InteractionBehavior.class);
        verify(interactionBehaviorRepository).save(behaviorCaptor.capture());
        assertNull(behaviorCaptor.getValue().getResponseDelayMs());
    }

    @Test
    void missingCurrentUserFailsBeforeAnySave() {
        VoiceAnalysisSaveRequest request = new VoiceAnalysisSaveRequest(
                "테스트 발화", null, null, null
        );
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> voiceAnalysisService.save(99L, request)
        );
        verify(interactionSessionRepository, never()).save(any());
        verify(interactionMessageRepository, never()).save(any());
        verify(voiceConditionRecordRepository, never()).save(any());
        verify(interactionBehaviorRepository, never()).save(any());
    }

    private void mockSavedSessionAndMessage(String text) {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(interactionSessionRepository.save(any(InteractionSession.class)))
                .thenReturn(InteractionSession.builder()
                        .sessionId(20L)
                        .userId(1L)
                        .build());
        when(interactionMessageRepository.save(any(InteractionMessage.class)))
                .thenReturn(InteractionMessage.builder()
                        .messageId(30L)
                        .sessionId(20L)
                        .senderType(SenderType.USER)
                        .inputType(InputType.VOICE)
                        .content(text)
                        .build());
    }
}

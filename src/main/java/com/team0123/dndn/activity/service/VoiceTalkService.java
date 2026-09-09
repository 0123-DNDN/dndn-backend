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
import com.team0123.dndn.ai.dto.AiVoiceConditionRequest;
import com.team0123.dndn.interaction.entity.InputType;
import com.team0123.dndn.interaction.entity.InteractionMessage;
import com.team0123.dndn.interaction.entity.InteractionSession;
import com.team0123.dndn.interaction.entity.SenderType;
import com.team0123.dndn.interaction.repository.InteractionMessageRepository;
import com.team0123.dndn.interaction.repository.InteractionSessionRepository;
import com.team0123.dndn.user.entity.Role;
import com.team0123.dndn.user.entity.User;
import com.team0123.dndn.user.repository.UserRepository;
import com.team0123.dndn.voice.entity.VoiceConditionRecord;
import com.team0123.dndn.voice.repository.VoiceConditionRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoiceTalkService {

    private static final int REQUIRED_ANSWER_COUNT = 4;

    private final InteractionSessionRepository interactionSessionRepository;
    private final InteractionMessageRepository interactionMessageRepository;
    private final VoiceConditionRecordRepository voiceConditionRecordRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final ActivityService activityService;
    private final VoiceTalkOpeningQuestions openingQuestions;
    private final GeminiDailyTalkClient geminiDailyTalkClient;

    @Transactional
    public VoiceTalkStartResponse start(Long userId) {
        requireSenior(userId);
        requireActiveVoiceTalkActivity();

        InteractionSession session = interactionSessionRepository.save(
                InteractionSession.builder()
                        .userId(userId)
                        .build()
        );
        String firstQuestion = openingQuestions.randomQuestion();

        interactionMessageRepository.save(
                InteractionMessage.builder()
                        .sessionId(session.getSessionId())
                        .senderType(SenderType.ASSISTANT)
                        .inputType(null)
                        .content(firstQuestion)
                        .build()
        );

        return new VoiceTalkStartResponse(
                session.getSessionId(),
                firstQuestion,
                REQUIRED_ANSWER_COUNT
        );
    }

    @Transactional
    public VoiceTalkAnswerResponse answer(
            Long userId,
            Long sessionId,
            VoiceTalkAnswerRequest request
    ) {
        requireSenior(userId);
        InteractionSession session = findOwnedSession(userId, sessionId);

        if (!session.isActive()) {
            throw new IllegalArgumentException("이미 완료된 오늘 이야기 세션입니다.");
        }

        InteractionMessage answerMessage = interactionMessageRepository.save(
                InteractionMessage.builder()
                        .sessionId(sessionId)
                        .senderType(SenderType.USER)
                        .inputType(InputType.VOICE)
                        .content(request.text().trim())
                        .build()
        );
        saveVoiceCondition(answerMessage.getMessageId(), request.voiceCondition());

        List<InteractionMessage> context = interactionMessageRepository
                .findAllBySessionIdOrderByCreatedAtAscMessageIdAsc(sessionId);
        validateConversationSequence(context);

        int answeredCount = (int) context.stream()
                .filter(message -> message.getSenderType() == SenderType.USER)
                .count();

        if (answeredCount == REQUIRED_ANSWER_COUNT) {
            String summary = geminiDailyTalkClient
                    .generateClosingSummary(context)
                    .filter(message -> !message.isBlank())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Gemini 대화 요약을 생성하지 못했습니다."
                    ));

            interactionMessageRepository.save(
                    InteractionMessage.builder()
                            .sessionId(sessionId)
                            .senderType(SenderType.ASSISTANT)
                            .inputType(null)
                            .content(summary.trim())
                            .build()
            );

            completeVoiceTalk(userId, sessionId, session);
            return new VoiceTalkAnswerResponse(
                    sessionId,
                    null,
                    summary.trim(),
                    answeredCount,
                    true
            );
        }

        String nextQuestion = geminiDailyTalkClient
                .generateNextQuestion(context)
                .filter(question -> !question.isBlank())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Gemini 후속 질문을 생성하지 못했습니다."
                ));

        interactionMessageRepository.save(
                InteractionMessage.builder()
                        .sessionId(sessionId)
                        .senderType(SenderType.ASSISTANT)
                        .inputType(null)
                        .content(nextQuestion.trim())
                        .build()
        );

        return new VoiceTalkAnswerResponse(
                sessionId,
                nextQuestion.trim(),
                null,
                answeredCount,
                false
        );
    }

    private void saveVoiceCondition(
            Long messageId,
            AiVoiceConditionRequest request
    ) {
        if (request == null) {
            return;
        }

        voiceConditionRecordRepository.save(
                VoiceConditionRecord.builder()
                        .messageId(messageId)
                        .speechDurationMs(request.speechDurationMs())
                        .speechRate(request.speechRate())
                        .avgPauseDurationMs(request.avgPauseDurationMs())
                        .longPauseCount(request.normalizedLongPauseCount())
                        .build()
        );
    }

    private void completeVoiceTalk(
            Long userId,
            Long sessionId,
            InteractionSession session
    ) {
        Activity activity = requireActiveVoiceTalkActivity();

        activityService.saveResult(
                userId,
                activity.getActivityId(),
                new ActivityResultSaveRequest(
                        null,
                        null,
                        sessionId,
                        ActivityStatus.COMPLETED
                )
        );
        session.end();
    }

    private void validateConversationSequence(
            List<InteractionMessage> messages
    ) {
        if (messages == null
                || messages.isEmpty()
                || messages.size() > REQUIRED_ANSWER_COUNT * 2) {
            throw new IllegalArgumentException("올바른 오늘 이야기 대화가 아닙니다.");
        }

        for (int index = 0; index < messages.size(); index++) {
            InteractionMessage message = messages.get(index);
            SenderType expected = index % 2 == 0
                    ? SenderType.ASSISTANT
                    : SenderType.USER;

            if (message.getSenderType() != expected) {
                throw new IllegalArgumentException("대화 순서가 올바르지 않습니다.");
            }
        }

        InteractionMessage firstMessage = messages.get(0);
        if (!openingQuestions.contains(firstMessage.getContent())) {
            throw new IllegalArgumentException("오늘 이야기 대화 세션이 아닙니다.");
        }

        InteractionMessage lastMessage = messages.get(messages.size() - 1);
        if (lastMessage.getSenderType() != SenderType.USER) {
            throw new IllegalArgumentException("사용자 답변 저장 상태가 올바르지 않습니다.");
        }
    }

    private InteractionSession findOwnedSession(
            Long userId,
            Long sessionId
    ) {
        if (sessionId == null) {
            throw new IllegalArgumentException("대화 세션 ID가 필요합니다.");
        }

        InteractionSession session = interactionSessionRepository
                .findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 대화 세션입니다."
                ));

        if (!userId.equals(session.getUserId())) {
            throw new IllegalArgumentException(
                    "다른 사용자의 대화 세션에 접근할 수 없습니다."
            );
        }
        return session;
    }

    private Activity requireActiveVoiceTalkActivity() {
        return activityRepository
                .findFirstByActivityTypeAndIsActiveTrue(ActivityType.VOICE_TALK)
                .orElseThrow(() -> new IllegalArgumentException(
                        "활성화된 VOICE_TALK 챌린지를 찾을 수 없습니다."
                ));
    }

    private User requireSenior(Long userId) {
        User user = userId == null
                ? null
                : userRepository.findById(userId).orElse(null);

        if (user == null) {
            throw new IllegalArgumentException("현재 로그인 사용자를 찾을 수 없습니다.");
        }
        if (user.getRole() != Role.SENIOR) {
            throw new IllegalArgumentException(
                    "시니어 사용자만 오늘 이야기를 진행할 수 있습니다."
            );
        }
        return user;
    }
}

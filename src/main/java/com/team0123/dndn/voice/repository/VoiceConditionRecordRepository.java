package com.team0123.dndn.voice.repository;

import com.team0123.dndn.voice.entity.VoiceConditionRecord;
import com.team0123.dndn.voice.dto.VoiceDetailSample;
import com.team0123.dndn.voice.dto.VoiceWeeklySample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VoiceConditionRecordRepository extends JpaRepository<VoiceConditionRecord, Long> {

    @Query("""
            SELECT new com.team0123.dndn.voice.dto.VoiceWeeklySample(
                    voice.speechRate,
                    voice.avgPauseDurationMs,
                    message.content
            )
            FROM VoiceConditionRecord voice,
                 InteractionMessage message,
                 InteractionSession session
            WHERE message.messageId = voice.messageId
              AND session.sessionId = message.sessionId
              AND session.userId = :userId
              AND voice.measuredAt >= :start
              AND voice.measuredAt < :endExclusive
            ORDER BY voice.measuredAt ASC, voice.voiceConditionId ASC
            """)
    List<VoiceWeeklySample> findWeeklySamplesByUserId(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("endExclusive") LocalDateTime endExclusive
    );

    @Query("""
            SELECT new com.team0123.dndn.voice.dto.VoiceDetailSample(
                    voice.measuredAt,
                    session.sessionId,
                    voice.speechRate,
                    voice.avgPauseDurationMs,
                    message.content
            )
            FROM VoiceConditionRecord voice,
                 InteractionMessage message,
                 InteractionSession session
            WHERE message.messageId = voice.messageId
              AND session.sessionId = message.sessionId
              AND session.userId = :userId
              AND voice.measuredAt >= :start
              AND voice.measuredAt < :endExclusive
            ORDER BY voice.measuredAt ASC, voice.voiceConditionId ASC
            """)
    List<VoiceDetailSample> findDetailSamplesByUserId(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("endExclusive") LocalDateTime endExclusive
    );
}

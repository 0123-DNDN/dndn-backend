package com.team0123.dndn.interaction.repository;

import com.team0123.dndn.interaction.entity.InteractionMessage;
import com.team0123.dndn.interaction.entity.SenderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface InteractionMessageRepository extends JpaRepository<InteractionMessage, Long> {

    List<InteractionMessage> findAllBySessionIdOrderByCreatedAtAscMessageIdAsc(
            Long sessionId
    );

    @Query("""
            SELECT DISTINCT message.sessionId
            FROM InteractionMessage message
            WHERE message.sessionId IN :sessionIds
              AND message.senderType = :senderType
            """)
    List<Long> findSessionIdsContainingSenderType(
            @Param("sessionIds") Collection<Long> sessionIds,
            @Param("senderType") SenderType senderType
    );
}

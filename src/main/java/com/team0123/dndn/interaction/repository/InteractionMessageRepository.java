package com.team0123.dndn.interaction.repository;

import com.team0123.dndn.interaction.entity.InteractionMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InteractionMessageRepository extends JpaRepository<InteractionMessage, Long> {

    List<InteractionMessage> findAllBySessionIdOrderByCreatedAtAscMessageIdAsc(
            Long sessionId
    );
}

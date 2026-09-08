package com.team0123.dndn.interaction.repository;

import com.team0123.dndn.interaction.entity.InteractionSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InteractionSessionRepository extends JpaRepository<InteractionSession, Long> {
}

package com.team0123.dndn.family.repository;

import com.team0123.dndn.family.entity.GuardianRelationship;
import com.team0123.dndn.family.entity.GuardianRelationshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuardianRelationshipRepository
        extends JpaRepository<GuardianRelationship, Long> {

    Optional<GuardianRelationship> findBySeniorUserIdAndStatus(
            Long seniorUserId,
            GuardianRelationshipStatus status
    );

    Optional<GuardianRelationship> findByGuardianUserIdAndStatus(
            Long guardianUserId,
            GuardianRelationshipStatus status
    );

    Optional<GuardianRelationship> findBySeniorUserIdAndGuardianUserId(
            Long seniorUserId,
            Long guardianUserId
    );

    boolean existsBySeniorUserIdAndGuardianUserId(
            Long seniorUserId,
            Long guardianUserId
    );
}
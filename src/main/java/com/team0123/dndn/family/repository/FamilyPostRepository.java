package com.team0123.dndn.family.repository;

import com.team0123.dndn.family.entity.FamilyPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FamilyPostRepository extends JpaRepository<FamilyPost, Long> {

    List<FamilyPost> findAllByRelationshipIdAndTargetDateOrderByCreatedAtAscFamilyPostIdAsc(
            Long relationshipId,
            LocalDate targetDate
    );
}

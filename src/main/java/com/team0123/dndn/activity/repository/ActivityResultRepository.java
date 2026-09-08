package com.team0123.dndn.activity.repository;

import com.team0123.dndn.activity.entity.ActivityResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ActivityResultRepository extends JpaRepository<ActivityResult, Long> {

    Optional<ActivityResult> findBySeniorUserIdAndActivityIdAndActivityDate(
            Long seniorUserId,
            Long activityId,
            LocalDate activityDate
    );

    List<ActivityResult> findAllBySeniorUserIdAndActivityDate(
            Long seniorUserId,
            LocalDate activityDate
    );
}

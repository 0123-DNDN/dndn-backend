package com.team0123.dndn.activity.repository;

import com.team0123.dndn.activity.entity.Activity;
import com.team0123.dndn.activity.entity.ActivityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    List<Activity> findAllByIsActiveTrueOrderByDisplayOrderAsc();

    Optional<Activity> findFirstByActivityTypeAndIsActiveTrue(
            ActivityType activityType
    );
}

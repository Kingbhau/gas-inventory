package com.gasagency.repository;

import com.gasagency.entity.AlertNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlertNotificationRepository extends JpaRepository<AlertNotification, Long> {
    List<AlertNotification> findByIsDismissedFalseAndExpiresAtGreaterThan(LocalDateTime now);

    Optional<AlertNotification> findByAlertKey(String alertKey);

    void deleteByExpiresAtLessThan(LocalDateTime now);
}

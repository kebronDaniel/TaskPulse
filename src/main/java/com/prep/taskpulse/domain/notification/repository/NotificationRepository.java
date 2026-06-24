package com.prep.taskpulse.domain.notification.repository;

import com.prep.taskpulse.domain.notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Optional<Notification> findByIdAndOwnerId(UUID id, UUID ownerId);
    List<Notification> findByOwnerId(UUID ownerId);
    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);
}

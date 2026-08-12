package com.prep.taskpulse.domain.notification.repository;

import com.prep.taskpulse.domain.notification.Notification;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
  Optional<Notification> findByIdAndRecipientId(UUID id, UUID recipientId);

  List<Notification> findByRecipientId(UUID recipientId);

  boolean existsByIdAndRecipientId(UUID id, UUID recipientId);
}

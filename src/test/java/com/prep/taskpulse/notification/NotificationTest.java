package com.prep.taskpulse.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.prep.taskpulse.domain.notification.Notification;
import com.prep.taskpulse.domain.notification.NotificationType;
import com.prep.taskpulse.domain.user.Role;
import com.prep.taskpulse.domain.user.User;
import org.junit.jupiter.api.Test;

class NotificationTest {

  @Test
  void create_whenValuesAreValid_createsUnreadNotification() {
    User recipient =
        User.createUser("Test User", "test@example.com", "encoded-password", Role.USER);

    Notification notification =
        Notification.create("Task assigned to you", recipient, NotificationType.TASK_ASSIGNED);

    assertThat(notification.getMessage()).isEqualTo("Task assigned to you");
    assertThat(notification.getRecipient()).isSameAs(recipient);
    assertThat(notification.getType()).isEqualTo(NotificationType.TASK_ASSIGNED);
    assertThat(notification.isRead()).isFalse();

    notification.markAsRead();
    notification.markAsRead();

    assertThat(notification.isRead()).isTrue();
  }
}

package com.prep.taskpulse.domain.notification;

import com.prep.taskpulse.domain.common.BaseEntity;
import com.prep.taskpulse.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @Column(nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type = NotificationType.TASK_CREATED;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(nullable = false)
    private boolean read = false;

    private Notification(String message, User recipient, NotificationType type) {

        if ((message == null) || (message.isBlank())) throw new IllegalArgumentException("Notification message must not be null");
        if (recipient == null) throw new IllegalArgumentException("Recipient must not be null");
        if (type == null) this.type = NotificationType.TASK_CREATED;

        this.message = message;
        this.recipient = recipient;
        this.type = type;
    }

    public static Notification create(String message, User recipient, NotificationType type){
        return new Notification(message,recipient,type);
    }

    public void markAsRead(){
        if (!this.read) this.read=true;
    }
}

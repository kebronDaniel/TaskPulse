package com.prep.taskpulse.domain.common;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.Hibernate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    @LastModifiedDate
    private Instant updatedAt;

    @Version
    private Long version;

    @Override
    public final boolean equals(Object o) {
        // Task proxy != Task actual class
        if (this == o) return true;
        // check if two objects are not different(user and Task or ..., they have to be the same type)
        // This means peeling off of the proxy layer and getting the DB objects.
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        BaseEntity that = (BaseEntity) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        // To generate a constant number for every instance.
        // this causes hash collision but its handled by the .equals which is clearly implemented.
        return getClass().hashCode();
    }
}

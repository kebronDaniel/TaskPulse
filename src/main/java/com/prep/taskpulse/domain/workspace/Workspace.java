package com.prep.taskpulse.domain.workspace;

import com.prep.taskpulse.domain.common.BaseEntity;
import com.prep.taskpulse.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "workspaces")
public class Workspace extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    private Workspace(String name, User owner) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Workspace name must not be blank");
        if (owner == null) throw new IllegalArgumentException("Workspace owner name must not be null");
        this.name = name;
        this.owner = owner;
    }

    public static Workspace create(String name, User owner){
        return new Workspace(name,owner);
    }

    public void rename(String name){
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Workspace name must not be blank");
        this.name = name;
    }

}

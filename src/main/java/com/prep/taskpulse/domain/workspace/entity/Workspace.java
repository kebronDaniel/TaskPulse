package com.prep.taskpulse.domain.workspace.entity;

import com.prep.taskpulse.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "workspaces")
public class Workspace extends BaseEntity {
    @NotBlank
    @Column(nullable = false)
    private String name;

    private Workspace(String name){
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Workspace name must not be blank");
        this.name = name;
    }

    public static Workspace create(String name){
        return new Workspace(name);
    }

    public void rename(String name){
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Workspace name must not be blank");
        this.name = name;
    }

}

package com.prep.taskpulse.domain.project;

import com.prep.taskpulse.domain.common.BaseEntity;
import com.prep.taskpulse.domain.workspace.Workspace;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "projects")
public class Project extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;
    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    private Project(String name, String description, Workspace workspace) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name must not be null");
        if (workspace == null) throw new IllegalArgumentException("Workspace must not be null");
        this.name = name;
        this.description = description;
        this.workspace = workspace;
    }

    public static Project create(String name, String description, Workspace workspace){
        return new Project(name,description,workspace);
    }

    public void rename(String name){
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name must not be null");
        this.name = name;
    }

    public void changeDescription(String description){
        this.description = description;
    }
}

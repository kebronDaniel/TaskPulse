package com.prep.taskpulse.domain.task.entity;

import com.prep.taskpulse.domain.common.BaseEntity;
import com.prep.taskpulse.domain.task.enums.TaskPriority;
import com.prep.taskpulse.domain.task.enums.TaskStatus;
import com.prep.taskpulse.domain.workspace.entity.Workspace;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tasks")
public class Task extends BaseEntity {

    private Task(String title, String description, TaskStatus taskStatus, TaskPriority taskPriority, Workspace workspace) {

        if (title == null || title.isBlank()) throw new IllegalArgumentException("Title must not be blank");
        if (taskStatus == null) throw new IllegalArgumentException("Task status must not be null");
        if (taskPriority == null) throw new IllegalArgumentException("Task priority must not be null");
        if (workspace == null) throw new IllegalArgumentException("Workspace must not be null");

        this.title = title;
        this.description = description;
        this.taskStatus = taskStatus;
        this.taskPriority = taskPriority;
        this.workspace = workspace;
    }

    @Column(nullable = false)
    @NotBlank
    private String title;
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus taskStatus;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskPriority taskPriority;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id",nullable = false)
    private Workspace workspace;

    public void changeTitle(String title) {
        if (title == null || title.isBlank()){
            //TODO: throw custom and specific exception
            throw new IllegalArgumentException("Task title must not be blank");
        }
        this.title = title;
    }

    public static Builder builder(){
        return new Builder();
    }

    public static class Builder{
        private String title;
        private String description;
        private TaskStatus taskStatus;
        private TaskPriority taskPriority;
        private Workspace workspace;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder taskStatus(TaskStatus taskStatus) {
            this.taskStatus = taskStatus;
            return this;
        }

        public Builder taskPriority(TaskPriority taskPriority) {
            this.taskPriority = taskPriority;
            return this;
        }

        public Builder workspace(Workspace workspace) {
            this.workspace = workspace;
            return this;
        }

        public Task build() {
            return new Task(
                    title,
                    description,
                    taskStatus,
                    taskPriority,
                    workspace
            );
        }
    }
}

package com.prep.taskpulse.domain.task;

import com.prep.taskpulse.domain.common.BaseEntity;
import com.prep.taskpulse.domain.project.Project;
import com.prep.taskpulse.domain.task.enums.TaskPriority;
import com.prep.taskpulse.domain.task.enums.TaskStatus;
import com.prep.taskpulse.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;


@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tasks")
public class Task extends BaseEntity {

    private Task(String title, String description, Project project, TaskPriority priority, Instant dueDate) {

        if (title == null || title.isBlank()) throw new IllegalArgumentException("Title must not be blank");
        if (project == null) throw new IllegalArgumentException("Project must not be null");

        this.title = title;
        this.description = description;
        this.project = project;
        this.priority = priority != null? priority : TaskPriority.MEDIUM;
        this.dueDate = dueDate;
    }

    @Column(nullable = false)
    private String title;
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.TODO;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskPriority priority = TaskPriority.MEDIUM;
    private Instant dueDate;
    private Instant deletedAt;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id",nullable = false)
    private Project project;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    public static Task create(String title, String description, Project project, TaskPriority priority, Instant dueDate){
        return new Task(title,description,project,priority,dueDate);
    }

    public void changeStatus(TaskStatus status){
        if (status == null) throw new IllegalArgumentException("Status must not be null");
        if (this.status == TaskStatus.DONE && status == TaskStatus.TODO) throw new IllegalArgumentException("Completed task cannot be reopened directly");
        this.status = status;
    }

    public void assignTo(User assignee){
        if (assignee == null) throw new IllegalArgumentException("Assignee must not be null");
        this.assignee = assignee;
    }

    public void changePriority(TaskPriority priority){
        if (priority == null) throw new IllegalArgumentException("Priority must not be null");
        this.priority = priority;
    }

    public void reschedule(Instant dueDate){
        this.dueDate = dueDate;
    }

    public void rename(String title){
        if ((title == null) || (title.isBlank())) throw new IllegalArgumentException("Title must not be null.");
        this.title = title;
    }

    public void unassign(){
        this.assignee = null;
    }

    public void changeDescription(String description){
        this.description = description;
    }

    public void softDelete(){
        if (this.deletedAt != null) throw new IllegalArgumentException("Task is already deleted");
        this.deletedAt = Instant.now();
    }
}

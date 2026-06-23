package com.prep.taskpulse.domain.comment;

import com.prep.taskpulse.domain.common.BaseEntity;
import com.prep.taskpulse.domain.task.Task;
import com.prep.taskpulse.domain.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "comments")
public class Comment extends BaseEntity {

    @Column(nullable = false, length = 500)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    private Comment(String content, Task task, User author) {

        if ((content == null) || (content.isBlank())) throw new IllegalArgumentException("Comment Content must not be null");
        if (task == null) throw new IllegalArgumentException("task must not be null");
        if (author == null) throw new IllegalArgumentException("author must not be null");

        this.content = content;
        this.task = task;
        this.author = author;
    }

    public static Comment create(String content, Task task, User author){
        return new Comment(content, task, author);
    }

    public void editContent(String content){
        if ((content == null) || (content.isBlank())) throw new IllegalArgumentException("Content must not be null");
        this.content = content;
    }
}

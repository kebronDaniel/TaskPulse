package com.prep.taskpulse.domain.task.repository;

import com.prep.taskpulse.domain.task.Task;
import com.prep.taskpulse.domain.task.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

// uses specifications : it allows dynamic filters(optional and composable)
public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {
    Page<Task> findByTaskStatus(TaskStatus taskStatus, Pageable pageable);
    // relationship traversal
    List<Task> findByWorkspace_Name(String workspaceName);
    // combining filters
    List<Task> findByWorkspace_NameAndTaskStatus(String workspaceName, TaskStatus taskStatus);
}

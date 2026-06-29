package com.prep.taskpulse.domain.task.repository;

import com.prep.taskpulse.domain.task.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    Optional<Task> findByIdAndProjectIdAndDeletedAtIsNull(UUID id, UUID projectId);
    Page<Task> findByProjectIdAndDeletedAtIsNull(UUID projectId, Pageable pageable);
    boolean existsByIdAndProjectIdAndDeletedAtIsNull(UUID id, UUID projectId);
}

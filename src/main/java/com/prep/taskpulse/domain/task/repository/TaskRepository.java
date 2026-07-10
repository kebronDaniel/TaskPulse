package com.prep.taskpulse.domain.task.repository;

import com.prep.taskpulse.domain.task.Task;
import com.prep.taskpulse.domain.task.dto.TaskSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {
    @EntityGraph(attributePaths = {"project","assignee"})
    Optional<Task> findWithProjectAndAssigneeByIdAndProjectIdAndDeletedAtIsNull(UUID id, UUID projectId);
    Optional<Task> findByIdAndProjectIdAndDeletedAtIsNull(UUID id, UUID projectId);
    @EntityGraph(attributePaths = {"project", "assignee"})
    Page<Task> findByProjectIdAndDeletedAtIsNull(UUID projectId, Pageable pageable);
    boolean existsByIdAndProjectIdAndDeletedAtIsNull(UUID id, UUID projectId);

    // DTO projection (join fetch without the fetch because you don't need to fetch and process project or user)
    @Query("""
            select new com.prep.taskpulse.domain.task.dto.TaskSummaryResponse(
                        t.id,t.title,t.priority,t.dueDate,p.name,a.email
                        )
            from Task t
            join t.project p
            left join t.assignee a
            where p.id = :projectId
            and t.deletedAt is null
            """)
    Page<TaskSummaryResponse> findTaskSummariesByProjectId(UUID projectId, Pageable pageable);
}

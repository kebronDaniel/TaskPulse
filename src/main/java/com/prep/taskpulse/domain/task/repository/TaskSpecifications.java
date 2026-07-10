package com.prep.taskpulse.domain.task.repository;

import com.prep.taskpulse.domain.task.Task;
import com.prep.taskpulse.domain.task.dto.TaskSearchCriteria;
import com.prep.taskpulse.domain.task.enums.TaskPriority;
import com.prep.taskpulse.domain.task.enums.TaskStatus;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

public final class TaskSpecifications {

    private TaskSpecifications() {
    }

    public static Specification<Task> belongsToProject(UUID projectId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("project").get("id"), projectId);
    }

    public static Specification<Task> hasPriority(TaskPriority priority){
        if (priority == null) return null;
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("priority"), priority);
    }

    public static Specification<Task> hasStatus(TaskStatus status){
        if (status == null) return null;
        return ((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), status));
    }

    public static Specification<Task> assignedTo(UUID assigneeId){
        if (assigneeId == null) return null;
        return ((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("assignee").get("id"), assigneeId));
    }

    public static Specification<Task> dueBefore(Instant dueDate){
        if (dueDate == null) return null;
        return ((root, query, criteriaBuilder) ->
                criteriaBuilder.lessThan(root.get("dueDate"), dueDate));
    }

    public static Specification<Task> createdAfter(Instant createdAt){
        if (createdAt == null) return null;
        return ((root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThan(root.get("createdAt"), createdAt));
    }

    public static Specification<Task> isNotDeleted(){
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.isNull(root.get("deletedAt"));
    }

    public static @NonNull Specification<Task> fromCriteria(UUID projectId, TaskSearchCriteria criteria) {
        Specification<Task> specification = TaskSpecifications.belongsToProject(projectId)
                .and(TaskSpecifications.hasPriority(criteria.priority()))
                .and(TaskSpecifications.hasStatus(criteria.status()))
                .and(TaskSpecifications.assignedTo(criteria.assigneeId()))
                .and(TaskSpecifications.createdAfter(criteria.createdAfter()))
                .and(TaskSpecifications.dueBefore(criteria.beforeDue()))
                .and(TaskSpecifications.isNotDeleted());
        return specification;
    }
}

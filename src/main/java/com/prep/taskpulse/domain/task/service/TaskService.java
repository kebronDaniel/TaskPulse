package com.prep.taskpulse.domain.task.service;

import com.prep.taskpulse.config.CacheConfig;
import com.prep.taskpulse.domain.project.Project;
import com.prep.taskpulse.domain.project.repository.ProjectRepository;
import com.prep.taskpulse.domain.task.Task;
import com.prep.taskpulse.domain.task.TaskEvent;
import com.prep.taskpulse.domain.task.dto.*;
import com.prep.taskpulse.domain.task.enums.TaskEventType;
import com.prep.taskpulse.domain.task.mapper.TaskMapper;
import com.prep.taskpulse.domain.task.repository.TaskRepository;
import com.prep.taskpulse.domain.task.repository.TaskSpecifications;
import com.prep.taskpulse.exception.ProjectNotFoundException;
import com.prep.taskpulse.exception.StaleTaskVersionException;
import com.prep.taskpulse.exception.TaskNotFoundException;
import com.prep.taskpulse.outbox.service.OutboxService;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {
  private final TaskRepository taskRepository;
  private final ProjectRepository projectRepository;
  private final TaskMapper taskMapper;
  private final OutboxService outboxService;
  private final MeterRegistry meterRegistry;

  @Transactional
  public TaskResponse createTask(UUID workspaceId, UUID projectId, CreateTaskRequest request) {

    Project project =
        projectRepository
            .findByIdAndWorkspaceId(projectId, workspaceId)
            .orElseThrow(() -> new ProjectNotFoundException(projectId));

    Task task =
        Task.create(
            request.title(), request.description(), project, request.priority(), request.dueDate());
    Task savedTask = taskRepository.save(task);
    TaskEvent taskEvent =
        new TaskEvent(
            UUID.randomUUID(),
            TaskEventType.CREATED,
            savedTask.getId(),
            project.getId(),
            workspaceId,
            savedTask.getAssignee() != null ? savedTask.getAssignee().getId() : null,
            Instant.now());
    outboxService.save(taskEvent);
    meterRegistry.counter("taskflow.tasks.created").increment();
    return taskMapper.toResponse(savedTask);
  }

  @Cacheable(cacheNames = CacheConfig.TASK_CACHE, key = "#taskId", sync = true)
  public TaskResponse getTask(UUID workspaceId, UUID projectId, UUID taskId) {
    projectRepository
        .findByIdAndWorkspaceId(projectId, workspaceId)
        .orElseThrow(() -> new ProjectNotFoundException(projectId));

    Task savedTask =
        taskRepository
            .findWithProjectAndAssigneeByIdAndProjectIdAndDeletedAtIsNull(taskId, projectId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));
    return taskMapper.toResponse(savedTask);
  }

  @CacheEvict(cacheNames = CacheConfig.TASK_CACHE, key = "#taskId")
  @Transactional
  public TaskResponse updateTask(
      UUID workspaceId, UUID projectId, UUID taskId, UpdateTaskRequest request) {
    projectRepository
        .findByIdAndWorkspaceId(projectId, workspaceId)
        .orElseThrow(() -> new ProjectNotFoundException(projectId));

    Task task =
        taskRepository
            .findByIdAndProjectIdAndDeletedAtIsNull(taskId, projectId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));

    if (!Objects.equals(request.version(), task.getVersion()))
      throw new StaleTaskVersionException(taskId, request.version(), task.getVersion());

    if (request.title() != null) task.rename(request.title());
    if (request.description() != null) task.changeDescription(request.description());
    if (request.priority() != null) task.changePriority(request.priority());
    if (request.dueDate() != null) task.reschedule(request.dueDate());

    taskRepository.flush();

    TaskEvent taskEvent =
        new TaskEvent(
            UUID.randomUUID(),
            TaskEventType.UPDATED,
            task.getId(),
            projectId,
            workspaceId,
            task.getAssignee() != null ? task.getAssignee().getId() : null,
            Instant.now());
    outboxService.save(taskEvent);

    return taskMapper.toResponse(task);
  }

  @CacheEvict(cacheNames = CacheConfig.TASK_CACHE, key = "#taskId")
  @Transactional
  public void deleteTask(UUID workspaceId, UUID projectId, UUID taskId) {
    projectRepository
        .findByIdAndWorkspaceId(projectId, workspaceId)
        .orElseThrow(() -> new ProjectNotFoundException(projectId));

    Task task =
        taskRepository
            .findByIdAndProjectIdAndDeletedAtIsNull(taskId, projectId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));

    task.deleteTask();
  }

  public Page<TaskResponse> getTasksByProject(UUID workspaceId, UUID projectId, Pageable pageable) {
    projectRepository
        .findByIdAndWorkspaceId(projectId, workspaceId)
        .orElseThrow(() -> new ProjectNotFoundException(projectId));
    Page<Task> tasks = taskRepository.findByProjectIdAndDeletedAtIsNull(projectId, pageable);
    return tasks.map(taskMapper::toResponse);
  }

  public Page<TaskSummaryResponse> getTaskSummariesByProject(
      UUID workspaceId, UUID projectId, Pageable pageable) {
    projectRepository
        .findByIdAndWorkspaceId(projectId, workspaceId)
        .orElseThrow(() -> new ProjectNotFoundException(projectId));
    return taskRepository.findTaskSummariesByProjectId(projectId, pageable);
  }

  @Timed(
      value = "taskflow.tasks.search",
      description = "task service search latency",
      percentiles = {
        0.5, 0.95, 0.99
      }, // takes values at 50th, 95th and 99th and use them as boundary values.
      histogram = true)
  public Page<TaskResponse> searchTasks(
      UUID workspaceId, UUID projectId, TaskSearchCriteria criteria, Pageable pageable) {

    projectRepository
        .findByIdAndWorkspaceId(projectId, workspaceId)
        .orElseThrow(() -> new ProjectNotFoundException(projectId));

    Specification<Task> specification = TaskSpecifications.fromCriteria(projectId, criteria);

    Page<Task> tasks = taskRepository.findAll(specification, pageable);
    return tasks.map(taskMapper::toResponse);
  }
}

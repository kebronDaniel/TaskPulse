package com.prep.taskpulse.domain.task.service;

import com.prep.taskpulse.domain.project.Project;
import com.prep.taskpulse.domain.project.repository.ProjectRepository;
import com.prep.taskpulse.domain.task.Task;
import com.prep.taskpulse.domain.task.dto.*;
import com.prep.taskpulse.domain.task.mapper.TaskMapper;
import com.prep.taskpulse.domain.task.repository.TaskRepository;
import com.prep.taskpulse.domain.task.repository.TaskSpecifications;
import com.prep.taskpulse.exception.ProjectNotFoundException;
import com.prep.taskpulse.exception.TaskNotFoundException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TaskMapper taskMapper;

    @Transactional
    public TaskResponse createTask(UUID workspaceId, UUID projectId, CreateTaskRequest request){

        Project project = projectRepository.findByIdAndWorkspaceId(projectId,workspaceId).
                orElseThrow(() -> new ProjectNotFoundException(projectId));

        Task task = Task.create(request.title(), request.description(),project,request.priority(),request.dueDate());
        Task savedTask = taskRepository.save(task);
        return taskMapper.toResponse(savedTask);
    }

    public TaskResponse getTask(UUID workspaceId, UUID projectId, UUID taskId){
        projectRepository.findByIdAndWorkspaceId(projectId, workspaceId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        Task savedTask = taskRepository.findWithProjectAndAssigneeByIdAndProjectIdAndDeletedAtIsNull(taskId,projectId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        return taskMapper.toResponse(savedTask);
    }

    @Transactional
    public TaskResponse updateTask(UUID workspaceId, UUID projectId, UUID taskId,UpdateTaskRequest request){
        projectRepository.findByIdAndWorkspaceId(projectId,workspaceId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        Task task = taskRepository.findByIdAndProjectIdAndDeletedAtIsNull(taskId, projectId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        if (request.title() != null) task.rename(request.title());
        if (request.description() != null) task.changeDescription(request.description());
        if (request.priority() != null) task.changePriority(request.priority());
        if (request.dueDate() != null) task.reschedule(request.dueDate());

        return taskMapper.toResponse(task);
    }

    @Transactional
    public void deleteTask(UUID workspaceId, UUID projectId, UUID taskId){
        projectRepository.findByIdAndWorkspaceId(projectId,workspaceId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        Task task = taskRepository.findByIdAndProjectIdAndDeletedAtIsNull(taskId, projectId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        task.deleteTask();
    }

    public Page<TaskResponse> getTasksByProject(UUID workspaceId, UUID projectId, Pageable pageable){
        Page<Task> tasks = taskRepository.findByProjectIdAndDeletedAtIsNull(projectId,pageable);
        return tasks.map(taskMapper::toResponse);
    }

    public Page<TaskSummaryResponse> getTaskSummariesByProject(UUID workspaceId,UUID projectId,Pageable pageable){
        projectRepository.findByIdAndWorkspaceId(projectId,workspaceId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        return taskRepository.findTaskSummariesByProjectId(projectId,pageable);
    }

    public Page<TaskResponse> searchTasks(UUID workspaceId, UUID projectId,
                                          TaskSearchCriteria criteria, Pageable pageable){

        projectRepository.findByIdAndWorkspaceId(projectId,workspaceId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        Specification<Task> specification = TaskSpecifications.fromCriteria(projectId, criteria);

        Page<Task> tasks = taskRepository.findAll(specification,pageable);
        return tasks.map(taskMapper::toResponse);
    }


}

package com.prep.taskpulse.domain.task.service;

import com.prep.taskpulse.domain.project.Project;
import com.prep.taskpulse.domain.project.repository.ProjectRepository;
import com.prep.taskpulse.domain.task.Task;
import com.prep.taskpulse.domain.task.dto.CreateTaskRequest;
import com.prep.taskpulse.domain.task.dto.TaskResponse;
import com.prep.taskpulse.domain.task.repository.TaskRepository;
import com.prep.taskpulse.exception.ProjectNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;

    @Transactional
    public TaskResponse createTask(UUID projectId, UUID workspaceId, CreateTaskRequest request){

        Project project = projectRepository.findByIdAndWorkspaceId(projectId,workspaceId).
                orElseThrow(() -> new ProjectNotFoundException(projectId));

        Task task = Task.create(request.title(), request.description(),project,request.priority(),request.dueDate());
        Task savedTask = taskRepository.save(task);
        return new TaskResponse(savedTask.getId());
    }

}

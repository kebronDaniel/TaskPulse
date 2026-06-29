package com.prep.taskpulse.domain.task.controller;

import com.prep.taskpulse.domain.task.dto.CreateTaskRequest;
import com.prep.taskpulse.domain.task.dto.TaskResponse;
import com.prep.taskpulse.domain.task.dto.UpdateTaskRequest;
import com.prep.taskpulse.domain.task.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/projects/{projectId}/tasks")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@PathVariable UUID workspaceId, @PathVariable UUID projectId,
                                                  @Valid @RequestBody CreateTaskRequest taskRequest){
        TaskResponse response = taskService.createTask(workspaceId, projectId,taskRequest);
        URI location = URI.create(
          "/api/v1/workspaces/" + workspaceId + "/projects/" + projectId + "/tasks/" + response.id()
        );
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> getTask(@PathVariable UUID workspaceId, @PathVariable UUID projectId,
                                @PathVariable UUID taskId){
        TaskResponse taskResponse = taskService.getTask(workspaceId,projectId,taskId);
        return ResponseEntity.ok(taskResponse);
    }

    @PatchMapping("/{taskId}")
    public ResponseEntity<TaskResponse> updateTask(@PathVariable UUID workspaceId, @PathVariable UUID projectId,
                                             @PathVariable UUID taskId, @Valid @RequestBody UpdateTaskRequest request){
        TaskResponse taskResponse = taskService.updateTask(workspaceId,projectId,taskId,request);
        return ResponseEntity.ok(taskResponse);
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable UUID workspaceId, @PathVariable UUID projectId,
                                           @PathVariable UUID taskId){
        taskService.deleteTask(workspaceId,projectId,taskId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<TaskResponse>> getTasks(@PathVariable UUID workspaceId, @PathVariable UUID projectId,
                                                       Pageable pageable){
        Page<TaskResponse> taskResponses = taskService.getTasksByProject(workspaceId,projectId,pageable);
        return ResponseEntity.ok(taskResponses);
    }
}

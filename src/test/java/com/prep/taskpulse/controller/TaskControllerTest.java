package com.prep.taskpulse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prep.taskpulse.domain.task.controller.TaskController;
import com.prep.taskpulse.domain.task.dto.CreateTaskRequest;
import com.prep.taskpulse.domain.task.dto.TaskResponse;
import com.prep.taskpulse.domain.task.dto.UpdateTaskRequest;
import com.prep.taskpulse.domain.task.enums.TaskPriority;
import com.prep.taskpulse.domain.task.enums.TaskStatus;
import com.prep.taskpulse.domain.task.service.TaskService;
import com.prep.taskpulse.exception.ProjectNotFoundException;
import com.prep.taskpulse.exception.TaskNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
public class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    private UUID mockWorkspaceUUID;
    private UUID mockProjectUUID;
    private UUID mockTaskUUID;
    private String baseUrl;

    @BeforeEach
    void setup(){
        mockWorkspaceUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174111");
        mockProjectUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174222");
        mockTaskUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174333");
        baseUrl="/api/v1/workspaces/{workspaceId}/projects/{projectId}/tasks";

    }


    @Test
    @WithMockUser
    void createTask_whenRequestIsValid_returns201Created() throws Exception {

        Instant dueDate = Instant.now().plus(Duration.ofDays(2));
        CreateTaskRequest request = new CreateTaskRequest("mock title","mock description",
                TaskPriority.MEDIUM, dueDate);
        TaskResponse response = new TaskResponse(mockTaskUUID, request.title(), request.description(),
                TaskStatus.TODO,request.priority(),dueDate);

        Mockito.when(taskService.createTask(mockWorkspaceUUID,mockProjectUUID,request)).thenReturn(response);

        mockMvc.perform(post(baseUrl,mockWorkspaceUUID,mockProjectUUID).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(response.id().toString()))
                .andExpect(jsonPath("$.title").value(response.title()))
                .andExpect(jsonPath("$.description").value(response.description()))
                .andExpect(jsonPath("$.status").value(response.status().toString()))
                .andExpect(jsonPath("$.priority").value(response.priority().toString()))
                .andExpect(jsonPath("$.dueDate").value(response.dueDate().toString()));

        verify(taskService).createTask(mockWorkspaceUUID,mockProjectUUID,request);
    }

    @Test
    @WithMockUser
    void createTask_whenProjectDoesNotExist_returns400NotFound() throws Exception{

        UUID mockTaskUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174333");
        Instant dueDate = Instant.now().plus(Duration.ofDays(2));
        CreateTaskRequest request = new CreateTaskRequest("mock title","mock description",
                TaskPriority.MEDIUM, dueDate);

        String path = "/api/v1/workspaces/" + mockWorkspaceUUID +"/projects/"+ mockProjectUUID+"/tasks";

        ProjectNotFoundException exception = new ProjectNotFoundException(mockProjectUUID);
        Mockito.when(taskService.createTask(mockWorkspaceUUID,mockProjectUUID,request)).thenThrow(new ProjectNotFoundException(mockProjectUUID));
        mockMvc.perform(post(baseUrl,mockWorkspaceUUID,mockProjectUUID).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(exception.getMessage()))
                .andExpect(jsonPath("$.path").value(path));

        verify(taskService).createTask(mockWorkspaceUUID,mockProjectUUID,request);
        verify(taskService, Mockito.times(1)).createTask(mockWorkspaceUUID,mockProjectUUID,request);

    }

    @Test
    @WithMockUser
    void getTask_whenRequestIsValid_returns200Ok() throws Exception{

        TaskResponse response = new TaskResponse(mockTaskUUID,"mock title","mock description",
                TaskStatus.TODO,TaskPriority.MEDIUM, Instant.now());
        Mockito.when(taskService.getTask(mockWorkspaceUUID,mockProjectUUID,mockTaskUUID)).thenReturn(response);

        mockMvc.perform(get(baseUrl+"/{taskId}",mockWorkspaceUUID,mockProjectUUID,mockTaskUUID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.id().toString()))
                .andExpect(jsonPath("$.title").value(response.title()))
                .andExpect(jsonPath("$.description").value(response.description()))
                .andExpect(jsonPath("$.status").value(response.status().toString()))
                .andExpect(jsonPath("$.priority").value(response.priority().toString()));

        verify(taskService, Mockito.times(1)).getTask(mockWorkspaceUUID,mockProjectUUID,mockTaskUUID);

    }

    @Test
    @WithMockUser
    void getTask_whenProjectDoesNotExist_returns400NotFound() throws Exception{

        ProjectNotFoundException exception = new ProjectNotFoundException(mockProjectUUID);
        Mockito.when(taskService.getTask(mockWorkspaceUUID,mockProjectUUID,mockTaskUUID)).thenThrow(exception);
        String path = "/api/v1/workspaces/" + mockWorkspaceUUID +"/projects/"+ mockProjectUUID+"/tasks/" + mockTaskUUID;

        mockMvc.perform(get(baseUrl + "/{taskId}", mockWorkspaceUUID,mockProjectUUID,mockTaskUUID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(exception.getMessage()))
                .andExpect(jsonPath("$.path").value(path));

        verify(taskService,Mockito.times(1)).getTask(mockWorkspaceUUID,mockProjectUUID,mockTaskUUID);
    }

    @Test
    @WithMockUser
    void getTask_whenTaskDoesNotExist_returns400NotFound() throws Exception{

        TaskNotFoundException exception = new TaskNotFoundException(mockTaskUUID);
        Mockito.when(taskService.getTask(mockWorkspaceUUID,mockProjectUUID,mockTaskUUID)).thenThrow(exception);
        String path = "/api/v1/workspaces/" + mockWorkspaceUUID +"/projects/"+ mockProjectUUID+"/tasks/" + mockTaskUUID;

        mockMvc.perform(get(baseUrl + "/{taskId}",mockWorkspaceUUID,mockProjectUUID,mockTaskUUID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(exception.getMessage()))
                .andExpect(jsonPath("$.path").value(path));

        verify(taskService, Mockito.times(1)).getTask(mockWorkspaceUUID,mockProjectUUID,mockTaskUUID);
    }

    @Test
    @WithMockUser
    void updateTask_whenRequestIsValid_returns200Ok() throws Exception{

        UpdateTaskRequest request = new UpdateTaskRequest("updated title",null
                ,TaskPriority.LOW,null);
        TaskResponse response = new TaskResponse(mockTaskUUID, request.title(),
                "original description",TaskStatus.TODO,
                request.priority(),Instant.now());
        Mockito.when(taskService.updateTask(mockWorkspaceUUID,mockProjectUUID,mockTaskUUID,request))
                .thenReturn(response);

        mockMvc.perform(patch(baseUrl + "/{taskId}", mockWorkspaceUUID,mockProjectUUID,mockTaskUUID)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.id().toString()))
                .andExpect(jsonPath("$.title").value(response.title()))
                .andExpect(jsonPath("$.description").value(response.description()))
                .andExpect(jsonPath("$.status").value(response.status().toString()))
                .andExpect(jsonPath("$.priority").value(response.priority().toString()))
                .andExpect(jsonPath("$.dueDate").value(response.dueDate().toString()));

        verify(taskService, Mockito.times(1)).updateTask(mockWorkspaceUUID,mockProjectUUID,mockTaskUUID,request);
    }


    @Test
    @WithMockUser
    void updateTask_whenProjectDoesNotExist_returns400NotFound() throws Exception{

        UpdateTaskRequest request = new UpdateTaskRequest("updated title",null
                ,TaskPriority.LOW,null);
        ProjectNotFoundException exception = new ProjectNotFoundException(mockProjectUUID);
        Mockito.when(taskService.updateTask(mockWorkspaceUUID,mockProjectUUID,mockTaskUUID,request)).thenThrow(exception);

        mockMvc.perform(patch(baseUrl + "/{taskId}", mockWorkspaceUUID,mockProjectUUID,mockTaskUUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(exception.getMessage()));

        verify(taskService,Mockito.times(1)).updateTask(mockWorkspaceUUID,mockProjectUUID,mockTaskUUID,request);
    }

    @Test
    @WithMockUser
    void updateTask_whenTaskDoesNotExist_returns400NotFound() throws Exception{

        UpdateTaskRequest request = new UpdateTaskRequest("updated title",null
                ,TaskPriority.LOW,null);
        TaskNotFoundException exception = new TaskNotFoundException(mockProjectUUID);
        Mockito.when(taskService.updateTask(mockWorkspaceUUID,mockProjectUUID,mockTaskUUID,request)).thenThrow(exception);

        mockMvc.perform(patch(baseUrl + "/{taskId}", mockWorkspaceUUID,mockProjectUUID,mockTaskUUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(exception.getMessage()));

        verify(taskService,Mockito.times(1)).updateTask(mockWorkspaceUUID,mockProjectUUID,mockTaskUUID,request);
    }

    @Test
    @WithMockUser
    void deleteTask_whenRequestIsValid_returns200Ok() throws Exception{

        Mockito.doNothing().when(taskService).deleteTask(mockWorkspaceUUID,mockProjectUUID,mockTaskUUID);
        mockMvc.perform(delete(baseUrl + "/{taskId}", mockWorkspaceUUID,mockProjectUUID,mockTaskUUID)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(taskService,Mockito.times(1)).deleteTask(mockWorkspaceUUID,mockProjectUUID,mockTaskUUID);
    }

    @Test
    @WithMockUser
    void deleteTask_whenProjectIsNotFound_returns400NotFound() throws Exception{

        ProjectNotFoundException exception = new ProjectNotFoundException(mockProjectUUID);
        Mockito.doThrow(exception).when(taskService).deleteTask(mockWorkspaceUUID,mockProjectUUID,mockTaskUUID);
        mockMvc.perform(delete(baseUrl + "/{taskId}", mockWorkspaceUUID,mockProjectUUID,mockTaskUUID)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(exception.getMessage()));

        verify(taskService,Mockito.times(1)).deleteTask(mockWorkspaceUUID,mockProjectUUID,mockTaskUUID);
    }

    @Test
    @WithMockUser
    void deleteTask_whenTaskIsNotFound_returns400NotFound() throws Exception{

        TaskNotFoundException exception = new TaskNotFoundException(mockProjectUUID);
        Mockito.doThrow(exception).when(taskService).deleteTask(mockWorkspaceUUID,mockProjectUUID,mockTaskUUID);
        mockMvc.perform(delete(baseUrl + "/{taskId}", mockWorkspaceUUID,mockProjectUUID,mockTaskUUID)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(exception.getMessage()));

        verify(taskService,Mockito.times(1)).deleteTask(mockWorkspaceUUID,mockProjectUUID,mockTaskUUID);
    }

    @Test
    @WithMockUser
    void getTasks_requestIsValid_returns200Ok() throws Exception{

        Pageable pageable = PageRequest.of(0,10);
        TaskResponse taskResponse = new TaskResponse(mockTaskUUID,"title","description"
                ,TaskStatus.TODO,TaskPriority.MEDIUM,Instant.now());
        Page<TaskResponse> response = new PageImpl<>(List.of(taskResponse));
        Mockito.when(taskService.getTasksByProject(mockWorkspaceUUID,mockProjectUUID,pageable)).thenReturn(response);
        mockMvc.perform(get(baseUrl,mockWorkspaceUUID,mockProjectUUID)
                        .param("page", String.valueOf(0))
                        .param("size", String.valueOf(10))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.content[0].id").value(mockTaskUUID.toString()))
                .andExpect(jsonPath("$.content[0].title").value(taskResponse.title()))
                .andExpect(jsonPath("$.content[0].description").value(taskResponse.description()))
                .andExpect(jsonPath("$.content[0].status").value(taskResponse.status().toString()))
                .andExpect(jsonPath("$.content[0].priority").value(taskResponse.priority().toString()));

        verify(taskService,Mockito.times(1)).getTasksByProject(mockWorkspaceUUID,mockProjectUUID,pageable);
    }

    @Test
    @WithMockUser
    void getTasks_whenProjectDoesNotExist_returns400NotFound() throws Exception{

        Pageable pageable = PageRequest.of(0,10);
        ProjectNotFoundException exception = new ProjectNotFoundException(mockProjectUUID);
        Mockito.when(taskService.getTasksByProject(mockWorkspaceUUID,mockProjectUUID,pageable)).thenThrow(exception);

        mockMvc.perform(get(baseUrl,mockWorkspaceUUID,mockProjectUUID,pageable)
                        .param("page","0")
                        .param("size", "10"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(exception.getMessage()));

        verify(taskService,Mockito.times(1)).getTasksByProject(mockWorkspaceUUID,mockProjectUUID,pageable);
    }

    @Test
    @WithMockUser
    void getTasks_whenTaskDoesNotExist_returns400NotFound() throws Exception{

        Pageable pageable = PageRequest.of(0,10);
        TaskNotFoundException exception = new TaskNotFoundException(mockTaskUUID);
        Mockito.when(taskService.getTasksByProject(mockWorkspaceUUID,mockProjectUUID,pageable)).thenThrow(exception);

        mockMvc.perform(get(baseUrl,mockWorkspaceUUID,mockProjectUUID,pageable)
                        .param("page","0")
                        .param("size", "10"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath("$.error").value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath("$.message").value(exception.getMessage()));

        verify(taskService,Mockito.times(1)).getTasksByProject(mockWorkspaceUUID,mockProjectUUID,pageable);
    }

}

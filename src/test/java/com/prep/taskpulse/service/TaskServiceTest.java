package com.prep.taskpulse.service;

import com.prep.taskpulse.domain.project.Project;
import com.prep.taskpulse.domain.project.repository.ProjectRepository;
import com.prep.taskpulse.domain.task.Task;
import com.prep.taskpulse.domain.task.dto.CreateTaskRequest;
import com.prep.taskpulse.domain.task.dto.TaskResponse;
import com.prep.taskpulse.domain.task.enums.TaskPriority;
import com.prep.taskpulse.domain.task.mapper.TaskMapper;
import com.prep.taskpulse.domain.task.repository.TaskRepository;
import com.prep.taskpulse.domain.task.service.TaskService;
import com.prep.taskpulse.domain.user.Role;
import com.prep.taskpulse.domain.user.User;
import com.prep.taskpulse.domain.workspace.Workspace;
import com.prep.taskpulse.exception.ProjectNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private UUID mockUserUUID;
    private UUID mockWorkspaceUUID;
    private UUID mockProjectUUID;
    private User mockUser;
    private Workspace mockWorkspace;
    private Project mockProject;


    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskService taskService;

    @BeforeEach
    void setup(){
        mockUserUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        mockWorkspaceUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174111");
        mockProjectUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174222");
        mockUser = User.createUser("testUser","test@gmail.com","12345678", Role.USER);
        mockWorkspace = Workspace.create("mockWorkspace", mockUser);
        mockProject = Project.create("mockProject","mock project descritption",mockWorkspace);
    }

    @Test
    void createTask_whenProjectExists_createsAndReturnsTaskResponse(){
        Instant dueDate = Instant.now().plus(java.time.Duration.ofDays(2));
        when(projectRepository.findByIdAndWorkspaceId(mockProjectUUID, mockWorkspaceUUID)).thenReturn(Optional.of(mockProject));
        Task task = Task.create("new task","sample description",mockProject,TaskPriority.MEDIUM, dueDate);
        UUID mockTaskUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174333");
        TaskResponse mockResponse = new TaskResponse(mockTaskUUID,task.getTitle(),task.getDescription(),task.getStatus(),task.getPriority(),task.getDueDate());
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        when(taskMapper.toResponse(task)).thenReturn(mockResponse);

        CreateTaskRequest request = new CreateTaskRequest("new task","sample description", TaskPriority.MEDIUM, dueDate);
        TaskResponse response = taskService.createTask(mockWorkspaceUUID,mockProjectUUID,request);

        assertThat(response).isEqualTo(mockResponse);
        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());

        Task capturedTask = taskCaptor.getValue();

        assertThat(capturedTask.getTitle()).isEqualTo(request.title());
        assertThat(capturedTask.getProject()).isEqualTo(mockProject);
        assertThat(capturedTask.getPriority()).isEqualTo(request.priority());

        verify(projectRepository).findByIdAndWorkspaceId(mockProjectUUID,mockWorkspaceUUID);
        verify(taskMapper).toResponse(task);

        ArgumentCaptor<Task> mapperTaskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskMapper).toResponse(mapperTaskCaptor.capture());
        Task taskSentToMapper = mapperTaskCaptor.getValue();
        assertThat(taskSentToMapper.getTitle()).isEqualTo("new task");

        verifyNoMoreInteractions(projectRepository,taskRepository,taskMapper);

    }

    @Test
    void createTask_whenProjectDoesNotExist_throwsProjectNotFoundException(){
        Instant dueDate = Instant.now().plus(java.time.Duration.ofDays(2));
        when(projectRepository.findByIdAndWorkspaceId(mockProjectUUID, mockWorkspaceUUID)).thenReturn(Optional.empty());
        CreateTaskRequest request = new CreateTaskRequest("new task","sample description", TaskPriority.MEDIUM, dueDate);

        ProjectNotFoundException exception = assertThrows(ProjectNotFoundException.class,
                () -> taskService.createTask(mockWorkspaceUUID,mockProjectUUID,request));

        assertThat(exception.getMessage()).isEqualTo("Project not found: " + mockProjectUUID);
        verify(projectRepository).findByIdAndWorkspaceId(mockProjectUUID, mockWorkspaceUUID);
        verifyNoMoreInteractions(projectRepository);
    }


}

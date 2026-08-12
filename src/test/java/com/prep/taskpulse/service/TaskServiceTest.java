package com.prep.taskpulse.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.prep.taskpulse.domain.project.Project;
import com.prep.taskpulse.domain.project.repository.ProjectRepository;
import com.prep.taskpulse.domain.task.Task;
import com.prep.taskpulse.domain.task.dto.CreateTaskRequest;
import com.prep.taskpulse.domain.task.dto.TaskResponse;
import com.prep.taskpulse.domain.task.dto.UpdateTaskRequest;
import com.prep.taskpulse.domain.task.enums.TaskPriority;
import com.prep.taskpulse.domain.task.enums.TaskStatus;
import com.prep.taskpulse.domain.task.mapper.TaskMapper;
import com.prep.taskpulse.domain.task.repository.TaskRepository;
import com.prep.taskpulse.domain.task.service.TaskService;
import com.prep.taskpulse.domain.user.Role;
import com.prep.taskpulse.domain.user.User;
import com.prep.taskpulse.domain.workspace.Workspace;
import com.prep.taskpulse.exception.ProjectNotFoundException;
import com.prep.taskpulse.exception.TaskNotFoundException;
import com.prep.taskpulse.outbox.service.OutboxService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

  private UUID mockWorkspaceUUID;
  private UUID mockProjectUUID;
  private User mockUser;
  private Workspace mockWorkspace;
  private Project mockProject;

  @Mock private TaskRepository taskRepository;

  @Mock private ProjectRepository projectRepository;

  @Mock private TaskMapper taskMapper;

  @Mock private OutboxService outboxService;

  @Mock private MeterRegistry meterRegistry;

  @Mock private Counter taskCreatedCounter;

  @InjectMocks private TaskService taskService;

  @BeforeEach
  void setup() {
    mockWorkspaceUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174111");
    mockProjectUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174222");
    mockUser = User.createUser("testUser", "test@gmail.com", "12345678", Role.USER);
    mockWorkspace = Workspace.create("mockWorkspace", mockUser);
    mockProject = Project.create("mockProject", "mock project descritption", mockWorkspace);
  }

  @Test
  void createTask_whenProjectExists_createsAndReturnsTaskResponse() {
    Instant dueDate = Instant.now().plus(java.time.Duration.ofDays(2));
    when(projectRepository.findByIdAndWorkspaceId(mockProjectUUID, mockWorkspaceUUID))
        .thenReturn(Optional.of(mockProject));
    Task task =
        Task.create("new task", "sample description", mockProject, TaskPriority.MEDIUM, dueDate);
    UUID mockTaskUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174333");
    TaskResponse mockResponse =
        new TaskResponse(
            mockTaskUUID,
            task.getTitle(),
            task.getDescription(),
            task.getStatus(),
            task.getPriority(),
            task.getDueDate(),
            0L);
    when(taskRepository.save(any(Task.class))).thenReturn(task);
    when(taskMapper.toResponse(task)).thenReturn(mockResponse);

    CreateTaskRequest request =
        new CreateTaskRequest("new task", "sample description", TaskPriority.MEDIUM, dueDate);

    when(meterRegistry.counter("taskflow.tasks.created")).thenReturn(taskCreatedCounter);

    TaskResponse response = taskService.createTask(mockWorkspaceUUID, mockProjectUUID, request);

    assertThat(response).isEqualTo(mockResponse);
    ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
    verify(taskRepository).save(taskCaptor.capture());

    Task capturedTask = taskCaptor.getValue();

    assertThat(capturedTask.getTitle()).isEqualTo(request.title());
    assertThat(capturedTask.getProject()).isEqualTo(mockProject);
    assertThat(capturedTask.getPriority()).isEqualTo(request.priority());

    verify(projectRepository).findByIdAndWorkspaceId(mockProjectUUID, mockWorkspaceUUID);
    verify(taskMapper).toResponse(task);

    ArgumentCaptor<Task> mapperTaskCaptor = ArgumentCaptor.forClass(Task.class);
    verify(taskMapper).toResponse(mapperTaskCaptor.capture());
    Task taskSentToMapper = mapperTaskCaptor.getValue();
    assertThat(taskSentToMapper.getTitle()).isEqualTo("new task");

    // the meterRegistry.counter returns a counter class, we check if increment was called.
    verify(taskCreatedCounter, times(1)).increment();

    verifyNoMoreInteractions(projectRepository, taskRepository, taskMapper);
  }

  @Test
  void createTask_whenProjectDoesNotExist_throwsProjectNotFoundException() {
    Instant dueDate = Instant.now().plus(java.time.Duration.ofDays(2));
    when(projectRepository.findByIdAndWorkspaceId(mockProjectUUID, mockWorkspaceUUID))
        .thenReturn(Optional.empty());
    CreateTaskRequest request =
        new CreateTaskRequest("new task", "sample description", TaskPriority.MEDIUM, dueDate);

    ProjectNotFoundException exception =
        assertThrows(
            ProjectNotFoundException.class,
            () -> taskService.createTask(mockWorkspaceUUID, mockProjectUUID, request));

    assertThat(exception.getMessage()).isEqualTo("Project not found: " + mockProjectUUID);
    verify(projectRepository).findByIdAndWorkspaceId(mockProjectUUID, mockWorkspaceUUID);
    verifyNoMoreInteractions(projectRepository);
  }

  @Test
  void getTask_whenTaskExists_returnsTaskResponse() {
    UUID mockTaskUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174333");
    Task task =
        Task.create(
            "new task", "sample description", mockProject, TaskPriority.MEDIUM, Instant.now());
    TaskResponse mockResponse =
        new TaskResponse(
            mockTaskUUID,
            task.getTitle(),
            task.getDescription(),
            task.getStatus(),
            task.getPriority(),
            task.getDueDate(),
            0L);

    when(projectRepository.findByIdAndWorkspaceId(mockProjectUUID, mockWorkspaceUUID))
        .thenReturn(Optional.of(mockProject));
    when(taskRepository.findWithProjectAndAssigneeByIdAndProjectIdAndDeletedAtIsNull(
            mockTaskUUID, mockProjectUUID))
        .thenReturn(Optional.of(task));
    when(taskMapper.toResponse(task)).thenReturn(mockResponse);

    TaskResponse response = taskService.getTask(mockWorkspaceUUID, mockProjectUUID, mockTaskUUID);

    verify(projectRepository).findByIdAndWorkspaceId(mockProjectUUID, mockWorkspaceUUID);
    verify(taskRepository)
        .findWithProjectAndAssigneeByIdAndProjectIdAndDeletedAtIsNull(
            mockTaskUUID, mockProjectUUID);
    verify(taskMapper).toResponse(task);
    verifyNoMoreInteractions(projectRepository, taskRepository, taskMapper);

    assertThat(response).isEqualTo(mockResponse);
  }

  @Test
  void getTask_whenProjectDoesNotExist_throwsProjectNotFoundException() {
    UUID mockTaskUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174333");
    when(projectRepository.findByIdAndWorkspaceId(mockProjectUUID, mockWorkspaceUUID))
        .thenReturn(Optional.empty());

    ProjectNotFoundException exception =
        assertThrows(
            ProjectNotFoundException.class,
            () -> taskService.getTask(mockWorkspaceUUID, mockProjectUUID, mockTaskUUID));

    verify(projectRepository).findByIdAndWorkspaceId(mockProjectUUID, mockWorkspaceUUID);
    verifyNoMoreInteractions(projectRepository);
    verifyNoInteractions(taskRepository, taskMapper);

    assertThat(exception.getMessage()).isEqualTo("Project not found: " + mockProjectUUID);
  }

  @Test
  void getTask_whenTaskDoesNotExist_throwsTaskNotFoundException() {
    UUID mockTaskUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174333");
    when(projectRepository.findByIdAndWorkspaceId(mockProjectUUID, mockWorkspaceUUID))
        .thenReturn(Optional.of(mockProject));
    when(taskRepository.findWithProjectAndAssigneeByIdAndProjectIdAndDeletedAtIsNull(
            mockTaskUUID, mockProjectUUID))
        .thenReturn(Optional.empty());

    TaskNotFoundException exception =
        assertThrows(
            TaskNotFoundException.class,
            () -> taskService.getTask(mockWorkspaceUUID, mockProjectUUID, mockTaskUUID));

    verify(projectRepository).findByIdAndWorkspaceId(mockProjectUUID, mockWorkspaceUUID);
    verify(taskRepository)
        .findWithProjectAndAssigneeByIdAndProjectIdAndDeletedAtIsNull(
            mockTaskUUID, mockProjectUUID);
    verifyNoInteractions(taskMapper);
    verifyNoMoreInteractions(projectRepository, taskRepository);

    assertThat(exception.getMessage()).isEqualTo("Task not found: " + mockTaskUUID);
  }

  @Test
  void updateTask_whenRequestContainsPartialFields_updatesOnlyProvidedFields() {
    UUID mockTaskUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174333");
    Instant updatedDueDate = Instant.now().plus(Duration.ofDays(3));
    Instant originalDueDate = Instant.now().plus(Duration.ofDays(2));
    UpdateTaskRequest request =
        new UpdateTaskRequest("updated title", null, TaskPriority.LOW, updatedDueDate, 1L);
    when(projectRepository.findByIdAndWorkspaceId(mockProjectUUID, mockWorkspaceUUID))
        .thenReturn(Optional.of(mockProject));

    Task task = mock(Task.class);
    when(task.getVersion()).thenReturn(1L);

    when(taskRepository.findByIdAndProjectIdAndDeletedAtIsNull(mockTaskUUID, mockProjectUUID))
        .thenReturn(Optional.of(task));

    TaskResponse mockResponse =
        new TaskResponse(
            mockTaskUUID,
            request.title(),
            task.getDescription(),
            task.getStatus(),
            request.priority(),
            updatedDueDate,
            2L);
    when(taskMapper.toResponse(task)).thenReturn(mockResponse);

    TaskResponse response =
        taskService.updateTask(mockWorkspaceUUID, mockProjectUUID, mockTaskUUID, request);
    verify(projectRepository).findByIdAndWorkspaceId(mockProjectUUID, mockWorkspaceUUID);
    verify(taskRepository).findByIdAndProjectIdAndDeletedAtIsNull(mockTaskUUID, mockProjectUUID);
    verify(taskMapper).toResponse(task);
    verify(taskRepository).flush();
    verifyNoMoreInteractions(projectRepository, taskRepository, taskMapper);

    assertThat(response).isEqualTo(mockResponse);
  }

  @Test
  void updateTask_whenProjectDoesNotExist_throwsProjectNotFoundException() {
    UUID mockTaskUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174333");
    when(projectRepository.findByIdAndWorkspaceId(mockProjectUUID, mockWorkspaceUUID))
        .thenReturn(Optional.empty());
    UpdateTaskRequest request =
        new UpdateTaskRequest(
            "updated title", null, TaskPriority.LOW, Instant.now().plus(Duration.ofDays(2)), 1L);
    ProjectNotFoundException exception =
        assertThrows(
            ProjectNotFoundException.class,
            () ->
                taskService.updateTask(mockWorkspaceUUID, mockProjectUUID, mockTaskUUID, request));

    verify(projectRepository).findByIdAndWorkspaceId(mockProjectUUID, mockWorkspaceUUID);
    verifyNoMoreInteractions(projectRepository);
    verifyNoInteractions(taskRepository, taskMapper);

    assertThat(exception.getMessage()).isEqualTo("Project not found: " + mockProjectUUID);
  }

  @Test
  void updateTask_whenTaskDoesNotExist_throwsTaskNotFoundException() {
    UUID mockTaskUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174333");
    when(projectRepository.findByIdAndWorkspaceId(mockProjectUUID, mockWorkspaceUUID))
        .thenReturn(Optional.of(mockProject));

    UpdateTaskRequest request =
        new UpdateTaskRequest(
            "updated title", null, TaskPriority.LOW, Instant.now().plus(Duration.ofDays(2)), 1L);
    TaskNotFoundException exception =
        assertThrows(
            TaskNotFoundException.class,
            () ->
                taskService.updateTask(mockWorkspaceUUID, mockProjectUUID, mockTaskUUID, request));

    verify(projectRepository).findByIdAndWorkspaceId(mockProjectUUID, mockWorkspaceUUID);
    verify(taskRepository).findByIdAndProjectIdAndDeletedAtIsNull(mockTaskUUID, mockProjectUUID);
    verifyNoMoreInteractions(projectRepository, taskRepository);
    verifyNoInteractions(taskMapper);

    assertThat(exception.getMessage()).isEqualTo("Task not found: " + mockTaskUUID);
  }

  @Test
  void deleteTask_whenTaskExists_softDeletesTask() {
    UUID mockTaskUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174333");
    when(projectRepository.findByIdAndWorkspaceId(mockProjectUUID, mockWorkspaceUUID))
        .thenReturn(Optional.of(mockProject));

    Task task = mock(Task.class);
    when(task.getDeletedAt()).thenReturn(Instant.now());
    when(taskRepository.findByIdAndProjectIdAndDeletedAtIsNull(mockTaskUUID, mockProjectUUID))
        .thenReturn(Optional.of(task));

    taskService.deleteTask(mockWorkspaceUUID, mockProjectUUID, mockTaskUUID);

    verify(projectRepository).findByIdAndWorkspaceId(mockProjectUUID, mockWorkspaceUUID);
    verify(taskRepository).findByIdAndProjectIdAndDeletedAtIsNull(mockTaskUUID, mockProjectUUID);
    verifyNoMoreInteractions(projectRepository, taskRepository);
    verify(taskRepository, never()).delete((Task) any());

    assertThat(task.getDeletedAt()).isNotNull();
  }

  @Test
  void deleteTask_whenProjectDoesNotExist_throwsProjectNotFoundException() {
    UUID mockTaskUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174333");
    when(projectRepository.findByIdAndWorkspaceId(mockProjectUUID, mockWorkspaceUUID))
        .thenReturn(Optional.empty());

    ProjectNotFoundException exception =
        assertThrows(
            ProjectNotFoundException.class,
            () -> taskService.deleteTask(mockWorkspaceUUID, mockProjectUUID, mockTaskUUID));

    verify(projectRepository).findByIdAndWorkspaceId(mockProjectUUID, mockWorkspaceUUID);
    verify(taskRepository, never())
        .findWithProjectAndAssigneeByIdAndProjectIdAndDeletedAtIsNull(
            mockTaskUUID, mockProjectUUID);
    verifyNoMoreInteractions(projectRepository);
    assertThat(exception.getMessage()).isEqualTo("Project not found: " + mockProjectUUID);
  }

  @Test
  void deleteTask_whenTaskDoesNotExist_throwsTaskNotFoundException() {
    UUID mockTaskUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174333");
    when(projectRepository.findByIdAndWorkspaceId(mockProjectUUID, mockWorkspaceUUID))
        .thenReturn(Optional.of(mockProject));

    TaskNotFoundException exception =
        assertThrows(
            TaskNotFoundException.class,
            () -> taskService.deleteTask(mockWorkspaceUUID, mockProjectUUID, mockTaskUUID));

    verify(projectRepository).findByIdAndWorkspaceId(mockProjectUUID, mockWorkspaceUUID);
    verify(taskRepository).findByIdAndProjectIdAndDeletedAtIsNull(mockTaskUUID, mockProjectUUID);
    verify(taskRepository, never()).delete((Task) any());

    assertThat(exception.getMessage()).isEqualTo("Task not found: " + mockTaskUUID);
  }

  @Test
  void getTasksByProject_whenProjectExists_returnsPagedTaskResponses() {
    UUID mockTaskUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174333");
    Instant dueDate = Instant.now();
    when(projectRepository.findByIdAndWorkspaceId(mockProjectUUID, mockWorkspaceUUID))
        .thenReturn(Optional.of(mockProject));

    Task task =
        Task.create("new task", "sample description", mockProject, TaskPriority.MEDIUM, dueDate);
    Pageable pageable = PageRequest.of(0, 10);
    Page<Task> mockPageTask = new PageImpl<>(List.of(task), pageable, 1);
    when(taskRepository.findByProjectIdAndDeletedAtIsNull(mockProjectUUID, pageable))
        .thenReturn(mockPageTask);

    TaskResponse response =
        new TaskResponse(
            mockTaskUUID,
            task.getTitle(),
            task.getDescription(),
            TaskStatus.TODO,
            TaskPriority.MEDIUM,
            dueDate,
            1L);
    when(taskMapper.toResponse(task)).thenReturn(response);

    Page<TaskResponse> taskResponsePage =
        taskService.getTasksByProject(mockWorkspaceUUID, mockProjectUUID, pageable);

    verify(projectRepository).findByIdAndWorkspaceId(mockProjectUUID, mockWorkspaceUUID);
    verify(taskRepository).findByProjectIdAndDeletedAtIsNull(mockProjectUUID, pageable);
    verify(taskMapper).toResponse(task);
    verifyNoMoreInteractions(projectRepository, taskRepository, taskMapper);

    assertThat(taskResponsePage).isNotNull();
    assertThat(taskResponsePage.getTotalPages()).isEqualTo(1);
    assertThat(taskResponsePage.getTotalElements()).isEqualTo(1);
    assertThat(taskResponsePage.getContent().get(0)).isEqualTo(response);
  }

  @Test
  void getTasksByProject_whenProjectDoesNotExist_throwsProjectNotFoundException() {
    when(projectRepository.findByIdAndWorkspaceId(mockProjectUUID, mockWorkspaceUUID))
        .thenReturn(Optional.empty());
    Pageable pageable = PageRequest.of(0, 10);
    ProjectNotFoundException exception =
        assertThrows(
            ProjectNotFoundException.class,
            () -> taskService.getTasksByProject(mockWorkspaceUUID, mockProjectUUID, pageable));

    verify(projectRepository).findByIdAndWorkspaceId(mockProjectUUID, mockWorkspaceUUID);
    verifyNoInteractions(taskRepository, taskMapper);
    verifyNoMoreInteractions(projectRepository);
    assertThat(exception.getMessage()).isEqualTo("Project not found: " + mockProjectUUID);
  }
}

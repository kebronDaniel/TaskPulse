package com.prep.taskpulse.integration;

import com.prep.taskpulse.domain.project.Project;
import com.prep.taskpulse.domain.project.repository.ProjectRepository;
import com.prep.taskpulse.domain.task.Task;
import com.prep.taskpulse.domain.task.TaskEvent;
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
import com.prep.taskpulse.outbox.service.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;
import static reactor.core.publisher.Mono.when;

@SpringBootTest
@Testcontainers
class TaskCacheIntegrationTest {

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);


    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry dynamicPropertyRegistry){
        dynamicPropertyRegistry.add("spring.data.registry.host", redis::getHost);
        dynamicPropertyRegistry.add("spring.data.registry.port", () -> redis.getMappedPort(6379));
    }


    @Autowired
    private TaskService taskService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private TaskRepository taskRepository;

    @MockitoBean
    private TaskMapper taskMapper;

    @MockitoBean
    private ProjectRepository projectRepository;

    @MockitoBean
    private OutboxService outboxService;

    @BeforeEach
    void clearCache(){
        var cache = cacheManager.getCache("tasks");
        if (cache != null) cache.clear();
    }

    private UUID mockWorkspaceUUID;
    private UUID mockProjectUUID;
    private User mockUser;
    private Workspace mockWorkspace;
    private Project mockProject;


    @BeforeEach
    void setup(){
        mockWorkspaceUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174111");
        mockProjectUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174222");
        mockUser = User.createUser("testUser","test@gmail.com","12345678", Role.USER);
        mockWorkspace = Workspace.create("mockWorkspace", mockUser);
        mockProject = Project.create("mockProject","mock project descritption",mockWorkspace);
    }

    @Test
    void getTask_whenCalledTwice_usesCacheOnSecondCall(){

        UUID mockTaskUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174333");
        Mockito.when(projectRepository.findByIdAndWorkspaceId(mockProjectUUID, mockWorkspaceUUID)).thenReturn(Optional.of(mockProject));

        Task task = Task.create("new task","sample description",mockProject, TaskPriority.MEDIUM, Instant.now());
        TaskResponse mockResponse = new TaskResponse(mockTaskUUID,task.getTitle(),task.getDescription(),task.getStatus(),task.getPriority(),task.getDueDate(),0L);

        Mockito.when(projectRepository.findByIdAndWorkspaceId(mockProjectUUID,mockWorkspaceUUID)).thenReturn(Optional.of(mockProject));
        Mockito.when(taskRepository.findWithProjectAndAssigneeByIdAndProjectIdAndDeletedAtIsNull(mockTaskUUID,mockProjectUUID)).thenReturn(Optional.of(task));
        Mockito.when(taskMapper.toResponse(task)).thenReturn(mockResponse);

        TaskResponse response = taskService.getTask(mockWorkspaceUUID, mockProjectUUID, mockTaskUUID);
        TaskResponse responseTwo = taskService.getTask(mockWorkspaceUUID, mockProjectUUID, mockTaskUUID);

        verify(projectRepository, times(1)).findByIdAndWorkspaceId(mockProjectUUID,mockWorkspaceUUID);
        verify(taskRepository, times(1)).findWithProjectAndAssigneeByIdAndProjectIdAndDeletedAtIsNull(mockTaskUUID,mockProjectUUID);
        verify(taskMapper, times(1)).toResponse(task);

        assertThat(response).isEqualTo(mockResponse);
        assertThat(responseTwo).isEqualTo(mockResponse);

        verifyNoMoreInteractions(projectRepository, taskRepository, taskMapper);

    }

    @Test
    void getTask_whenCacheEvicted_usesDbCallToGetTask(){
        UUID mockTaskUUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174333");
        Mockito.when(projectRepository.findByIdAndWorkspaceId(mockProjectUUID, mockWorkspaceUUID)).thenReturn(Optional.of(mockProject));

        Task task = mock(Task.class);
        Mockito.when(task.getVersion()).thenReturn(0L);
        TaskResponse response = new TaskResponse(mockTaskUUID,"title","desc",
                TaskStatus.TODO,TaskPriority.MEDIUM,Instant.now(),1L);

        Mockito.when(projectRepository.findByIdAndWorkspaceId(mockProjectUUID,mockWorkspaceUUID)).thenReturn(Optional.of(mockProject));
        Mockito.when(taskRepository.findWithProjectAndAssigneeByIdAndProjectIdAndDeletedAtIsNull(mockTaskUUID,mockProjectUUID)).thenReturn(Optional.of(task));
        Mockito.when(taskRepository.findByIdAndProjectIdAndDeletedAtIsNull(mockTaskUUID,mockProjectUUID)).thenReturn(Optional.of(task));
        Mockito.when(taskMapper.toResponse(task)).thenReturn(response);

        TaskResponse firstRead = taskService.getTask(mockWorkspaceUUID, mockProjectUUID, mockTaskUUID);

        UpdateTaskRequest request = new UpdateTaskRequest("updated title",null,null,null,0L);

        taskService.updateTask(mockWorkspaceUUID,mockProjectUUID,mockTaskUUID,request);
        TaskResponse afterEvict = taskService.getTask(mockWorkspaceUUID, mockProjectUUID, mockTaskUUID);
        TaskResponse cacheRead = taskService.getTask(mockWorkspaceUUID, mockProjectUUID, mockTaskUUID);

        verify(taskRepository, times(2)).findWithProjectAndAssigneeByIdAndProjectIdAndDeletedAtIsNull(mockTaskUUID,mockProjectUUID);
        verify(taskMapper, times(3)).toResponse(task); // update also calls the mapper.
        verify(taskRepository).flush();

        assertThat(firstRead).isEqualTo(response);
        assertThat(afterEvict).isEqualTo(response);
        assertThat(cacheRead).isEqualTo(response);

    }

}

package com.prep.taskpulse.repository;

import com.prep.taskpulse.config.AuditConfig;
import com.prep.taskpulse.domain.project.Project;
import com.prep.taskpulse.domain.project.repository.ProjectRepository;
import com.prep.taskpulse.domain.task.Task;
import com.prep.taskpulse.domain.task.dto.TaskSearchCriteria;
import com.prep.taskpulse.domain.task.enums.TaskPriority;
import com.prep.taskpulse.domain.task.enums.TaskStatus;
import com.prep.taskpulse.domain.task.repository.TaskRepository;
import com.prep.taskpulse.domain.task.repository.TaskSpecifications;
import com.prep.taskpulse.domain.user.Role;
import com.prep.taskpulse.domain.user.User;
import com.prep.taskpulse.domain.user.repository.UserRepository;
import com.prep.taskpulse.domain.workspace.Workspace;
import com.prep.taskpulse.domain.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AuditConfig.class)
public class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    private User savedUser;
    private Project savedProject;

    @BeforeEach
    void setUp(){
        User user = User.createUser("test user","test@gmail.com","1254345123", Role.USER);
        savedUser = userRepository.save(user);

        Workspace workspace = Workspace.create("test workspace",user);
        workspaceRepository.save(workspace);

        Project project = Project.create("test project","test description",workspace);
        savedProject = projectRepository.save(project);
    }


    @Test
    void findWithProjectAndAssigneeByIdAndProjectIdAndDeletedAtIsNull_whenTaskActive_returnsTask(){

        Task mockTask = Task.create("test task","test task description"
                ,savedProject,null, Instant.now().plus(Duration.ofDays(2)));
        mockTask.assignTo(savedUser);
        Task savedTask = taskRepository.save(mockTask);

        Optional<Task> task = taskRepository.findWithProjectAndAssigneeByIdAndProjectIdAndDeletedAtIsNull(savedTask.getId(),savedProject.getId());

        assertThat(task).isNotNull();
        assertThat(task.get().getTitle()).isEqualTo(savedTask.getTitle());
        assertThat(task.get().getDescription()).isEqualTo(savedTask.getDescription());
        assertThat(task.get().getProject()).isEqualTo(savedTask.getProject());
        assertThat(task.get().getPriority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(task.get().getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(task.get().getAssignee()).isEqualTo(savedUser);
        assertThat(task.get().getDeletedAt()).isNull();

    }

    @Test
    void findWithProjectAndAssigneeByIdAndProjectIdAndDeletedAtIsNull_whenTaskSoftDeleted_returnsEmpty(){

        Task mockTask = Task.create("test task","test task description"
                ,savedProject,null, Instant.now().plus(Duration.ofDays(2)));
        mockTask.assignTo(savedUser);
        mockTask.deleteTask();
        Task savedTask = taskRepository.save(mockTask);

        Optional<Task> task = taskRepository.findWithProjectAndAssigneeByIdAndProjectIdAndDeletedAtIsNull(savedTask.getId(),savedProject.getId());
        assertThat(savedTask.getDeletedAt()).isNotNull();
        assertThat(task).isEmpty();
    }

    @Test
    void findByProjectIdAndDeletedAtIsNull_returnsOnlyActiveTasks(){

        Task mockTask = Task.create("test task 1","test task 1 description"
                ,savedProject,null, Instant.now().plus(Duration.ofDays(2)));
        mockTask.assignTo(savedUser);
        Task savedTask = taskRepository.save(mockTask);

        Task mockTask2 = Task.create("test task 2","test task 2 description"
                ,savedProject,null, Instant.now().plus(Duration.ofDays(2)));
        mockTask2.assignTo(savedUser);
        Task savedTask2 = taskRepository.save(mockTask2);

        Task deletedTask = Task.create("test task 3","test task 3 description"
                ,savedProject,null, Instant.now().plus(Duration.ofDays(2)));
        deletedTask.assignTo(savedUser);
        deletedTask.deleteTask();
        taskRepository.save(deletedTask);

        Pageable pageable = PageRequest.of(0,10);

        Page<Task> tasks = taskRepository.findByProjectIdAndDeletedAtIsNull(savedProject.getId(),pageable);
        assertThat(tasks).isNotNull();
        assertThat(tasks.getTotalPages()).isEqualTo(1);
        assertThat(tasks.getTotalElements()).isEqualTo(2);
        assertThat(tasks.getContent().get(0)).isEqualTo(savedTask);
        assertThat(tasks.getContent()).extracting(task -> task.getId()).contains(savedTask.getId());
        assertThat(tasks.getContent()).extracting(Task::getTitle)
                .containsExactlyInAnyOrder(savedTask.getTitle(),savedTask2.getTitle())
                .doesNotContain(deletedTask.getTitle());
    }

    @Test
    void existsByIdAndProjectIdAndDeletedAtIsNull_returnsTrue(){
        Task mockTask = Task.create("test task 1","test task 1 description"
                ,savedProject,null, Instant.now().plus(Duration.ofDays(2)));
        mockTask.assignTo(savedUser);
        Task savedTask = taskRepository.save(mockTask);

        boolean taskExists = taskRepository.existsByIdAndProjectIdAndDeletedAtIsNull(savedTask.getId(),savedProject.getId());
        assertThat(taskExists).isTrue();
    }

    @Test
    void existsByIdAndProjectIdAndDeletedAtIsNull_whenTaskIsDeleted_returnsFalse(){
        Task mockTask = Task.create("test task 1","test task 1 description"
                ,savedProject,null, Instant.now().plus(Duration.ofDays(2)));
        mockTask.assignTo(savedUser);
        mockTask.deleteTask();
        Task savedTask = taskRepository.save(mockTask);

        boolean taskExists = taskRepository.existsByIdAndProjectIdAndDeletedAtIsNull(savedTask.getId(),savedProject.getId());
        assertThat(taskExists).isFalse();
    }

    @Test
    void findAll_withCompleteSpecification_returnsOnlyMatchingTask() {
        Instant now = Instant.now();

        Task matchingTask = Task.create(
                "matching task",
                "matches every criterion",
                savedProject,
                TaskPriority.HIGH,
                now.plus(Duration.ofDays(2))
        );
        matchingTask.assignTo(savedUser);
        matchingTask.changeStatus(TaskStatus.IN_PROGRESS);
        taskRepository.save(matchingTask);

        Task wrongPriorityTask = Task.create(
                "wrong priority",
                "must not be returned",
                savedProject,
                TaskPriority.LOW,
                now.plus(Duration.ofDays(2))
        );
        wrongPriorityTask.assignTo(savedUser);
        wrongPriorityTask.changeStatus(TaskStatus.IN_PROGRESS);
        taskRepository.save(wrongPriorityTask);

        taskRepository.flush();

        TaskSearchCriteria criteria = new TaskSearchCriteria(
                TaskPriority.HIGH,
                TaskStatus.IN_PROGRESS,
                savedUser.getId(),
                now.plus(Duration.ofDays(3)),
                now.minus(Duration.ofMinutes(1))
        );

        List<Task> result = taskRepository.findAll(
                TaskSpecifications.fromCriteria(savedProject.getId(), criteria)
        );

        assertThat(result)
                .extracting(Task::getId)
                .containsExactly(matchingTask.getId());
    }

    @Test
    void findAll_withEmptyCriteria_returnsActiveProjectTasksOnly(){
        Task task = Task.create(
                "new task",
                "new task desc",
                savedProject,
                TaskPriority.LOW,
                Instant.now().plus(Duration.ofDays(2))
        );
        Task savedTask = taskRepository.save(task);

        Task deletedTask = Task.create(
                "deleted task",
                "new task to be removed",
                savedProject,
                TaskPriority.LOW,
                Instant.now().plus(Duration.ofDays(2))
        );
        deletedTask.deleteTask();
        taskRepository.flush();

        TaskSearchCriteria criteria = new TaskSearchCriteria(null,null,null,null,null);

        List<Task> taskList = taskRepository.findAll(TaskSpecifications.fromCriteria(savedProject.getId(),criteria));
        assertThat(taskList).extracting(Task::getId).contains(savedTask.getId()).doesNotContain(deletedTask.getId());
    }


}

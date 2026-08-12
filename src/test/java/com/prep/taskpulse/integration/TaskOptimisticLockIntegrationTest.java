package com.prep.taskpulse.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.prep.taskpulse.domain.project.Project;
import com.prep.taskpulse.domain.project.repository.ProjectRepository;
import com.prep.taskpulse.domain.task.Task;
import com.prep.taskpulse.domain.task.enums.TaskPriority;
import com.prep.taskpulse.domain.task.repository.TaskRepository;
import com.prep.taskpulse.domain.user.Role;
import com.prep.taskpulse.domain.user.User;
import com.prep.taskpulse.domain.user.repository.UserRepository;
import com.prep.taskpulse.domain.workspace.Workspace;
import com.prep.taskpulse.domain.workspace.repository.WorkspaceRepository;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.RollbackException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
public class TaskOptimisticLockIntegrationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Autowired private WorkspaceRepository workspaceRepository;

  @Autowired private ProjectRepository projectRepository;

  @Autowired private TaskRepository taskRepository;

  @Autowired private UserRepository userRepository;

  private UUID taskId;

  @BeforeEach
  void setUp() {
    Task task = createPersistedTask();
    taskId = task.getId();
  }

  private Task createPersistedTask() {

    User user =
        User.createUser(
            "Joseph", "joseph-" + UUID.randomUUID() + "@gmail.com", "testpasswordhash", Role.USER);
    User savedUser = userRepository.save(user);

    Workspace workspace = Workspace.create("workspace one", savedUser);
    Workspace savedWorkspace = workspaceRepository.save(workspace);

    Project project = Project.create("project", "proj-desc", savedWorkspace);
    Project savedProject = projectRepository.save(project);

    Task task = Task.create("task", "task-desc", savedProject, TaskPriority.MEDIUM, Instant.now());
    Task savedTask = taskRepository.saveAndFlush(task);
    return savedTask;
  }

  @Test
  void concurrentUpdates_whenSecondTransactionUsesStaleVersion_throwsOptimisticLockException() {

    var entityManagerA = entityManagerFactory.createEntityManager();
    var entityManagerB = entityManagerFactory.createEntityManager();

    var transactionA = entityManagerA.getTransaction();
    var transactionB = entityManagerB.getTransaction();
    try {
      transactionA.begin();
      transactionB.begin();

      Task taskA = entityManagerA.find(Task.class, taskId);
      Task taskB = entityManagerB.find(Task.class, taskId);

      assertThat(taskA).isNotNull();
      assertThat(taskB).isNotNull();
      assertThat(taskA.getVersion()).isEqualTo(taskB.getVersion());

      taskA.rename("task renamed by A");
      transactionA.commit(); // can use taskrepo.saveandFlush
      assertThat(taskA.getVersion()).isGreaterThan(taskB.getVersion());

      taskB.rename("task renamed again by B");
      assertThatThrownBy(transactionB::commit)
          .isInstanceOfAny(RollbackException.class, OptimisticLockException.class);

      Task persistedTask =
          taskRepository.findById(taskId).orElseThrow(); // this uses a separate entity manager.
      assertThat(persistedTask.getTitle()).isEqualTo("task renamed by A");
    } finally {
      if (transactionA.isActive()) transactionA.rollback();
      if (transactionB.isActive()) transactionB.rollback();
      entityManagerA.close();
      entityManagerB.close();
    }
  }
}

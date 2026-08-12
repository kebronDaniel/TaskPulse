package com.prep.taskpulse.domain.project.repository;

import com.prep.taskpulse.domain.project.Project;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
  Optional<Project> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

  List<Project> findByWorkspaceId(UUID workspaceId);

  boolean existsByIdAndWorkspaceId(UUID id, UUID workspaceId);
}

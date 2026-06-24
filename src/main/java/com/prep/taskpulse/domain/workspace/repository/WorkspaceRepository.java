package com.prep.taskpulse.domain.workspace.repository;

import com.prep.taskpulse.domain.workspace.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {
    Optional<Workspace> findByIdAndOwnerId(UUID id, UUID ownerId);
    List<Workspace> findByOwnerId(UUID id);
    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);
}

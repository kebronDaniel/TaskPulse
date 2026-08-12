package com.prep.taskpulse.domain.comment.repository;

import com.prep.taskpulse.domain.comment.Comment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
  Optional<Comment> findByIdAndTaskId(UUID id, UUID taskId);

  List<Comment> findByAuthorId(UUID authorId);

  boolean existsByIdAndAuthorId(UUID id, UUID authorId);
}

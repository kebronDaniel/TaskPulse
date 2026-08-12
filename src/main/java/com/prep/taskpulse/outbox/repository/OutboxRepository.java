package com.prep.taskpulse.outbox.repository;

import com.prep.taskpulse.outbox.OutboxEvent;
import com.prep.taskpulse.outbox.OutboxStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

  @Query(
      value =
          """
            select * from outbox_events
                        where status = 'PENDING'
                        order by occurred_at asc
                        limit :batchSize
                        for update skip locked
            """,
      nativeQuery = true)
  List<OutboxEvent> findPendingEventsForUpdate(@Param("batchSize") int batchSize);

  List<OutboxEvent> findByStatusAndClaimedAtBefore(OutboxStatus status, Instant claimedAtBefore);

  double countByStatus(OutboxStatus status);
}

package com.aidenwu.esl.panpan.consumer.repository;

import com.aidenwu.esl.panpan.consumer.domain.CommandStatus;
import com.aidenwu.esl.panpan.consumer.domain.CommandTask;
import com.aidenwu.esl.panpan.consumer.domain.MessageType;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommandTaskRepository extends JpaRepository<CommandTask, Long> {

    Optional<CommandTask> findByTaskUuid(String taskUuid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from CommandTask t where t.taskUuid = :taskUuid")
    Optional<CommandTask> findByTaskUuidForUpdate(@Param("taskUuid") String taskUuid);

    List<CommandTask> findTop10ByTagIdAndStatusInOrderByCreatedAtDesc(
            String tagId,
            Collection<CommandStatus> statuses
    );

    List<CommandTask> findTop200ByStatusInAndDeadlineAtBefore(
            Collection<CommandStatus> statuses,
            Instant deadline
    );

    List<CommandTask> findByMessageType(MessageType messageType);
}

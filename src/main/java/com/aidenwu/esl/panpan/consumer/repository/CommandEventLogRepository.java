package com.aidenwu.esl.panpan.consumer.repository;

import com.aidenwu.esl.panpan.consumer.domain.CommandEventLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommandEventLogRepository extends JpaRepository<CommandEventLog, Long> {

    List<CommandEventLog> findByTaskUuidOrderByCreatedAtAsc(String taskUuid);
}

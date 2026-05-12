package com.aidenwu.esl.panpan.consumer.repository;

import com.aidenwu.esl.panpan.consumer.domain.ApDevice;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApDeviceRepository extends JpaRepository<ApDevice, Long> {

    Optional<ApDevice> findByApCode(String apCode);
}

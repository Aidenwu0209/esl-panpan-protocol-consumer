package com.aidenwu.esl.panpan.consumer.repository;

import com.aidenwu.esl.panpan.consumer.domain.EslTag;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EslTagRepository extends JpaRepository<EslTag, Long> {

    Optional<EslTag> findByTagId(String tagId);
}

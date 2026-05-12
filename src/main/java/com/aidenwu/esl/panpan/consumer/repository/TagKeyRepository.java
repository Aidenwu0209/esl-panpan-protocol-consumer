package com.aidenwu.esl.panpan.consumer.repository;

import com.aidenwu.esl.panpan.consumer.domain.TagKey;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagKeyRepository extends JpaRepository<TagKey, Long> {

    Optional<TagKey> findByTagId(String tagId);
}

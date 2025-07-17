package com.github.sdms.repository;

import com.github.sdms.model.IdSequenceEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface IdSequenceRepository extends JpaRepository<IdSequenceEntity, Long> {
    Optional<IdSequenceEntity> findByType(String type);

    @Modifying
    @Query("UPDATE IdSequenceEntity e SET e.currentValue = :newVal WHERE e.type = :type AND e.currentValue = :oldVal")
    int updateCurrentValue(String type, Long oldVal, Long newVal);
}



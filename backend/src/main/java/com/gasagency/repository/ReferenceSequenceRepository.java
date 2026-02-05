package com.gasagency.repository;

import com.gasagency.entity.ReferenceSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.Optional;

@Repository
public interface ReferenceSequenceRepository extends JpaRepository<ReferenceSequence, Long> {
    Optional<ReferenceSequence> findBySeqKey(String seqKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReferenceSequence r WHERE r.seqKey = :seqKey")
    Optional<ReferenceSequence> findBySeqKeyForUpdate(@Param("seqKey") String seqKey);
}

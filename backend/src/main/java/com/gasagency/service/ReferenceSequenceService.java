package com.gasagency.service;

import com.gasagency.entity.ReferenceSequence;
import com.gasagency.repository.ReferenceSequenceRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReferenceSequenceService {
    private final ReferenceSequenceRepository repository;

    public ReferenceSequenceService(ReferenceSequenceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public long next(String seqKey) {
        int attempts = 0;
        while (attempts < 3) {
            attempts++;
            try {
                ReferenceSequence seq = repository.findBySeqKeyForUpdate(seqKey).orElse(null);
                if (seq == null) {
                    seq = new ReferenceSequence(seqKey, 0L);
                    seq = repository.save(seq);
                }

                long next = (seq.getValue() != null ? seq.getValue() : 0L) + 1L;
                seq.setValue(next);
                repository.save(seq);
                return next;
            } catch (DataIntegrityViolationException | ObjectOptimisticLockingFailureException e) {
                if (attempts >= 3) {
                    throw e;
                }
            }
        }

        throw new IllegalStateException("Failed to generate reference sequence after retries.");
    }
}


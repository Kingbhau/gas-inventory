package com.gasagency.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(name = "reference_sequence", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "seq_key" }, name = "uq_reference_sequence_key")
})
public class ReferenceSequence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "seq_key", nullable = false, length = 200)
    private String seqKey;

    @Column(nullable = false)
    private Long value = 0L;

    public ReferenceSequence() {
    }

    public ReferenceSequence(String seqKey, Long value) {
        this.seqKey = seqKey;
        this.value = value != null ? value : 0L;
    }

    public Long getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getSeqKey() {
        return seqKey;
    }

    public void setSeqKey(String seqKey) {
        this.seqKey = seqKey;
    }

    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }
}


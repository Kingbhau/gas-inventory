package com.gasagency.repository;

import com.gasagency.entity.CylinderVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CylinderVariantRepository extends JpaRepository<CylinderVariant, Long> {
    Optional<CylinderVariant> findByName(String name);

    List<CylinderVariant> findAllByActive(Boolean active);
}

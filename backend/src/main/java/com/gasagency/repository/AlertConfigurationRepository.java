package com.gasagency.repository;

import com.gasagency.entity.AlertConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertConfigurationRepository extends JpaRepository<AlertConfiguration, Long> {
    Optional<AlertConfiguration> findByAlertType(String alertType);

    List<AlertConfiguration> findByEnabled(Boolean enabled);
}


package com.gasagency.repository;

import com.gasagency.entity.PaymentMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentModeRepository extends JpaRepository<PaymentMode, Long> {

    List<PaymentMode> findByIsActiveTrue();

    Optional<PaymentMode> findByName(String name);

    Optional<PaymentMode> findByCode(String code);

    @Query("SELECT p.name FROM PaymentMode p WHERE p.isActive = true ORDER BY p.name")
    List<String> findActiveNames();
}


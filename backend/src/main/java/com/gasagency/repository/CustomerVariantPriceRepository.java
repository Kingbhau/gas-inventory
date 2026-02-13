package com.gasagency.repository;

import com.gasagency.entity.CustomerVariantPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerVariantPriceRepository extends JpaRepository<CustomerVariantPrice, Long> {

    // Find price for a specific customer and variant
    Optional<CustomerVariantPrice> findByCustomerIdAndVariantId(Long customerId, Long variantId);

    // Find all prices for a customer
    List<CustomerVariantPrice> findByCustomerId(Long customerId);

    // Find all prices for a variant
    List<CustomerVariantPrice> findByVariantId(Long variantId);

    // Check if pricing exists for customer-variant combination
    boolean existsByCustomerIdAndVariantId(Long customerId, Long variantId);

    // Delete pricing for a customer-variant combination
    void deleteByCustomerIdAndVariantId(Long customerId, Long variantId);

    // Delete all prices for a customer (when customer is deleted)
    void deleteByCustomerId(Long customerId);
}


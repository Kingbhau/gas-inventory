package com.gasagency.repository;

import com.gasagency.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findAllByActive(Boolean active);
    Page<Customer> findAllByActive(Boolean active, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.active = true AND (" +
            "LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "c.mobile LIKE CONCAT('%', :search, '%'))")
    Page<Customer> searchActive(@Param("search") String search, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.active = true AND (" +
            "LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "c.mobile LIKE CONCAT('%', :search, '%'))")
    List<Customer> searchActiveList(@Param("search") String search);

    Optional<Customer> findByMobile(String mobile);

    long countByActive(Boolean active);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Customer c WHERE c.id = :id")
    Optional<Customer> findByIdForUpdate(@Param("id") Long id);
}

package com.gasagency.repository;

import com.gasagency.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findAllByActive(Boolean active);

    Optional<Customer> findByMobile(String mobile);
}

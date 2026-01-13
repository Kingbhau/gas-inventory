package com.gasagency.repository;

import com.gasagency.entity.Sale;
import com.gasagency.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long>, SaleRepositoryCustom {
        List<Sale> findByCustomer(Customer customer);

        Page<Sale> findByCustomer(Customer customer, Pageable pageable);

}

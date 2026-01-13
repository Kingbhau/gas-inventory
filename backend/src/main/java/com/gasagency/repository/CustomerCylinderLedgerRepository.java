package com.gasagency.repository;

import com.gasagency.entity.CustomerCylinderLedger;
import com.gasagency.entity.Customer;
import com.gasagency.entity.CylinderVariant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerCylinderLedgerRepository extends JpaRepository<CustomerCylinderLedger, Long> {
        List<CustomerCylinderLedger> findByCustomer(Customer customer);

        Page<CustomerCylinderLedger> findByCustomer(Customer customer, Pageable pageable);

        @Query("SELECT l FROM CustomerCylinderLedger l WHERE l.customer = :customer " +
                        "AND l.variant = :variant ORDER BY l.id ASC")
        List<CustomerCylinderLedger> findByCustomerAndVariant(@Param("customer") Customer customer,
                        @Param("variant") CylinderVariant variant);

        // Get latest ledger entry for a customer-variant combination
        @Query(value = "SELECT l FROM CustomerCylinderLedger l WHERE l.customer.id = :customerId " +
                        "AND l.variant.id = :variantId ORDER BY l.id DESC")
        List<CustomerCylinderLedger> findLatestLedger(@Param("customerId") Long customerId,
                        @Param("variantId") Long variantId);

        List<CustomerCylinderLedger> findByVariant(CylinderVariant variant);
}

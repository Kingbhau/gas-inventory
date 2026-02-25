package com.gasagency.repository;

import com.gasagency.entity.CustomerLedgerPaymentSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerLedgerPaymentSplitRepository extends JpaRepository<CustomerLedgerPaymentSplit, Long> {
    List<CustomerLedgerPaymentSplit> findByLedgerId(Long ledgerId);
    List<CustomerLedgerPaymentSplit> findByLedgerIdIn(List<Long> ledgerIds);
}

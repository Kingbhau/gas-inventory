package com.gasagency.repository;

import com.gasagency.entity.BankAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    @Query("SELECT ba FROM BankAccount ba WHERE ba.isActive = true ORDER BY ba.createdDate DESC")
    List<BankAccount> findActiveAccounts();

    @Query("SELECT ba FROM BankAccount ba")
    Page<BankAccount> findAllAccounts(Pageable pageable);

    @Query("SELECT ba FROM BankAccount ba WHERE ba.accountNumber = :accountNumber")
    Optional<BankAccount> findByAccountNumber(@Param("accountNumber") String accountNumber);

    @Query("SELECT COUNT(ba) > 0 FROM BankAccount ba WHERE ba.isActive = true")
    boolean hasActiveAccounts();
}


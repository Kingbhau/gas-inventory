package com.gasagency.repository;

import com.gasagency.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    Optional<Warehouse> findByName(String name);

    @Query("SELECT w FROM Warehouse w WHERE w.status = 'ACTIVE' ORDER BY w.name ASC")
    List<Warehouse> findAllActive();

    @Query("SELECT w FROM Warehouse w ORDER BY w.name ASC")
    List<Warehouse> findAllOrderByName();

    boolean existsByNameAndIdNot(String name, Long id);
}


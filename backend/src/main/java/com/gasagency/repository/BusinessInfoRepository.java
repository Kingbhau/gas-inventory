package com.gasagency.repository;

import com.gasagency.entity.BusinessInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessInfoRepository extends JpaRepository<BusinessInfo, Long> {
}


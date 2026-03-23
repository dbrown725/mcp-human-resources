package com.megacorp.humanresources.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.megacorp.humanresources.entity.BenefitsConfig;

public interface BenefitsConfigRepository extends JpaRepository<BenefitsConfig, Long> {

    List<BenefitsConfig> findByPlanType(String planType);

    List<BenefitsConfig> findByEnrollmentStatus(Boolean enrollmentStatus);
}

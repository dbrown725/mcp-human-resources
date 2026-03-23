package com.megacorp.humanresources.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.megacorp.humanresources.entity.EmployeeBenefit;

public interface EmployeeBenefitRepository extends JpaRepository<EmployeeBenefit, Long> {

    List<EmployeeBenefit> findByEmployee_EmployeeId(Long employeeId);

    List<EmployeeBenefit> findByBenefitsPlan_PlanId(Long planId);

    List<EmployeeBenefit> findByBenefitsPlan_PlanType(String planType);

    Optional<EmployeeBenefit> findByEmployee_EmployeeIdAndBenefitsPlan_PlanType(Long employeeId, String planType);

    Optional<EmployeeBenefit> findByEmployee_EmployeeIdAndBenefitsPlan_PlanId(Long employeeId, Long planId);

    void deleteByEmployee_EmployeeIdAndBenefitsPlan_PlanId(Long employeeId, Long planId);

    long countByBenefitsPlan_PlanId(Long planId);

    long countByBenefitsPlan_PlanType(String planType);
}

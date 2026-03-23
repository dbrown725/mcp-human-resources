package com.megacorp.humanresources.service;

import java.util.List;

import com.megacorp.humanresources.entity.BenefitsConfig;
import com.megacorp.humanresources.entity.EmployeeBenefit;

public interface BenefitsService {

    // --- Plan operations ---

    List<BenefitsConfig> getAllPlans();

    BenefitsConfig getPlanById(Long planId);

    List<BenefitsConfig> getPlansByType(String planType);

    // --- Enrollment operations ---

    List<EmployeeBenefit> getEnrollmentsByEmployeeId(Long employeeId);

    List<EmployeeBenefit> getEnrollmentsByPlanId(Long planId);

    List<EmployeeBenefit> getEnrollmentsByPlanType(String planType);

    EmployeeBenefit enrollEmployee(Long employeeId, Long planId);

    void unenrollEmployee(Long employeeId, Long planId);

    long countEnrollmentsByPlanId(Long planId);

    long countEnrollmentsByPlanType(String planType);
}

package com.megacorp.humanresources.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.megacorp.humanresources.entity.BenefitsConfig;
import com.megacorp.humanresources.entity.EmployeeBenefit;
import com.megacorp.humanresources.service.BenefitsService;

@RestController
@RequestMapping("/benefits")
public class BenefitsController {

    private static final Logger log = LoggerFactory.getLogger(BenefitsController.class);

    @Autowired
    private BenefitsService benefitsService;

    // ==================== Plan Endpoints ====================

    @GetMapping("/plans")
    public List<BenefitsConfig> getAllPlans() {
        log.debug("Entering getAllPlans");
        List<BenefitsConfig> plans = benefitsService.getAllPlans();
        log.info("Fetched {} benefit plans", plans.size());
        return plans;
    }

    @GetMapping("/plans/{planId}")
    public BenefitsConfig getPlanById(@PathVariable Long planId) {
        log.debug("Entering getPlanById with planId={}", planId);
        BenefitsConfig plan = benefitsService.getPlanById(planId);
        log.info("Retrieved plan with id={}", planId);
        return plan;
    }

    @GetMapping("/plans/type/{planType}")
    public List<BenefitsConfig> getPlansByType(@PathVariable String planType) {
        log.debug("Entering getPlansByType with planType={}", planType);
        List<BenefitsConfig> plans = benefitsService.getPlansByType(planType);
        log.info("Fetched {} plans of type={}", plans.size(), planType);
        return plans;
    }

    // ==================== Enrollment Endpoints ====================

    @GetMapping("/enrollments/employee/{employeeId}")
    public List<EmployeeBenefit> getEnrollmentsByEmployeeId(@PathVariable Long employeeId) {
        log.debug("Entering getEnrollmentsByEmployeeId with employeeId={}", employeeId);
        List<EmployeeBenefit> enrollments = benefitsService.getEnrollmentsByEmployeeId(employeeId);
        log.info("Fetched {} enrollments for employeeId={}", enrollments.size(), employeeId);
        return enrollments;
    }

    @GetMapping("/enrollments/plan/{planId}")
    public List<EmployeeBenefit> getEnrollmentsByPlanId(@PathVariable Long planId) {
        log.debug("Entering getEnrollmentsByPlanId with planId={}", planId);
        List<EmployeeBenefit> enrollments = benefitsService.getEnrollmentsByPlanId(planId);
        log.info("Fetched {} enrollments for planId={}", enrollments.size(), planId);
        return enrollments;
    }

    @GetMapping("/enrollments/type/{planType}")
    public List<EmployeeBenefit> getEnrollmentsByPlanType(@PathVariable String planType) {
        log.debug("Entering getEnrollmentsByPlanType with planType={}", planType);
        List<EmployeeBenefit> enrollments = benefitsService.getEnrollmentsByPlanType(planType);
        log.info("Fetched {} enrollments for planType={}", enrollments.size(), planType);
        return enrollments;
    }

    @GetMapping("/enrollments/plan/{planId}/count")
    public long countEnrollmentsByPlanId(@PathVariable Long planId) {
        log.debug("Entering countEnrollmentsByPlanId with planId={}", planId);
        return benefitsService.countEnrollmentsByPlanId(planId);
    }

    @GetMapping("/enrollments/type/{planType}/count")
    public long countEnrollmentsByPlanType(@PathVariable String planType) {
        log.debug("Entering countEnrollmentsByPlanType with planType={}", planType);
        return benefitsService.countEnrollmentsByPlanType(planType);
    }

    @PostMapping("/enrollments")
    public EmployeeBenefit enrollEmployee(@RequestBody EnrollmentRequest request) {
        log.debug("Entering enrollEmployee for employeeId={}, planId={}", request.employeeId, request.planId);
        EmployeeBenefit enrollment = benefitsService.enrollEmployee(request.employeeId, request.planId);
        log.info("Employee {} enrolled in plan {}", request.employeeId, request.planId);
        return enrollment;
    }

    @DeleteMapping("/enrollments/employee/{employeeId}/plan/{planId}")
    public String unenrollEmployee(@PathVariable Long employeeId, @PathVariable Long planId) {
        log.debug("Entering unenrollEmployee for employeeId={}, planId={}", employeeId, planId);
        benefitsService.unenrollEmployee(employeeId, planId);
        log.info("Employee {} unenrolled from plan {}", employeeId, planId);
        return "Unenrolled Successfully";
    }

    private static class EnrollmentRequest {
        public Long employeeId;
        public Long planId;
    }
}

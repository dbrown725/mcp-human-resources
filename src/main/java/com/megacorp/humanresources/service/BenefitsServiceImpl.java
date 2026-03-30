package com.megacorp.humanresources.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.megacorp.humanresources.entity.BenefitsConfig;
import com.megacorp.humanresources.entity.Employee;
import com.megacorp.humanresources.entity.EmployeeBenefit;
import com.megacorp.humanresources.exceptions.ResourceNotFoundException;
import com.megacorp.humanresources.repository.BenefitsConfigRepository;
import com.megacorp.humanresources.repository.EmployeeBenefitRepository;
import com.megacorp.humanresources.repository.EmployeeRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation for managing Benefits and Employee Benefit Enrollments.
 *
 * Main Methods:
 * - getAllPlans: Retrieves all benefit plans.
 * - getPlanById: Retrieves a single plan by ID.
 * - getPlansByType: Retrieves plans filtered by type (Medical, Dental, Vision).
 * - getEnrollmentsByEmployeeId: Retrieves all benefit enrollments for a specific employee.
 * - getEnrollmentsByPlanId: Retrieves all enrollments for a specific plan.
 * - getEnrollmentsByPlanType: Retrieves all enrollments by plan type.
 * - enrollEmployee: Enrolls an employee in a benefit plan (one plan per type enforced).
 * - unenrollEmployee: Removes an employee's enrollment from a specific plan.
 * - countEnrollmentsByPlanId: Counts enrollments for a specific plan.
 * - countEnrollmentsByPlanType: Counts enrollments by plan type.
 */
@Service
@Slf4j
public class BenefitsServiceImpl implements BenefitsService {

    private static final Logger logger = LoggerFactory.getLogger(BenefitsServiceImpl.class);

    @Autowired
    private BenefitsConfigRepository benefitsConfigRepository;

    @Autowired
    private EmployeeBenefitRepository employeeBenefitRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    // ==================== Plan Operations ====================

    @Override
    @Tool(name = "get_all_benefit_plans", description = "Get all available benefit plans including Medical, Dental, and Vision options.")
    public List<BenefitsConfig> getAllPlans() {
        logger.debug("Fetching all benefit plans");
        List<BenefitsConfig> plans = benefitsConfigRepository.findAll();
        logger.info("Retrieved {} benefit plans", plans.size());
        return plans;
    }

    @Override
    @Tool(name = "get_benefit_plan_by_id", description = "Get a specific benefit plan by its plan ID.")
    public BenefitsConfig getPlanById(@ToolParam(description = "The unique ID of the benefit plan") Long planId) {
        logger.debug("Fetching benefit plan with id={}", planId);
        return benefitsConfigRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("BenefitsConfig", planId));
    }

    @Override
    @Tool(name = "get_benefit_plans_by_type", description = "Get all benefit plans of a specific type. Valid types are: Medical, Dental, Vision.")
    public List<BenefitsConfig> getPlansByType(@ToolParam(description = "The plan type to filter by (Medical, Dental, or Vision)") String planType) {
        logger.debug("Fetching benefit plans of type={}", planType);
        List<BenefitsConfig> plans = benefitsConfigRepository.findByPlanType(planType);
        logger.info("Retrieved {} benefit plans of type={}", plans.size(), planType);
        return plans;
    }

    // ==================== Enrollment Operations ====================
    

    @Override
    @Tool(name = "get_employee_benefit_enrollments", description = "Get all benefit enrollments for a specific employee by their employee ID. Returns the list of plans the employee is enrolled in.")
    public List<EmployeeBenefit> getEnrollmentsByEmployeeId(@ToolParam(description = "The employee ID to look up enrollments for") Long employeeId) {
        logger.debug("Fetching enrollments for employeeId={}", employeeId);
        // Verify employee exists
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee", employeeId);
        }
        List<EmployeeBenefit> enrollments = employeeBenefitRepository.findByEmployee_EmployeeId(employeeId);
        logger.info("Retrieved {} enrollments for employeeId={}", enrollments.size(), employeeId);
        return enrollments;
    }

    @Override
    @Tool(name = "get_enrollments_by_plan_id", description = "Get all employee enrollments for a specific benefit plan by plan ID.")
    public List<EmployeeBenefit> getEnrollmentsByPlanId(@ToolParam(description = "The plan ID to look up enrollments for") Long planId) {
        logger.debug("Fetching enrollments for planId={}", planId);
        List<EmployeeBenefit> enrollments = employeeBenefitRepository.findByBenefitsPlan_PlanId(planId);
        logger.info("Retrieved {} enrollments for planId={}", enrollments.size(), planId);
        return enrollments;
    }

    @Override
    @Tool(name = "get_enrollments_by_plan_type", description = "Get all employee enrollments for a specific plan type. Valid types are: Medical, Dental, Vision.")
    public List<EmployeeBenefit> getEnrollmentsByPlanType(@ToolParam(description = "The plan type to filter enrollments by (Medical, Dental, or Vision)") String planType) {
        logger.debug("Fetching enrollments for planType={}", planType);
        List<EmployeeBenefit> enrollments = employeeBenefitRepository.findByBenefitsPlan_PlanType(planType);
        logger.info("Retrieved {} enrollments for planType={}", enrollments.size(), planType);
        return enrollments;
    }

    @Override
    @Transactional
    @Tool(name = "enroll_employee_in_benefit", description = "Enroll an employee in a benefit plan. Each employee can only have one plan per plan type (Medical, Dental, Vision). If the employee is already enrolled in a plan of the same type, the existing enrollment will be replaced.")
    public EmployeeBenefit enrollEmployee(
            @ToolParam(description = "The employee ID to enroll") Long employeeId,
            @ToolParam(description = "The plan ID to enroll the employee in") Long planId) {

        logger.debug("Enrolling employeeId={} in planId={}", employeeId, planId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));

        BenefitsConfig plan = benefitsConfigRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("BenefitsConfig", planId));

        // Check if employee already has a plan of this type — replace it if so
        Optional<EmployeeBenefit> existing = employeeBenefitRepository
                .findByEmployee_EmployeeIdAndBenefitsPlan_PlanType(employeeId, plan.getPlanType());

        if (existing.isPresent()) {
            EmployeeBenefit current = existing.get();
            if (current.getBenefitsPlan().getPlanId().equals(planId)) {
                logger.info("Employee {} is already enrolled in plan {}", employeeId, planId);
                return current;
            }
            logger.info("Replacing existing {} enrollment for employeeId={}: planId={} -> planId={}",
                    plan.getPlanType(), employeeId, current.getBenefitsPlan().getPlanId(), planId);
            employeeBenefitRepository.delete(current);
        }

        EmployeeBenefit enrollment = EmployeeBenefit.builder()
            .employee(employee)
            .benefitsPlan(plan)
            .enrollmentDate(LocalDate.now())
            .build();

        EmployeeBenefit saved = employeeBenefitRepository.save(enrollment);
        logger.info("Employee {} enrolled in plan {} ({})", employeeId, planId, plan.getPlanName());
        return saved;
    }

    @Override
    @Transactional
    @Tool(name = "unenroll_employee_from_benefit", description = "Remove an employee's enrollment from a specific benefit plan.")
    public void unenrollEmployee(
            @ToolParam(description = "The employee ID to unenroll") Long employeeId,
            @ToolParam(description = "The plan ID to unenroll the employee from") Long planId) {

        logger.debug("Unenrolling employeeId={} from planId={}", employeeId, planId);

        EmployeeBenefit enrollment = employeeBenefitRepository
                .findByEmployee_EmployeeIdAndBenefitsPlan_PlanId(employeeId, planId)
                .orElseThrow(() -> new ResourceNotFoundException("EmployeeBenefit",
                        "employeeId=" + employeeId + ", planId=" + planId));

        employeeBenefitRepository.delete(enrollment);
        logger.info("Employee {} unenrolled from plan {}", employeeId, planId);
    }

    @Override
    @Tool(name = "count_enrollments_by_plan_id", description = "Count the number of employees enrolled in a specific benefit plan.")
    public long countEnrollmentsByPlanId(@ToolParam(description = "The plan ID to count enrollments for") Long planId) {
        logger.debug("Counting enrollments for planId={}", planId);
        long count = employeeBenefitRepository.countByBenefitsPlan_PlanId(planId);
        logger.info("Plan {} has {} enrollments", planId, count);
        return count;
    }

    @Override
    @Tool(name = "count_enrollments_by_plan_type", description = "Count the number of employees enrolled in plans of a specific type (Medical, Dental, or Vision).")
    public long countEnrollmentsByPlanType(@ToolParam(description = "The plan type to count enrollments for") String planType) {
        logger.debug("Counting enrollments for planType={}", planType);
        long count = employeeBenefitRepository.countByBenefitsPlan_PlanType(planType);
        logger.info("Plan type {} has {} enrollments", planType, count);
        return count;
    }
}

package com.megacorp.humanresources.service;

import org.springframework.web.multipart.MultipartFile;

public interface EmployeeOnboardingService {

    String generateWelcomeMessage(String employeeName, String position, String startDate);

    /**
     * Executes the full employee onboarding workflow with LLM-as-a-Judge validation at each step.
     *
     * Steps: 1) Insert DB records from CSV, 2) Upload image to GCS, 3) Generate temp badge,
     * 4-7) Draft emails (manager, employee, facilities, IT), 8) Clean up GCS onboarding folder.
     *
     * Each step is validated by a judge LLM. Failed steps are retried up to a configurable max.
     * On unrecoverable failure, all changes are rolled back (DB, GCS, draft emails).
     *
     * @param employeeImage  the employee's photo file
     * @param csvFile        CSV containing employee and address data (one header + one data row)
     * @param firstName      employee's first name
     * @param lastName       employee's last name
     * @param personalEmail  employee's personal email address
     * @return a summary report of all completed steps, or a failure report with rollback details
     */
    String executeOnboarding(MultipartFile employeeImage, MultipartFile csvFile,
                             String firstName, String lastName, String personalEmail);
}

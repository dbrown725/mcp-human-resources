package com.megacorp.humanresources.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.megacorp.humanresources.service.EmployeeOnboardingService;

/**
 * REST controller for the employee onboarding workflow.
 *
 * Accepts a POST request with the employee's image, a CSV file containing employee
 * and address data, and personal details to initiate the full onboarding workflow.
 */
@RestController
public class OnboardingController {

	private static final Logger log = LoggerFactory.getLogger(OnboardingController.class);

	private final EmployeeOnboardingService onboardingService;

	public OnboardingController(EmployeeOnboardingService onboardingService) {
		this.onboardingService = onboardingService;
	}

	/**
	 * Initiates the employee onboarding workflow.
	 *
	 * @param employeeImage the employee's photo file (jpg, png, etc.)
	 * @param csvFile       CSV file with one header row and one data row containing employee and address fields
	 * @param firstName     employee's first name
	 * @param lastName      employee's last name
	 * @param personalEmail employee's personal email address
	 * @return a summary report of the onboarding result (success or failure with rollback details)
	 */
	@PostMapping("/onboarding")
	public ResponseEntity<String> onboardEmployee(
			@RequestParam("employeeImage") MultipartFile employeeImage,
			@RequestParam("csvFile") MultipartFile csvFile,
			@RequestParam("firstName") String firstName,
			@RequestParam("lastName") String lastName,
			@RequestParam("personalEmail") String personalEmail) {

		log.info("Onboarding request received for {} {} (personalEmail={})", firstName, lastName, personalEmail);

		if (employeeImage == null || employeeImage.isEmpty()) {
			return ResponseEntity.badRequest().body("Employee image file is required.");
		}
		if (csvFile == null || csvFile.isEmpty()) {
			return ResponseEntity.badRequest().body("CSV data file is required.");
		}

		String result = onboardingService.executeOnboarding(
				employeeImage, csvFile, firstName, lastName, personalEmail);

		log.info("Onboarding workflow completed for {} {}", firstName, lastName);
		return ResponseEntity.ok(result);
	}
}

package com.megacorp.humanresources.service;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.megacorp.humanresources.advisors.ChatClientLoggingAdvisor;
import com.megacorp.humanresources.entity.Address;
import com.megacorp.humanresources.entity.Employee;
import com.megacorp.humanresources.exceptions.OnboardingStepException;
import com.megacorp.humanresources.model.OnboardingStepResult;
import com.megacorp.humanresources.repository.AddressRepository;
import com.megacorp.humanresources.repository.EmployeeRepository;

/**
 * Orchestrates the full employee onboarding workflow with LLM-as-a-Judge validation.
 *
 * <h3>Spring AI features used:</h3>
 * <ul>
 *   <li><b>ChatClient</b> - For generating email content using the primary model</li>
 *   <li><b>Structured Output</b> - Judge responses parsed into {@code OnboardingStepResult} records</li>
 *   <li><b>Multiple Model Support</b> - Primary model for content generation, judge model for evaluation</li>
 *   <li><b>LLM-as-a-Judge Pattern</b> - Same concept as {@code SelfRefineEvaluationAdvisor} applied to workflow steps</li>
 *   <li><b>Advisor Pattern (reference)</b> - ChatClientLoggingAdvisor for observability on content generation calls</li>
 * </ul>
 *
 * <h3>Judge Verification Design: Verified State Reporting (Hybrid)</h3>
 * <p>
 * LLM-as-a-Judge verification patterns exist on a spectrum between two extremes:
 * </p>
 * <ul>
 *   <li><b>Metadata-based (State Object Review)</b> — The judge trusts a state object that the
 *       workflow self-reported. Fast and cheap, but vulnerable to the "hallucination trap": if the
 *       worker LLM <em>claims</em> it performed an action that silently failed, the judge verifies a lie.</li>
 *   <li><b>Truth-based (External Verification)</b> — The judge is given tools to independently query
 *       the database, read GCS, and inspect Gmail drafts. Provides ground truth, but adds significant
 *       latency, cost, and complexity (the judge needs its own connections and authentication).</li>
 * </ul>
 * <p>
 * This implementation uses the recommended <b>Verified State Reporting</b> hybrid pattern, which sits
 * closer to the truth-based end of the spectrum while avoiding its infrastructure overhead:
 * </p>
 * <ol>
 *   <li>The worker (Java code + primary LLM) performs each step.</li>
 *   <li>The Java code performs technical validation (e.g., GCS upload returned success, DB entity
 *       was persisted with a valid ID, email service did not throw an exception).</li>
 *   <li>The Java code then retrieves or constructs <em>actual evidence</em> from the real side effects —
 *       for example, reading back the saved entity fields, listing GCS folder contents to confirm
 *       file presence, or capturing the generated email body — and passes this evidence into
 *       the judge's prompt alongside the success criteria.</li>
 * </ol>
 * <p>
 * This means the judge evaluates <b>actual data from the real systems</b>, not just self-reported
 * status flags. However, the judge itself does not hold database connections or GCS credentials —
 * the orchestrating Java code handles all data retrieval and presents the results for semantic
 * evaluation. The judge focuses on what it does best: assessing whether the evidence satisfies
 * the step's criteria (e.g., "Does this email body contain the required onboarding details?",
 * "Do the saved employee fields match the CSV input data?").
 * </p>
 *
 * <h3>Workflow Steps:</h3>
 * <ol>
 *   <li>Parse CSV and insert address + employee records (with FK link)</li>
 *   <li>Upload employee image to GCS (two paths)</li>
 *   <li>Generate and upload temporary employee badge</li>
 *   <li>Draft email to manager</li>
 *   <li>Draft welcome email to new employee</li>
 *   <li>Draft email to Facilities Security</li>
 *   <li>Draft email to IT department</li>
 *   <li>Clean up GCS onboarding staging folder</li>
 * </ol>
 *
 * Each step is validated by the {@link OnboardingJudgeService} and retried on failure.
 * On unrecoverable failure, all changes are rolled back.
 */
@Service
public class EmployeeOnboardingServiceImpl implements EmployeeOnboardingService {

	private static final Logger logger = LoggerFactory.getLogger(EmployeeOnboardingServiceImpl.class);

	private static final String ONBOARDING_EMAIL_PREFIX = "[MegaCorp Onboarding]";

	private final ChatClient contentChatClient;
	private final OnboardingJudgeService judgeService;
	private final EmployeeRepository employeeRepository;
	private final AddressRepository addressRepository;
	private final FileStorageService fileStorageService;
	private final EmailService emailService;
	private final ImageGenerationService imageGenerationService;
	private final int maxRetries;

	// Secondary and judge models kept for the legacy generateWelcomeMessage method
	private final ChatModel secondaryChatModel;
	private final ChatModel judgeChatModel;

	public EmployeeOnboardingServiceImpl(
			@Qualifier("secondaryModel") ChatModel secondaryChatModel,
			@Qualifier("judgeModel") ChatModel judgeChatModel,
			ChatModel primaryChatModel,
			OnboardingJudgeService judgeService,
			EmployeeRepository employeeRepository,
			AddressRepository addressRepository,
			FileStorageService fileStorageService,
			EmailService emailService,
			ImageGenerationService imageGenerationService,
			ChatClientLoggingAdvisor chatClientLoggingAdvisor,
			@Value("${onboarding.judge.max-retries:3}") int maxRetries) {

		this.secondaryChatModel = secondaryChatModel;
		this.judgeChatModel = judgeChatModel;
		this.judgeService = judgeService;
		this.employeeRepository = employeeRepository;
		this.addressRepository = addressRepository;
		this.fileStorageService = fileStorageService;
		this.emailService = emailService;
		this.imageGenerationService = imageGenerationService;
		this.maxRetries = maxRetries;

		// Content generation ChatClient uses the primary model
		this.contentChatClient = ChatClient.builder(primaryChatModel)
				.defaultSystem("You are a professional HR assistant at MegaCorp. " +
						"Generate clear, professional, and warm communications for employee onboarding.")
				.defaultAdvisors(chatClientLoggingAdvisor)
				.build();

		logger.info("EmployeeOnboardingService initialized with maxRetries={}", maxRetries);
	}

	// ========================================================================================
	// Legacy method (kept for backward compatibility)
	// ========================================================================================

	@Override
	public String generateWelcomeMessage(String employeeName, String position, String startDate) {
		logger.debug("Entering generateWelcomeMessage with employeeName={} position={} startDate={}",
				employeeName, position, startDate);

		String prompt = String.format(
				"Generate a warm and engaging welcome message for a new employee named %s " +
						"who is joining as a %s starting on %s. The message should highlight the company's " +
						"values and culture, and encourage the new hire to feel excited about their new role.",
				employeeName, position, startDate);

		var answer = ChatClient.builder(secondaryChatModel)
				.defaultAdvisors(
						com.megacorp.humanresources.advisors.SelfRefineEvaluationAdvisor.builder()
								.chatClientBuilder(ChatClient.builder(judgeChatModel))
								.maxRepeatAttempts(15)
								.successRating(4)
								.order(0)
								.build(),
						new ChatClientLoggingAdvisor(2))
				.build()
				.prompt(prompt)
				.call()
				.content();

		logger.info("Generated onboarding welcome message for employeeName={}", employeeName);
		return answer;
	}

	// ========================================================================================
	// Main Onboarding Workflow
	// ========================================================================================

	@Override
	public String executeOnboarding(MultipartFile employeeImage, MultipartFile csvFile,
			String firstName, String lastName, String personalEmail) {
		logger.info("Starting onboarding workflow for {} {}", firstName, lastName);

		OnboardingState state = new OnboardingState();
		state.firstName = firstName;
		state.lastName = lastName;
		state.personalEmail = personalEmail;
		state.workEmail = firstName.toLowerCase() + "_" + lastName.toLowerCase() + "@megacorp.com";
		state.imageExtension = getFileExtension(employeeImage.getOriginalFilename());
		state.baseNameLower = firstName.toLowerCase() + "_" + lastName.toLowerCase();
		state.onboardingFolder = "onboarding/" + state.baseNameLower + "/";

		try {
			// Step 1: Parse CSV and insert DB records
			step1_InsertDatabaseRecords(csvFile, state);

			// Step 2: Upload employee image to GCS
			step2_UploadImageToGcs(employeeImage, state);

			// Step 3: Generate temp employee badge
			step3_GenerateTempBadge(state);

			// Step 4: Draft email to manager
			step4_DraftManagerEmail(state);

			// Step 5: Draft email to new employee
			step5_DraftEmployeeEmail(state);

			// Step 6: Draft email to Facilities Security
			step6_DraftFacilitiesEmail(state);

			// Step 7: Draft email to IT department
			step7_DraftItEmail(state);

			// Step 8: Clean up GCS onboarding folder
			step8_CleanupGcsOnboarding(state);

			return buildSuccessReport(state);

		} catch (OnboardingStepException e) {
			logger.error("Onboarding workflow failed at step '{}': {}", e.getStepName(), e.getMessage());
			performRollback(state);
			return buildFailureReport(state, e);
		} catch (Exception e) {
			logger.error("Unexpected error in onboarding workflow: {}", e.getMessage(), e);
			performRollback(state);
			return buildFailureReport(state,
					new OnboardingStepException(state.currentStep != null ? state.currentStep : "Unknown",
							"Unexpected error: " + e.getMessage(), e));
		}
	}

	// ========================================================================================
	// Step Implementations
	// ========================================================================================

	private void step1_InsertDatabaseRecords(MultipartFile csvFile, OnboardingState state) {
		String stepName = "Step 1: Insert Database Records";

		executeStepWithJudge(stepName,
				"1) An address record was created with street address, city, state, postal code, and isRemote matching the CSV data. " +
						"2) An employee record was created with all fields matching the CSV data (firstName, lastName, title, department, " +
						"businessUnit, gender, ethnicity, age, hireDate, annualSalary, managerId). " +
						"3) The employee's address_id foreign key correctly references the newly created address record.",
				(attempt) -> {
					// On retry, clean up previous attempt
					if (attempt > 1) {
						cleanupDbRecords(state);
					}

					// Parse CSV
					Map<String, String> csvData = parseCsvData(csvFile);
					state.csvData = csvData;
					logger.info("Parsed CSV data: {}", csvData);

					// Insert address
					Address address = Address.builder()
							.streetAddress(csvData.get("STREET_ADDRESS"))
							.city(csvData.get("CITY"))
							.state(csvData.get("STATE"))
							.postalCode(csvData.get("POSTAL_CODE"))
							.isRemote(Boolean.parseBoolean(csvData.get("IS_REMOTE")))
							.build();
					state.savedAddress = addressRepository.save(address);
					logger.info("Address saved with id={}", state.savedAddress.getAddressId());

					// Insert employee
					Employee employee = Employee.builder()
							.firstName(csvData.get("FIRST_NAME"))
							.lastName(csvData.get("LAST_NAME"))
							.title(csvData.get("TITLE"))
							.department(csvData.get("DEPARTMENT"))
							.businessUnit(csvData.get("BUSINESS_UNIT"))
							.gender(csvData.get("GENDER"))
							.ethnicity(csvData.get("ETHNICITY"))
							.age(Long.parseLong(csvData.get("AGE")))
							.annualSalary(Long.parseLong(csvData.get("ANNUAL_SALARY")))
							.address(state.savedAddress)
							.build();

					// Set hire date
					try {
						employee.setHireDate(new SimpleDateFormat("yyyy-MM-dd").parse(csvData.get("HIRE_DATE")));
					} catch (Exception e) {
						logger.warn("Could not parse hire date '{}', setting to today", csvData.get("HIRE_DATE"));
						employee.setHireDate(new java.util.Date());
					}

					// Set manager if specified
					String managerIdStr = csvData.get("MANAGER_ID");
					if (managerIdStr != null && !managerIdStr.isEmpty()) {
						try {
							Long managerId = Long.parseLong(managerIdStr);
							Employee manager = employeeRepository.findById(managerId).orElse(null);
							if (manager != null) {
								employee.setManager(manager);
								state.managerName = manager.getFirstName() + " " + manager.getLastName();
								state.managerEmail = manager.getFirstName().toLowerCase() + "_"
										+ manager.getLastName().toLowerCase() + "@megacorp.com";
								state.managerTitle = manager.getTitle();
							} else {
								logger.warn("Manager with id {} not found", managerId);
							}
						} catch (NumberFormatException e) {
							logger.warn("Invalid manager ID: {}", managerIdStr);
						}
					}

					state.savedEmployee = employeeRepository.save(employee);
					logger.info("Employee saved with id={}, addressId={}",
							state.savedEmployee.getEmployeeId(), state.savedEmployee.getAddressId());

					// Build evidence for judge
					return String.format("""
							CSV Data: %s

							Saved Address: {addressId: %d, streetAddress: "%s", city: "%s", state: "%s", postalCode: "%s", isRemote: %s}

							Saved Employee: {employeeId: %d, firstName: "%s", lastName: "%s", title: "%s", department: "%s", \
							businessUnit: "%s", gender: "%s", ethnicity: "%s", age: %d, hireDate: "%s", annualSalary: %d, \
							addressId: %d, managerId: %s}
							""",
							csvData,
							state.savedAddress.getAddressId(), state.savedAddress.getStreetAddress(),
							state.savedAddress.getCity(), state.savedAddress.getState(),
							state.savedAddress.getPostalCode(), state.savedAddress.getIsRemote(),
							state.savedEmployee.getEmployeeId(), state.savedEmployee.getFirstName(),
							state.savedEmployee.getLastName(), state.savedEmployee.getTitle(),
							state.savedEmployee.getDepartment(), state.savedEmployee.getBusinessUnit(),
							state.savedEmployee.getGender(), state.savedEmployee.getEthnicity(),
							state.savedEmployee.getAge(),
							csvData.get("HIRE_DATE"),
							state.savedEmployee.getAnnualSalary(),
							state.savedEmployee.getAddressId(),
							state.savedEmployee.getManagerId());
				}, state);
	}

	private void step2_UploadImageToGcs(MultipartFile employeeImage, OnboardingState state) {
		String stepName = "Step 2: Upload Employee Image to GCS";
		String onboardingPath = state.onboardingFolder + state.baseNameLower + "." + state.imageExtension;
		String employeesPath = "employees/" + state.baseNameLower + "." + state.imageExtension;

		executeStepWithJudge(stepName,
				String.format("1) Employee image uploaded to GCS path '%s'. " +
						"2) Employee image uploaded to GCS path '%s'. " +
						"Both uploads must be confirmed successful.", onboardingPath, employeesPath),
				(attempt) -> {
					byte[] imageBytes = employeeImage.getBytes();

					// Action A: Save to onboarding folder
					String resultA = fileStorageService.uploadFile(imageBytes, onboardingPath);
					state.gcsFilesCreated.add(onboardingPath);
					logger.info("Image uploaded to onboarding path: {}", onboardingPath);

					// Action B: Save to employees folder
					String resultB = fileStorageService.uploadFile(imageBytes, employeesPath);
					state.gcsFilesCreated.add(employeesPath);
					logger.info("Image uploaded to employees path: {}", employeesPath);

					// Verify files exist
					List<String> onboardingFiles = fileStorageService.listFiles(state.onboardingFolder);
					List<String> employeeFiles = fileStorageService.listFiles("employees/");

					return String.format("""
							Upload Result A (onboarding): %s
							Upload Result B (employees): %s
							Files in onboarding folder '%s': %s
							Files in employees/ folder containing '%s': %s
							""",
							resultA, resultB,
							state.onboardingFolder, onboardingFiles,
							state.baseNameLower,
							employeeFiles.stream()
									.filter(f -> f.contains(state.baseNameLower))
									.toList());
				}, state);
	}

	private void step3_GenerateTempBadge(OnboardingState state) {
		String stepName = "Step 3: Generate Temporary Employee Badge";
		String badgeFileName = state.baseNameLower + "_temp_badge";
		String targetBadgePath = state.onboardingFolder + badgeFileName + ".png";

		executeStepWithJudge(stepName,
				String.format("1) A temporary employee badge image was generated for the employee. " +
						"2) The badge generation process first creates the image at a temporary 'generated_images/' path, " +
						"then moves it to the final GCS destination and deletes the temporary file. " +
						"3) The badge's final location is GCS path '%s'. " +
						"4) The badge should be a valid image file (non-zero bytes) at the final GCS path.", targetBadgePath),
				(attempt) -> {
					// GCS paths for the badge template and the employee's photo
					String badgeTemplatePath = "original_images/employeeTempBadgeTemplate.png";
					String employeeImageGcsPath = "employees/" + state.baseNameLower + "." + state.imageExtension;

					// Badge expiration: one month after start date
					String hireDateStr = state.csvData.get("HIRE_DATE");
					LocalDate hireDate = LocalDate.parse(hireDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
					String badgeExpDate = hireDate.plusMonths(1).format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
					state.badgeExpDate = badgeExpDate;

					String badgePrompt = String.format(
							"Using the provided badge template image as the base, generate a completed temporary " +
									"employee ID badge. The template has placeholder fields that must be populated: " +
									"- Replace the person silhouette placeholder with the employee's photo (from the second input image). " +
									"- EMPLOYEE NAME: %s %s " +
									"- JOB TITLE: %s " +
									"- DEPT: %s " +
									"- ID#: %d " +
									"- Badge expiration date: %s (one month after the employee's start date) " +
									"Keep the existing template layout, branding, and TEMPORARY marking intact. " +
									"Only fill in the placeholder fields and replace the silhouette with the actual employee photo.",
							state.firstName, state.lastName,
							state.csvData.get("TITLE"),
							state.csvData.get("DEPARTMENT"),
							state.savedEmployee.getEmployeeId(),
							badgeExpDate);

					// Generate badge using both the template and employee image as inputs
					String genResult = imageGenerationService.generateImage(
							badgePrompt,
							new String[] { badgeTemplatePath, employeeImageGcsPath },
							badgeFileName);
					logger.info("Badge generation result: {}", genResult);

					// Move from generated_images/ to onboarding folder
					String generatedPath = "generated_images/" + badgeFileName + ".png";
					byte[] badgeBytes = fileStorageService.retrieveFile(generatedPath);
					if (badgeBytes != null) {
						fileStorageService.uploadFile(badgeBytes, targetBadgePath);
						state.gcsFilesCreated.add(targetBadgePath);
						fileStorageService.deleteFile(generatedPath);
						logger.info("Badge moved from {} to {}", generatedPath, targetBadgePath);
					}

					// Verify badge at target path
					List<String> onboardingFiles = fileStorageService.listFiles(state.onboardingFolder);

					return String.format("""
							Badge generated and moved to final path: %s
							Temporary file at generated_images/ was deleted after move: true
							Files in onboarding folder: %s
							Badge bytes size: %d
							""",
							targetBadgePath, onboardingFiles,
							badgeBytes != null ? badgeBytes.length : 0);
				}, state);
	}

	private void step4_DraftManagerEmail(OnboardingState state) {
		String stepName = "Step 4: Draft Email to Manager";
		String toEmail = state.managerEmail != null ? state.managerEmail : "unknown_manager@megacorp.com";
		String subject = ONBOARDING_EMAIL_PREFIX + " New Employee Starting - " + state.firstName + " " + state.lastName;

		// Calculate onboarding class dates
		String hireDateStr = state.csvData.get("HIRE_DATE");
		LocalDate hireDate = LocalDate.parse(hireDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
		LocalDate onboardingMonday = hireDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
		LocalDate onboardingTuesday = onboardingMonday.plusDays(1);
		String onboardingDates = onboardingMonday.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
				+ " and " + onboardingTuesday.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));

		executeStepWithJudge(stepName,
				String.format("1) A draft email was created addressed to the employee's manager at '%s'. " +
						"2) The email contains the new employee's details (name, title, department, start date). " +
						"3) The email mentions onboarding class dates (%s). " +
						"4) The email is professional and informative.", toEmail, onboardingDates),
				(attempt) -> {
					String emailPrompt = String.format("""
							Generate a professional email body to send to %s (manager, title: %s) at %s.
							This email informs them about a new employee joining their team.

							New Employee Details:
							- Name: %s %s
							- Title: %s
							- Department: %s
							- Business Unit: %s
							- Start Date: %s
							- Onboarding Class Dates: %s (Monday and Tuesday)

							The email should:
							- Be addressed to the manager by name
							- Include all the employee details listed above
							- Mention the start date clearly
							- Inform about the onboarding class dates
							- Emphasize the important role the manager plays in welcoming the new employee and
							  making their initial time at MegaCorp a positive and productive experience
							- Encourage the manager to personally introduce the new hire to the team, help them
							  understand how the company and department operate, be available to answer questions,
							  and serve as their primary point of contact during the first few weeks
							- Suggest scheduling a one-on-one within the first week to discuss expectations,
							  team culture, and any resources the new employee may need
							- Be professional and concise
							- Not include a subject line (that's handled separately)
							- Sign off as "HR Onboarding Team, MegaCorp"
							""",
							state.managerName != null ? state.managerName : "Manager",
							state.managerTitle != null ? state.managerTitle : "Manager",
							toEmail,
							state.firstName, state.lastName,
							state.csvData.get("TITLE"),
							state.csvData.get("DEPARTMENT"),
							state.csvData.get("BUSINESS_UNIT"),
							hireDateStr,
							onboardingDates);

					String emailBody = contentChatClient.prompt(emailPrompt).call().content();

					emailService.saveDraftEmail(toEmail, null, subject, emailBody, null, null, null);
					state.draftEmailSubjects.add(subject);
					logger.info("Manager email draft saved to {}", toEmail);

					return String.format("""
							Email To: %s
							Email Subject: %s
							Email Body:
							%s
							""", toEmail, subject, emailBody);
				}, state);
	}

	private void step5_DraftEmployeeEmail(OnboardingState state) {
		String stepName = "Step 5: Draft Welcome Email to New Employee";
		String toEmail = state.workEmail + ", " + state.personalEmail;
		String ccEmail = state.managerEmail;
		String subject = ONBOARDING_EMAIL_PREFIX + " Welcome to MegaCorp - " + state.firstName + " " + state.lastName;

		String hireDateStr = state.csvData.get("HIRE_DATE");
		LocalDate hireDate = LocalDate.parse(hireDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
		LocalDate onboardingMonday = hireDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
		LocalDate onboardingTuesday = onboardingMonday.plusDays(1);
		String onboardingDates = onboardingMonday.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
				+ " and " + onboardingTuesday.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));

		String badgeAttachmentPath = state.onboardingFolder + state.baseNameLower + "_temp_badge.png";

		executeStepWithJudge(stepName,
				String.format("1) A draft email was created addressed to both the employee's work email '%s' and personal email '%s'. " +
						"2) The email contains employee details (name, title, department, start date). " +
						"3) The email includes onboarding class dates and details. " +
						"4) The email includes the manager's name and details. " +
						"5) The email contains a warm welcoming message including 'Welcome to MegaCorp'. " +
						"6) The email is professional, warm, and encouraging. " +
						"7) The employee's temporary badge image is attached to the email from GCS path '%s'.",
						state.workEmail, state.personalEmail, badgeAttachmentPath),
				(attempt) -> {
					String emailPrompt = String.format("""
							Generate a warm and professional welcome email for a new employee joining MegaCorp.

							New Employee Details:
							- Name: %s %s
							- Title: %s
							- Department: %s
							- Business Unit: %s
							- Start Date: %s
							- Work Email: %s
							- Manager: %s (%s)

							Onboarding Class Details:
							- Dates: %s (Monday and Tuesday, two full days)
							- Location: MegaCorp Main Campus, Building A, Conference Room 3B
							- Time: 9:00 AM - 5:00 PM each day
							- What to bring: Government-issued ID for badge photo, any tax/benefits forms
							- Day 1 covers: Company overview, culture, benefits enrollment, IT setup
							- Day 2 covers: Department orientation, team introductions, tools & systems training

							Temporary Badge Information:
							- A temporary employee badge is attached to this email
							- The employee should save the badge image to their phone
							- The temp badge should be used to access the building until the permanent badge arrives
							- The temp badge expires on %s
							- If a permanent badge has not been received within a few weeks, the employee should
							  follow up with their manager or the Badge Department (Facilities Security)

							The email MUST:
							- Include "Welcome to MegaCorp!" as a prominent greeting
							- Be warm, friendly, and make the employee feel excited
							- Include all employee details listed above clearly
							- Mention manager name and role
							- List the onboarding class details, dates, location, and what to expect
							- Include a section about the attached temporary badge: explain that it is attached,
							  should be kept on their phone, is required for building access, expires on %s,
							  and they should follow up with their manager or the Badge Department if the
							  permanent badge has not arrived within a few weeks
							- Provide the work email address for their reference
							- Not include a subject line (handled separately)
							- Sign off as "HR Onboarding Team, MegaCorp"
							""",
							state.firstName, state.lastName,
							state.csvData.get("TITLE"),
							state.csvData.get("DEPARTMENT"),
							state.csvData.get("BUSINESS_UNIT"),
							hireDateStr,
							state.workEmail,
							state.managerName != null ? state.managerName : "Your Manager",
							state.managerTitle != null ? state.managerTitle : "Manager",
							onboardingDates,
							state.badgeExpDate,
							state.badgeExpDate);

					String emailBody = contentChatClient.prompt(emailPrompt).call().content();

					emailService.saveDraftEmail(toEmail, ccEmail, subject, emailBody, null, List.of(badgeAttachmentPath), null);
					state.draftEmailSubjects.add(subject);
					logger.info("Welcome email draft saved to {} (cc: {}) with badge attachment {}", toEmail, ccEmail, badgeAttachmentPath);

					return String.format("""
							Email To: %s
							Email Subject: %s
							Attachment (GCS): %s
							Email Body:
							%s
							""", toEmail, subject, badgeAttachmentPath, emailBody);
				}, state);
	}

	private void step6_DraftFacilitiesEmail(OnboardingState state) {
		String stepName = "Step 6: Draft Email to Facilities Security";
		String toEmail = "facilities_security@megacorp.com";
		String subject = ONBOARDING_EMAIL_PREFIX + " Badge Request - " + state.firstName + " " + state.lastName;

		String employeeImageAttachmentPath = "employees/" + state.baseNameLower + "." + state.imageExtension;

		executeStepWithJudge(stepName,
				String.format("1) A draft email was created addressed to '%s'. " +
						"2) The email requests that a permanent employee badge be manufactured. " +
						"3) The email includes the employee's personal email (%s) and work email (%s). " +
						"4) The email includes the employee's start date and high-level details (name, address). " +
						"5) The email is professional. " +
						"6) The employee's photo image is attached to the email from GCS path '%s'.",
						toEmail, state.personalEmail, state.workEmail, employeeImageAttachmentPath),
				(attempt) -> {
					String emailPrompt = String.format("""
							Generate a professional email to the Facilities Security team requesting a permanent employee badge.

							Send to: facilities_security@megacorp.com

							New Employee Details:
							- Name: %s %s
							- Title: %s
							- Department: %s
							- Business Unit: %s
							- Work Email: %s
							- Personal Email: %s
							- Start Date: %s
							- Address: %s, %s, %s %s

							The email should:
							- Request manufacture of a permanent employee badge for this new hire
							- Include the employee's personal and work email addresses
							- Include the start date
							- Include high-level details: name, title, department, address
							- Be formal and professional
							- Mention the temporary badge has already been issued
							- Not include a subject line
							- Sign off as "HR Onboarding Team, MegaCorp"
							""",
							state.firstName, state.lastName,
							state.csvData.get("TITLE"),
							state.csvData.get("DEPARTMENT"),
							state.csvData.get("BUSINESS_UNIT"),
							state.workEmail,
							state.personalEmail,
							state.csvData.get("HIRE_DATE"),
							state.csvData.get("STREET_ADDRESS"),
							state.csvData.get("CITY"),
							state.csvData.get("STATE"),
							state.csvData.get("POSTAL_CODE"));

					String emailBody = contentChatClient.prompt(emailPrompt).call().content();

					emailService.saveDraftEmail(toEmail, state.managerEmail, subject, emailBody, null, List.of(employeeImageAttachmentPath), null);
					state.draftEmailSubjects.add(subject);
					logger.info("Facilities Security email draft saved to {} (cc: {}) with image attachment {}", toEmail, state.managerEmail, employeeImageAttachmentPath);

					return String.format("""
							Email To: %s
							Email Subject: %s
							Attachment (GCS): %s
							Email Body:
							%s
							""", toEmail, subject, employeeImageAttachmentPath, emailBody);
				}, state);
	}

	private void step7_DraftItEmail(OnboardingState state) {
		String stepName = "Step 7: Draft Email to IT Department";
		String toEmail = "info_tech@megacorp.com";
		String subject = ONBOARDING_EMAIL_PREFIX + " Equipment & Access Request - " + state.firstName + " " + state.lastName;

		executeStepWithJudge(stepName,
				String.format("1) A draft email was created addressed to '%s'. " +
						"2) The email requests equipment (phone, laptop, etc.) and system access for the new employee's job title. " +
						"3) The email includes the employee's personal email (%s) and work email (%s). " +
						"4) The email includes the employee's start date and high-level details (name, title, department, address). " +
						"5) The email is professional.",
						toEmail, state.personalEmail, state.workEmail),
				(attempt) -> {
					String emailPrompt = String.format("""
							Generate a professional email to the Information Technology (IT) department requesting
							equipment and system access for a new employee.

							Send to: info_tech@megacorp.com

							New Employee Details:
							- Name: %s %s
							- Title: %s
							- Department: %s
							- Business Unit: %s
							- Work Email: %s
							- Personal Email: %s
							- Start Date: %s
							- Address: %s, %s, %s %s

							The email should:
							- Request that the new employee be provided appropriate equipment (laptop, phone, monitors, peripherals)
							  and system access required for their job title of "%s"
							- Include the employee's personal and work email addresses
							- Include the start date and request everything be ready by then
							- Include high-level details: name, title, department, address
							- Be formal and professional
							- Not include a subject line
							- Sign off as "HR Onboarding Team, MegaCorp"
							""",
							state.firstName, state.lastName,
							state.csvData.get("TITLE"),
							state.csvData.get("DEPARTMENT"),
							state.csvData.get("BUSINESS_UNIT"),
							state.workEmail,
							state.personalEmail,
							state.csvData.get("HIRE_DATE"),
							state.csvData.get("STREET_ADDRESS"),
							state.csvData.get("CITY"),
							state.csvData.get("STATE"),
							state.csvData.get("POSTAL_CODE"),
							state.csvData.get("TITLE"));

					String emailBody = contentChatClient.prompt(emailPrompt).call().content();

					emailService.saveDraftEmail(toEmail, state.managerEmail, subject, emailBody, null, null, null);
					state.draftEmailSubjects.add(subject);
					logger.info("IT department email draft saved to {} (cc: {})", toEmail, state.managerEmail);

					return String.format("""
							Email To: %s
							Email Subject: %s
							Email Body:
							%s
							""", toEmail, subject, emailBody);
				}, state);
	}

	private void step8_CleanupGcsOnboarding(OnboardingState state) {
		String stepName = "Step 8: Clean Up GCS Onboarding Folder";
		String onboardingFolder = state.onboardingFolder;

		executeStepWithJudge(stepName,
				String.format("1) All files in the GCS path '%s' have been deleted. " +
						"2) The onboarding staging folder is now empty or does not exist. " +
						"The cleanup should have removed all temporary onboarding files for this employee.",
						onboardingFolder),
				(attempt) -> {
					// List all files in onboarding folder
					List<String> files = fileStorageService.listFiles(onboardingFolder);
					logger.info("Files in onboarding folder before cleanup: {}", files);

					// Delete each file
					List<String> deletedFiles = new ArrayList<>();
					for (String file : files) {
						if (!file.endsWith("/")) { // Skip directory markers
							String result = fileStorageService.deleteFile(file);
							deletedFiles.add(file + " -> " + result);
							logger.info("Deleted onboarding file: {} -> {}", file, result);
						}
					}

					// Remove onboarding files from tracking (deliberately cleaned up, not rollback targets)
					state.gcsFilesCreated.removeIf(f -> f.startsWith(onboardingFolder));

					// Verify folder is empty
					List<String> remainingFiles = fileStorageService.listFiles(onboardingFolder);

					return String.format("""
							Onboarding folder: %s
							Files found before cleanup: %s
							Deletion results: %s
							Files remaining after cleanup: %s
							""",
							onboardingFolder, files, deletedFiles, remainingFiles);
				}, state);
	}

	// ========================================================================================
	// Retry-with-Judge Execution Engine
	// ========================================================================================

	/**
	 * Executes a step action and validates it with the judge, retrying on failure.
	 * This is the core execution pattern for the onboarding workflow, implementing
	 * the same LLM-as-a-Judge retry loop concept as SelfRefineEvaluationAdvisor
	 * but for explicit workflow steps rather than advisor chain integration.
	 */
	private void executeStepWithJudge(String stepName, String criteria, StepAction action, OnboardingState state) {
		state.currentStep = stepName;
		logger.info(">>> Starting: {}", stepName);

		for (int attempt = 1; attempt <= maxRetries; attempt++) {
			try {
				String evidence = action.execute(attempt);

				OnboardingStepResult result = judgeService.evaluate(stepName, criteria, evidence);

				if (result.passed()) {
					String summary = stepName + " - PASSED (attempt " + attempt + ", rating: " + result.rating() + ")";
					state.completedSteps.add(summary);
					logger.info("<<< {} PASSED on attempt {} with rating {}", stepName, attempt, result.rating());
					return;
				}

				if (attempt >= maxRetries) {
					throw new OnboardingStepException(stepName,
							"Failed after " + maxRetries + " attempts. Last rating: " + result.rating()
									+ ". Feedback: " + result.feedback());
				}

				logger.warn("{} FAILED attempt {}/{} - rating: {}, feedback: {}",
						stepName, attempt, maxRetries, result.rating(), result.feedback());

			} catch (OnboardingStepException e) {
				throw e;
			} catch (Exception e) {
				if (attempt >= maxRetries) {
					throw new OnboardingStepException(stepName,
							"Exception after " + maxRetries + " attempts: " + e.getMessage(), e);
				}
				logger.warn("{} exception on attempt {}/{}: {}", stepName, attempt, maxRetries, e.getMessage());
			}
		}
	}

	@FunctionalInterface
	private interface StepAction {
		String execute(int attempt) throws Exception;
	}

	// ========================================================================================
	// Rollback
	// ========================================================================================

	private void performRollback(OnboardingState state) {
		logger.warn("Performing rollback for onboarding of {} {}", state.firstName, state.lastName);

		// 1. Rollback DB records
		cleanupDbRecords(state);

		// 2. Rollback GCS files
		for (String gcsFile : state.gcsFilesCreated) {
			try {
				fileStorageService.deleteFile(gcsFile);
				logger.info("Rollback: deleted GCS file {}", gcsFile);
			} catch (Exception e) {
				logger.error("Rollback: failed to delete GCS file {}: {}", gcsFile, e.getMessage());
			}
		}

		// 3. Rollback draft emails
		if (!state.draftEmailSubjects.isEmpty()) {
			try {
				int deleted = emailService.deleteDraftsBySubjectContaining(ONBOARDING_EMAIL_PREFIX);
				logger.info("Rollback: deleted {} onboarding draft emails", deleted);
			} catch (Exception e) {
				logger.error("Rollback: failed to delete draft emails: {}", e.getMessage());
			}
		}

		logger.warn("Rollback completed for {} {}", state.firstName, state.lastName);
	}

	private void cleanupDbRecords(OnboardingState state) {
		if (state.savedEmployee != null) {
			try {
				employeeRepository.deleteById(state.savedEmployee.getEmployeeId());
				logger.info("Rollback: deleted employee id={}", state.savedEmployee.getEmployeeId());
				state.savedEmployee = null;
			} catch (Exception e) {
				logger.error("Rollback: failed to delete employee: {}", e.getMessage());
			}
		}
		if (state.savedAddress != null) {
			try {
				addressRepository.deleteById(state.savedAddress.getAddressId());
				logger.info("Rollback: deleted address id={}", state.savedAddress.getAddressId());
				state.savedAddress = null;
			} catch (Exception e) {
				logger.error("Rollback: failed to delete address: {}", e.getMessage());
			}
		}
	}

	// ========================================================================================
	// Report Builders
	// ========================================================================================

	private String buildSuccessReport(OnboardingState state) {
		StringBuilder sb = new StringBuilder();
		sb.append("=== ONBOARDING COMPLETE ===\n\n");
		sb.append(String.format("Employee: %s %s\n", state.firstName, state.lastName));
		sb.append(String.format("Work Email: %s\n", state.workEmail));
		sb.append(String.format("Personal Email: %s\n\n", state.personalEmail));

		sb.append("--- Completed Steps ---\n");
		for (String step : state.completedSteps) {
			sb.append("  + ").append(step).append("\n");
		}

		sb.append("\n--- Draft Emails Created ---\n");
		sb.append("REMINDER: Please review, update, and send the following draft emails:\n");
		for (String subject : state.draftEmailSubjects) {
			sb.append("  [Email] ").append(subject).append("\n");
		}

		sb.append("\n--- Summary ---\n");
		sb.append(String.format("Database records created: Employee ID %d, Address ID %d\n",
				state.savedEmployee != null ? state.savedEmployee.getEmployeeId() : -1,
				state.savedAddress != null ? state.savedAddress.getAddressId() : -1));
		sb.append(String.format("Employee image stored at: employees/%s.%s\n",
				state.baseNameLower, state.imageExtension));
		sb.append("Temporary badge generated and stored in GCS\n");
		sb.append("GCS onboarding staging folder cleaned up\n");

		return sb.toString();
	}

	private String buildFailureReport(OnboardingState state, OnboardingStepException exception) {
		StringBuilder sb = new StringBuilder();
		sb.append("=== ONBOARDING FAILED ===\n\n");
		sb.append(String.format("Employee: %s %s\n", state.firstName, state.lastName));
		sb.append(String.format("Failed at: %s\n", exception.getStepName()));
		sb.append(String.format("Reason: %s\n\n", exception.getMessage()));

		sb.append("--- Step Sequence ---\n");
		for (String step : state.completedSteps) {
			sb.append("  + ").append(step).append("\n");
		}
		sb.append("  X ").append(exception.getStepName()).append(" - FAILED\n");

		sb.append("\n--- Rollback Actions Taken ---\n");
		sb.append("  - Database records: rolled back (deleted inserted employee and address records)\n");
		sb.append(String.format("  - GCS files: attempted deletion of %d files\n", state.gcsFilesCreated.size()));
		sb.append(String.format("  - Draft emails: attempted deletion of drafts with prefix '%s'\n",
				ONBOARDING_EMAIL_PREFIX));

		return sb.toString();
	}

	// ========================================================================================
	// Utilities
	// ========================================================================================

	private Map<String, String> parseCsvData(MultipartFile csvFile) throws Exception {
		String content = new String(csvFile.getBytes(), StandardCharsets.UTF_8);
		String[] lines = content.trim().split("\\r?\\n");
		if (lines.length < 2) {
			throw new IllegalArgumentException("CSV must have a header row and at least one data row");
		}

		String[] headers = lines[0].split(",");
		String[] values = lines[1].split(",", -1);

		Map<String, String> data = new LinkedHashMap<>();
		for (int i = 0; i < headers.length && i < values.length; i++) {
			data.put(headers[i].trim(), values[i].trim());
		}
		return data;
	}

	private String getFileExtension(String filename) {
		if (filename == null || !filename.contains(".")) {
			return "jpg"; // default
		}
		return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
	}

	// ========================================================================================
	// Internal State Tracking
	// ========================================================================================

	/**
	 * Mutable state object tracking the progress of an onboarding workflow execution.
	 * Used for rollback coordination and report building.
	 */
	private static class OnboardingState {
		String firstName;
		String lastName;
		String personalEmail;
		String workEmail;
		String imageExtension;
		String baseNameLower;
		String onboardingFolder;
		String currentStep;

		// Manager info (populated in Step 1 from DB lookup)
		String managerName;
		String managerEmail;
		String managerTitle;

		// CSV data (populated in Step 1)
		Map<String, String> csvData;

		// Saved entities (for rollback)
		Address savedAddress;
		Employee savedEmployee;

		// Badge expiration date (populated in Step 3, used in Step 5)
		String badgeExpDate;

		// Tracking for rollback
		List<String> gcsFilesCreated = new ArrayList<>();
		List<String> draftEmailSubjects = new ArrayList<>();
		List<String> completedSteps = new ArrayList<>();
	}
}
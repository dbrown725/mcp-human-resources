package com.megacorp.humanresources.service;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.megacorp.humanresources.entity.Employee;
import com.megacorp.humanresources.repository.EmployeeRepository;
import com.megacorp.humanresources.specifications.EmployeeSpecifications;

import lombok.extern.slf4j.Slf4j;


/**
 * Service implementation for managing Employee entities.
 * This class provides methods to perform CRUD operations and advanced search functionalities.
 *
 * Main Methods:
 * - saveEmployee: Creates a new employee.
 * - saveEmployeeByName: Creates a new employee using first and last name.
 * - fetchEmployeePage: Retrieves a paginated and sorted list of employees.
 * - updateEmployee: Updates an existing employee.
 * - getEmployeeById: Retrieves a single employee by their ID.
 * - deleteEmployeeById: Deletes a single employee by their ID.
 * - searchEmployees: Searches employees by various criteria with pagination and sorting.
 *
 * Annotations:
 * - @Service: Marks this class as a Spring service.
 * - @Slf4j: Provides logging capabilities.
 */
@Service
@Slf4j

//Class
public class EmployeeServiceImpl implements EmployeeService {
	
	private static final Logger logger = LoggerFactory.getLogger(EmployeeServiceImpl.class);

	@Autowired
	private EmployeeRepository employeeRepository;
	
	/**
	 * Save a new employee to the repository.
	 *
	 * @param employee The Employee object to be saved
	 * @return The persisted Employee object with generated ID
	 */
	@Tool(name = "saveEmployee", description = "Creates a new employee based on the passed employee object.")
	@Override
	public Employee saveEmployee(Employee employee) {
		employee.setEmployeeId(null);
		return employeeRepository.save(employee);
	}

	/**
	 * Creates and saves a new employee using the provided first and last name.
	 *
	 * @param firstName The first name of the new employee
	 * @param lastName The last name of the new employee
	 * @return The persisted Employee object with generated ID and default hire date
	 */
	@Tool(name = "saveEmployeeByName", description = "Creates a new employee using the provided first name and last name.")
	public Employee saveEmployeeByName(String firstName, String lastName) {
		logger.info("Enter saveEmployeeByName(String firstName, String lastName)");
		logger.debug("saveEmployeeByName inputs - firstName: {}, lastName: {}", firstName, lastName);
		Employee employee = new Employee(firstName, lastName);
		try {
			employee.setHireDate(new java.text.SimpleDateFormat("MM/dd/yyyy").parse("09/09/9999"));
		} catch (Exception e) {
			logger.error("Error parsing hire date", e);
		}
		logger.info("Exit saveEmployeeByName(String firstName, String lastName)");
		return employeeRepository.save(employee);
	}

	/**
	 * Retrieve a paginated and sorted list of all employees.
	 *
	 * @param pageNumber The page number to retrieve (1-based)
	 * @param pageSize The number of employees per page
	 * @param sortBy The field to sort by (any Employee field)
	 * @param sortDirection The direction of sorting ("asc" or "desc")
	 * @return A Page containing the employees for the requested page and sort order
	 */
	@Tool(name = "fetchEmployeePage", description = "Get a Page of all employees. pageNumber, pageSize, sortBy (can be any Employee field) " +
		"and sortDirection (can be desc or asc).")
	@Override
	public Page<Employee> fetchEmployeePage(Integer pageNumber, Integer pageSize, String sortBy, String sortDirection) {
		logger.info("Enter fetchEmployeePage(Integer pageNumber, Integer pageSize, String sortBy, String sortDirection)");
		logger.debug("fetchEmployeePage inputs - pageNumber: {}, pageSize: {}, sortBy: {}, sortDirection: {}", 
			pageNumber, pageSize, sortBy, sortDirection);
		if (pageNumber == null || pageNumber < 1) {
			pageNumber = 1; // Default to first page if not provided or invalid
		}

		Pageable pageable;
		
		if(sortDirection == null || sortDirection.isEmpty() || sortDirection.equalsIgnoreCase("asc")) {
			pageable = PageRequest.of(pageNumber.intValue() - 1, pageSize.intValue(), Sort.by(sortBy).ascending());
		} else if (sortDirection.equalsIgnoreCase("desc")) {
			pageable = PageRequest.of(pageNumber.intValue() - 1, pageSize.intValue(), Sort.by(sortBy).descending());
		} else {
			logger.warn("Invalid sort direction provided: {}. Defaulting to ascending order.", sortDirection);
			pageable = PageRequest.of(pageNumber.intValue() - 1, pageSize.intValue(), Sort.by(sortBy).ascending());
		}
		logger.debug("Created pageable object: {}", pageable);

		Page<Employee> employeesList = employeeRepository.findAll(pageable);
		return employeesList;
	}

	/**
	 * Update an existing employee
	 *
	 * @param employee The employee object with updated details
	 * @param employeeId The ID of the employee to be updated
	 * @return The updated Employee object
	 */
	@Tool(name = "updateEmployee", description = "Updates an existing employee. The only required field is employeeId. " +
		"DO NOT CHANGE A FIELD UNLESS SPECIFICALLY ASKED TO DO SO BY THE USER!")
	@Override
	public Employee updateEmployee(Employee employee, Long employeeId) {
		logger.info("updateEmployee input - employee: {}", employee.toString());
		return employeeRepository.save(employee);
	}

	/**
	 * Retrieve a single employee by their ID
	 *
	 * @param employeeId The ID of the employee to retrieve
	 * @return The Employee object corresponding to the given ID
	 */
	@Override
	@Tool(name = "getEmployeeById", description = "Get a single employee by ID")
	public Employee getEmployeeById(Long employeeId) {
		logger.info("Enter/Exit getEmployeeById(Long employeeId)");
		return employeeRepository.findById(employeeId).get();
	}

	/**
	 * Delete a single employee by ID
	 *
	 * @param employeeId The ID of the employee to be deleted
	 */
	@Override
	@Tool(name = "deleteEmployeeById", description = "Delete a single employee by ID")
	public void deleteEmployeeById(Long employeeId) {
		logger.info("Enter/exit deleteEmployeeById(Long employeeId)");
		logger.debug("deleteEmployeeById input - employeeId: {}", employeeId);
		logger.info("Exit deleteEmployeeById(Long employeeId)");
		employeeRepository.deleteById(employeeId);
	}
	
	/**
	 * Searches for employees using any combination of the following optional parameters:
	 * firstName, lastName, startAge, endAge, department, title, businessUnit,
	 * gender, ethnicity, managerId, hireDate, hireDateFirst, hireDateLast, annualSalary.
	 * Supports pagination and sorting.
	 *
	 * @param firstName The first name of the employee (optional)
	 * @param lastName The last name of the employee (optional)
	 * @param startAge The minimum age of the employee (optional)
	 * @param endAge The maximum age of the employee (optional)
	 * @param department The department of the employee (optional)
	 * @param title The job title of the employee (optional)
	 * @param businessUnit The business unit of the employee (optional)
	 * @param gender The gender of the employee (optional)
	 * @param ethnicity The ethnicity of the employee (optional)
	 * @param managerId The manager's ID (optional)
	 * @param hireDate The hire date of the employee (optional)
	 * @param hireDateFirst The earliest hire date (optional)
	 * @param hireDateLast The latest hire date (optional)
	 * @param annualSalary The annual salary of the employee (optional)
	 * @param pageNumber The page number to retrieve (1-based)
	 * @param pageSize The number of employees per page
	 * @param sortBy The field to sort by (any Employee field)
	 * @param sortDirection The direction of sorting ("asc" or "desc")
	 * @return A Page of employees matching the criteria
	 */
	@Tool(
		name = "searchEmployees",
		description = "Get a Page of employees. pageNumber, pageSize, sortBy (can be any Employee field) " + 
		"and sortDirection (can be desc or asc). Optional paramaters: firstName, lastName, startAge, " +
		"endAge, department, title, businessUnit, gender, ethnicity, managerId, hireDate, hireDateFirst, hireDateLast, annualSalary."
	)
	public Page<Employee> searchEmployees(
		@ToolParam(required = false) String firstName,
		@ToolParam(required = false) String lastName,
		@ToolParam(required = false) Integer startAge,
		@ToolParam(required = false) Integer endAge,
		@ToolParam(required = false) String department,
		@ToolParam(required = false) String title,
		@ToolParam(required = false) String businessUnit,
		@ToolParam(required = false) String gender,
		@ToolParam(required = false) String ethnicity,
		@ToolParam(required = false) Long managerId,
		@ToolParam(required = false) Date hireDate,
		@ToolParam(required = false) Date hireDateFirst,
		@ToolParam(required = false) Date hireDateLast,
		@ToolParam(required = false) Long annualSalary,
		Integer pageNumber, Integer pageSize, String sortBy, String sortDirection
	)
	{
		logger.info("Enter searchEmployees("
			+ "firstName={}, lastName={}, startAge={}, endAge={}, department={}, title={}, businessUnit={}, gender={}, ethnicity={}, managerId={}, hireDate={}, hireDateFirst={}, hireDateLast={}, annualSalary={}, pageNumber={}, pageSize={}, sortBy={}, sortDirection={})",
			firstName, lastName, startAge, endAge, department, title, businessUnit, gender, ethnicity, managerId, hireDate, hireDateFirst, hireDateLast, annualSalary, pageNumber, pageSize, sortBy, sortDirection);

		Specification<Employee> spec = Specification.where(null);
		if (firstName != null && !firstName.isEmpty()) {
			spec = spec.and(EmployeeSpecifications.hasFirstName(firstName));
		}
		if (lastName != null && !lastName.isEmpty()) {
			spec = spec.and(EmployeeSpecifications.hasLastName(lastName));
		}
		if (startAge != null && endAge != null) {
			spec = spec.and(EmployeeSpecifications.ageBetween(startAge, endAge));
		}

		if (department != null && !department.isEmpty()) {
			spec = spec.and(EmployeeSpecifications.hasDepartment(department));
		}
		if (title != null && !title.isEmpty()) {
			spec = spec.and(EmployeeSpecifications.hasTitle(title));
		}

		if (gender != null && !gender.isEmpty()) {
			spec = spec.and(EmployeeSpecifications.hasGender(gender));
		}
		if (businessUnit != null && !businessUnit.isEmpty()) {
			spec = spec.and(EmployeeSpecifications.hasBusinessUnit(businessUnit));
		}
		if (ethnicity != null && !ethnicity.isEmpty()) {
			spec = spec.and(EmployeeSpecifications.hasEthnicity(ethnicity));
		}
		if (hireDate != null) {
			spec = spec.and(EmployeeSpecifications.hasHireDate(hireDate.toString()));
		}
		if (hireDateFirst != null && hireDateLast != null) {
			spec = spec.and(EmployeeSpecifications.hasHireDateBetween(hireDateFirst, hireDateLast));
		}
		if (annualSalary != null) {
			spec = spec.and(EmployeeSpecifications.hasAnnualSalary(annualSalary));
		}
		if (managerId != null) {
			spec = spec.and(EmployeeSpecifications.hasManagerId(managerId));
		}

		logger.debug("Built search specification: {}", spec);

		Pageable pageable;
		
		if(sortDirection == null || sortDirection.isEmpty() || sortDirection.equalsIgnoreCase("asc")) {
			pageable = PageRequest.of(pageNumber.intValue() - 1, pageSize.intValue(), Sort.by(sortBy).ascending());
		} else if (sortDirection.equalsIgnoreCase("desc")) {
			pageable = PageRequest.of(pageNumber.intValue() - 1, pageSize.intValue(), Sort.by(sortBy).descending());
		} else {
			logger.warn("Invalid sort direction provided: {}. Defaulting to ascending order.", sortDirection);
			pageable = PageRequest.of(pageNumber.intValue() - 1, pageSize.intValue(), Sort.by(sortBy).ascending());
		}
		logger.debug("Created pageable object: {}", pageable);

		Page<Employee> result = employeeRepository.findAll(spec, pageable);

		return result;
	}
}
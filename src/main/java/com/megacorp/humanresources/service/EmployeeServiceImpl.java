package com.megacorp.humanresources.service;

import java.util.Date;
import java.util.List;

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
	@Tool(name = "save_employee", description = "Creates a new employee based on the passed employee object.")
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
	@Tool(name = "save_employee_with_name", description = "Creates a new employee using the provided first name and last name.")
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
	 * Update an existing employee
	 *
	 * @param employee The employee object with updated details
	 * @param employeeId The ID of the employee to be updated
	 * @return The updated Employee object
	 */
	// @Tool(name = "updateEmployee", description = "Updates an existing employee. The only required field is employeeId. " +
	// 	"DO NOT CHANGE A FIELD UNLESS SPECIFICALLY ASKED TO DO SO BY THE USER!")
	// @Override
	// public Employee updateEmployee(Employee employee, Long employeeId) {
	// 	logger.info("updateEmployee input - employee: {}", employee.toString());
	// 	return employeeRepository.save(employee);
	// }

	/**
	 * Updates specific fields of an existing employee.
	 * Only the fields provided as non-null parameters will be updated.
	 *
	 * @param employeeId The ID of the employee to update (required)
	 * @param firstName The new first name of the employee (optional)
	 * @param lastName The new last name of the employee (optional)
	 * @param age The new age of the employee (optional)
	 * @param department The new department of the employee (optional)
	 * @param title The new job title of the employee (optional)
	 * @param businessUnit The new business unit of the employee (optional)
	 * @param gender The new gender of the employee (optional)
	 * @param ethnicity The new ethnicity of the employee (optional)
	 * @param managerId The new manager's ID (optional)
	 * @param hireDate The new hire date of the employee (optional)
	 * @param annualSalary The new annual salary of the employee (optional)
	 * @return The updated Employee object
	 */
	@Tool(
		name = "update_employee",
		description = "Update specific fields of an Employee. Optional parameters: firstName, lastName, age, department, " +
			"title, businessUnit, gender, ethnicity, managerId, hireDate, annualSalary. " +
			"Only non-null parameters will be updated. Requires employeeId."
	)
	public Employee updateEmployee(
		Long employeeId,
		@ToolParam(required = false) String firstName,
		@ToolParam(required = false) String lastName,
		@ToolParam(required = false) Long age,
		@ToolParam(required = false) String department,
		@ToolParam(required = false) String title,
		@ToolParam(required = false) String businessUnit,
		@ToolParam(required = false) String gender,
		@ToolParam(required = false) String ethnicity,
		@ToolParam(required = false) Long managerId,
		@ToolParam(required = false) Date hireDate,
		@ToolParam(required = false) Long annualSalary
	)
	{
		logger.info("Enter updateEmployee("
			+ "employeeId={}, firstName={}, lastName={}, age={}, department={}, title={}, businessUnit={}, gender={}, ethnicity={},\n"
			+ "managerId={}, hireDate={}, annualSalary={})",
			employeeId, firstName, lastName, age, department, title, businessUnit, gender, ethnicity, managerId, hireDate, annualSalary);

		Employee employee = employeeRepository.findById(employeeId).get();

		if (firstName != null && !firstName.isEmpty()) {
			employee.setFirstName(firstName);
		}
		if (lastName != null && !lastName.isEmpty()) {
			employee.setLastName(lastName);
		}
		if (age != null) {
			employee.setAge(age);
		}
		if (department != null && !department.isEmpty()) {
			employee.setDepartment(department);
		}
		if (title != null && !title.isEmpty()) {
			employee.setTitle(title);
		}
		if (gender != null && !gender.isEmpty()) {
			employee.setGender(gender);
		}
		if (businessUnit != null && !businessUnit.isEmpty()) {
			employee.setBusinessUnit(businessUnit);
		}
		if (ethnicity != null && !ethnicity.isEmpty()) {
			employee.setEthnicity(ethnicity);
		}
		if (hireDate != null) {
			employee.setHireDate(hireDate);
		}
		if (annualSalary != null) {
			employee.setAnnualSalary(annualSalary);
		}
		if (managerId != null) {
			employee.setManagerId(managerId);
		}
		employeeRepository.save(employee);

		logger.info("Exit updateEmployee with updated employee: {}", employee.toString());
		return employee;
	}


	/**
	 * Retrieve a single employee by their ID
	 *
	 * @param employeeId The ID of the employee to retrieve
	 * @return The Employee object corresponding to the given ID
	 */
	@Override
	@Tool(name = "get_employee_with_id", description = "Get a single employee by ID")
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
	@Tool(name = "delete_employee_with_id", description = "Delete a single employee by ID")
	public void deleteEmployeeById(Long employeeId) {
		logger.info("Enter/exit deleteEmployeeById(Long employeeId)");
		logger.debug("deleteEmployeeById input - employeeId: {}", employeeId);
		logger.info("Exit deleteEmployeeById(Long employeeId)");
		employeeRepository.deleteById(employeeId);
	}

	/**
	 * Retrieve a list of all employees
	 * 
	 * @return A list containing all employees
	 */
	@Tool(name = "fetch_employee_list", description = "Get a list of all employees")
	public List<Employee> fetchEmployeeList() {
		logger.info("Enter fetchEmployeeList()");
		List<Employee> employeesList = (List<Employee>) employeeRepository.findAll();
		logger.info("Exit fetchEmployeeList()");
		return employeesList;
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
		name = "search_employees",
		description = "Get a Page of employees. Optional parameters: pageNumber, pageSize, sortBy (any Employee field), " +
		"sortDirection (desc or asc), firstName, lastName, startAge, endAge, department, title, businessUnit, gender, ethnicity, " +
		"managerId, hireDate, hireDateFirst, hireDateLast, annualSalary."
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
		@ToolParam(required = false) Integer pageNumber, 
		@ToolParam(required = false) Integer pageSize, 
		@ToolParam(required = false) String sortBy, 
		@ToolParam(required = false) String sortDirection
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

		Pageable pageable = null;

		if (pageNumber == null || pageNumber < 1) {
			pageNumber = 1; // Default to first page if not provided or invalid
		}
		if (pageSize == null || pageSize < 1) {
			pageSize = 10; // Default to 10 items per page if not provided or invalid
		}
		if (sortBy == null || sortBy.isEmpty()) {
			sortBy = "employeeId"; // Default sort by employeeId if not provided
		}
		if (sortDirection == null || sortDirection.isEmpty()) {
			sortDirection = "asc"; // Default to ascending order if not provided
		}
		logger.debug("Pagination and sorting parameters - pageNumber: {}, pageSize: {}, sortBy: {}, sortDirection: {}", 
			pageNumber, pageSize, sortBy, sortDirection);

		// pageNumber - 1 is used because PageRequest is 0-based
		if (sortDirection.equalsIgnoreCase("desc")) {
			pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.by(sortBy).descending());
		} else if (!sortDirection.equalsIgnoreCase("asc")) {
			logger.warn("Invalid sort direction provided: {}. Defaulting to ascending order.", sortDirection);
			pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.by(sortBy).ascending());
		} else {
			pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.by(sortBy).ascending());
		}
		logger.debug("Created pageable object: {}", pageable);

		Page<Employee> result = employeeRepository.findAll(spec, pageable);

		return result;
	}
}
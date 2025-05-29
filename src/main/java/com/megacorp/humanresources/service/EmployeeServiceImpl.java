package com.megacorp.humanresources.service;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.megacorp.humanresources.entity.Employee;
import com.megacorp.humanresources.repository.EmployeeRepository;
import com.megacorp.humanresources.specifications.EmployeeSpecifications;

import lombok.extern.slf4j.Slf4j;

//Annotation
/**
 * Service implementation for managing Employee entities.
 * This class provides methods to perform CRUD operations and additional functionalities
 * such as searching employees by specific criteria.
 * 
 * Methods:
 * - saveEmployee: Creates a new employee.
 * - fetchEmployeeList: Retrieves a list of all employees.
 * - updateEmployee: Updates an existing employee.
 * - getEmployeeById: Retrieves a single employee by their ID.
 * - deleteEmployeeById: Deletes a single employee by their ID.
 * - searchUsers: Searches employees by first name and/or age range.
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
	 * Create a new employee
	 *
	 * @param employee The employee object to be created
	 * @return The created Employee object
	 */
	@Tool(name = "saveEmployee", description = "Creates a new employee based on the passed employee object.")
	@Override
	public Employee saveEmployee(Employee employee) {
		employee.setEmployeeId(null);
		return employeeRepository.save(employee);
	}

	/**
	 * Save a new employee using first name and last name
	 *
	 * @param firstName The first name of the employee
	 * @param lastName The last name of the employee
	 * @return The created Employee object
	 */
	@Tool(name = "saveEmployeeByName", description = "Creates a new employee using the provided first name and last name.")
	public Employee saveEmployeeByName(String firstName, String lastName) {
		logger.info("Enter saveEmployeeByName(String firstName, String lastName)");
		logger.debug("saveEmployeeByName inputs - firstName: {}, lastName: {}", firstName, lastName);
		Employee employee = new Employee(firstName, lastName);
		logger.info("Exit saveEmployeeByName(String firstName, String lastName)");
		return employeeRepository.save(employee);
	}

	/**
	 * Retrieve a list of all employees
	 * 
	 * @return A list containing all employees
	 */
	@Tool(name = "fetchEmployeeList", description = "Get a list of all employees")
	@Override
	public List<Employee> fetchEmployeeList() {
		logger.debug("Enter fetchEmployeeList()");
		List<Employee> employeesList = (List<Employee>) employeeRepository.findAll();
		return employeesList;
	}

	/**
	 * Update an existing employee
	 *
	 * @param employee The employee object with updated details
	 * @param employeeId The ID of the employee to be updated
	 * @return The updated Employee object
	 */
	@Tool(name = "updateEmployee", description = "Updates an existing employee.")
	@Override
	public Employee updateEmployee(Employee employee, Long employeeId) {
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
	 * Search users by any combination of the following parameters:
	 * firstName, lastName, startAge, endAge, department, title, businessUnit,
	 * gender, ethnicity, hireDate, hireDateFirst, hireDateLast, annualSalary.
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
	 * @param hireDate The hire date of the employee (optional)
	 * @param hireDateFirst The earliest hire date (optional)
	 * @param hireDateLast The latest hire date (optional)
	 * @param annualSalary The annual salary of the employee (optional)
	 * @return List of employees matching the criteria
	 */
	@Tool(
		name = "searchUsers",
		description = "Search users by any combination of the following parameters: firstName, lastName, startAge, " +
		"endAge, department, title, businessUnit, gender, ethnicity, managerId, hireDate, hireDateFirst, hireDateLast, annualSalary."
	)
	public List<Employee> searchUsers(
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
		@ToolParam(required = false) Long annualSalary
	)
	{
		logger.info("Entering searchUsers with parameters: firstName={}, lastName={}, startAge={}, endAge={}, department={}, title={}, businessUnit={}, gender={}, ethnicity={}, managerId={}, hireDate={}, hireDateFirst={}, hireDateLast={}, annualSalary={}",
			firstName, lastName, startAge, endAge, department, title, businessUnit, gender, ethnicity, managerId, hireDate, hireDateFirst, hireDateLast, annualSalary);

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

		// Pageable limit = PageRequest.of(0, 10);
		List<Employee> result = employeeRepository.findAll(spec);

		logger.info("Exiting searchUsers. Found {} employees.", result.size());
		return result;
	}
}
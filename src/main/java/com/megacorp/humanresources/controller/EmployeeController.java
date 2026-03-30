package com.megacorp.humanresources.controller;

import java.time.LocalDate;
import java.util.List;

import com.megacorp.humanresources.entity.Employee;
import com.megacorp.humanresources.model.EmployeeCount;
import com.megacorp.humanresources.service.EmployeeService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;


@RestController
public class EmployeeController {

	private static final Logger log = LoggerFactory.getLogger(EmployeeController.class);

	@Autowired
	private EmployeeService employeeService;

	// Save operation
	@PostMapping("/employees")
	public Employee saveEmployee(@Valid @RequestBody Employee employee) {
		log.debug("Entering saveEmployee for employee id={} name={} {}", employee.getEmployeeId(), employee.getFirstName(), employee.getLastName());
		Employee savedEmployee = employeeService.saveEmployee(employee);
		log.info("Employee saved successfully with id={}", savedEmployee.getEmployeeId());
		return savedEmployee;
	}

	// Delete operation
	@DeleteMapping("/employees/{id}")
	public String deleteEmployeeById(@PathVariable("id") Long employeeId) {
		log.debug("Entering deleteEmployeeById with id={}", employeeId);
		employeeService.deleteEmployeeById(employeeId);
		log.info("Employee deleted successfully with id={}", employeeId);
		return "Deleted Successfully";
	}
	
	// Get operation
	@GetMapping("/employees/{id}")
	public Employee employeeById(@PathVariable("id") Long employeeId) {
		log.debug("Entering employeeById with id={}", employeeId);
		Employee employee = employeeService.getEmployeeById(employeeId);
		log.info("Employee retrieved successfully with id={}", employeeId);
		return employee;
	}

	// List operation
	@GetMapping("/employees")
	public List<Employee> fetchEmployeeList() {
		log.debug("Entering fetchEmployeeList");
		List<Employee> employees = employeeService.fetchEmployeeList();
		log.info("Fetched {} employees", employees.size());
		return employees;
	}

	// Partial update operation
	@PatchMapping("/employees/{id}")
	public Employee updateEmployeeById(@PathVariable("id") Long employeeId, @RequestBody EmployeeUpdateRequest request) {
		log.debug("Entering updateEmployeeById with id={}", employeeId);
		Employee updatedEmployee = employeeService.updateEmployee(
			employeeId,
			request.firstName,
			request.lastName,
			request.age,
			request.department,
			request.title,
			request.businessUnit,
			request.gender,
			request.ethnicity,
			request.managerId,
			request.addressId,
			request.hireDate,
			request.terminationDate,
			request.annualSalary
		);
		log.info("Employee updated successfully with id={}", employeeId);
		return updatedEmployee;
	}

	// Search operation
	@GetMapping("/employees/search")
	public String searchEmployees(
			@RequestParam(required = false) String firstName,
			@RequestParam(required = false) String lastName,
			@RequestParam(required = false) Integer startAge,
			@RequestParam(required = false) Integer endAge,
			@RequestParam(required = false) String department,
			@RequestParam(required = false) String title,
			@RequestParam(required = false) String businessUnit,
			@RequestParam(required = false) String gender,
			@RequestParam(required = false) String ethnicity,
			@RequestParam(required = false) Long managerId,
			@RequestParam(required = false) Long addressId,
			@RequestParam(required = false) String state,
			@RequestParam(required = false) String city,
			@RequestParam(required = false) String postalCode,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hireDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hireDateFirst,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hireDateLast,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate terminationDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate terminationDateFirst,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate terminationDateLast,
			@RequestParam(required = false) Long annualSalary,
			@RequestParam(required = false) Integer pageNumber,
			@RequestParam(required = false) Integer pageSize,
			@RequestParam(required = false) String sortBy,
			@RequestParam(required = false) String sortDirection) {

		log.debug("Entering searchEmployees endpoint");
		return employeeService.searchEmployees(
				firstName,
				lastName,
				startAge,
				endAge,
				department,
				title,
				businessUnit,
				gender,
				ethnicity,
				managerId,
				addressId,
				state,
				city,
				postalCode,
				hireDate,
				hireDateFirst,
				hireDateLast,
				terminationDate,
				terminationDateFirst,
				terminationDateLast,
				annualSalary,
				pageNumber,
				pageSize,
				sortBy,
				sortDirection
		);
	}

	// Count operation
	@GetMapping("/employees/count")
	public EmployeeCount countEmployees(
			@RequestParam(required = false) String firstName,
			@RequestParam(required = false) String lastName,
			@RequestParam(required = false) Integer startAge,
			@RequestParam(required = false) Integer endAge,
			@RequestParam(required = false) String department,
			@RequestParam(required = false) String title,
			@RequestParam(required = false) String businessUnit,
			@RequestParam(required = false) String gender,
			@RequestParam(required = false) String ethnicity,
			@RequestParam(required = false) Long managerId,
			@RequestParam(required = false) Long addressId,
			@RequestParam(required = false) String state,
			@RequestParam(required = false) String city,
			@RequestParam(required = false) String postalCode,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hireDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hireDateFirst,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hireDateLast,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate terminationDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate terminationDateFirst,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate terminationDateLast,
			@RequestParam(required = false) Long annualSalary) {

		log.debug("Entering countEmployees endpoint");
		return employeeService.countEmployees(
				firstName,
				lastName,
				startAge,
				endAge,
				department,
				title,
				businessUnit,
				gender,
				ethnicity,
				managerId,
				addressId,
				state,
				city,
				postalCode,
				hireDate,
				hireDateFirst,
				hireDateLast,
				terminationDate,
				terminationDateFirst,
				terminationDateLast,
				annualSalary
		);
	}

	private static class EmployeeUpdateRequest {
		public String firstName;
		public String lastName;
		public Long age;
		public String department;
		public String title;
		public String businessUnit;
		public String gender;
		public String ethnicity;
		public Long managerId;
		public Long addressId;
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
		public LocalDate hireDate;
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
		public LocalDate terminationDate;
		public Long annualSalary;
	}
}

package com.megacorp.humanresources.service;

import java.time.LocalDate;
import java.util.List;

import com.megacorp.humanresources.entity.Employee;
import com.megacorp.humanresources.model.EmployeeCount;

public interface EmployeeService {
	// Save operation
	Employee saveEmployee(Employee employee);

	Employee saveEmployeeByName(String firstName, String lastName);

	Employee updateEmployee(
		Long employeeId,
		String firstName,
		String lastName,
		Long age,
		String department,
		String title,
		String businessUnit,
		String gender,
		String ethnicity,
		Long managerId,
		Long addressId,
		LocalDate hireDate,
		LocalDate terminationDate,
		Long annualSalary
	);

	// Get operation
	Employee getEmployeeById(Long employeeId);

	// Delete operation
	void deleteEmployeeById(Long employeeId);

	List<Employee> fetchEmployeeList();

	String searchEmployees(
		String firstName,
		String lastName,
		Integer startAge,
		Integer endAge,
		String department,
		String title,
		String businessUnit,
		String gender,
		String ethnicity,
		Long managerId,
		Long addressId,
		String state,
		String city,
		String postalCode,
		LocalDate hireDate,
		LocalDate hireDateFirst,
		LocalDate hireDateLast,
		LocalDate terminationDate,
		LocalDate terminationDateFirst,
		LocalDate terminationDateLast,
		Long annualSalary,
		Integer pageNumber, 
		Integer pageSize, 
		String sortBy, 
		String sortDirection
	);

	
	EmployeeCount countEmployees(
		String firstName,
		String lastName,
		Integer startAge,
		Integer endAge,
		String department,
		String title,
		String businessUnit,
		String gender,
		String ethnicity,
		Long managerId,
		Long addressId,
		String state,
		String city,
		String postalCode,
		LocalDate hireDate,
		LocalDate hireDateFirst,
		LocalDate hireDateLast,
		LocalDate terminationDate,
		LocalDate terminationDateFirst,
		LocalDate terminationDateLast,
		Long annualSalary
	);

}

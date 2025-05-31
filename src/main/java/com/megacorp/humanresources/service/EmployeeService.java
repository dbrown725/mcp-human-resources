package com.megacorp.humanresources.service;

import org.springframework.data.domain.Page;

import com.megacorp.humanresources.entity.Employee;

public interface EmployeeService {
	// Save operation
	Employee saveEmployee(Employee employee);

	// Read operation
	Page<Employee> fetchEmployeePage(Integer pageNumber, Integer pageSize, String sortBy, String sortDirection);

	// Update operation
	Employee updateEmployee(Employee employee, Long id);

	// Delete operation
	void deleteEmployeeById(Long employeeId);

	// Get operation
	Employee getEmployeeById(Long employeeId);

}

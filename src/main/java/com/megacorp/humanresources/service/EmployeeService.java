package com.megacorp.humanresources.service;

import java.util.List;

import com.megacorp.humanresources.entity.Employee;

public interface EmployeeService {
	// Save operation
	Employee saveEmployee(Employee employee);

	// Read operation
	List<Employee> fetchEmployeeList();

	// Update operation
	Employee updateEmployee(Employee employee, Long dId);

	// Delete operation
	void deleteEmployeeById(Long employeeId);

	// Get operation
	Employee getEmployeeById(Long employeeId);

}

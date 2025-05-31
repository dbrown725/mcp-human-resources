package com.megacorp.humanresources.service;

import com.megacorp.humanresources.entity.Employee;

public interface EmployeeService {
	// Save operation
	Employee saveEmployee(Employee employee);

	// Delete operation
	void deleteEmployeeById(Long employeeId);

	// Get operation
	Employee getEmployeeById(Long employeeId);

}

package com.megacorp.humanresources.service;

import com.megacorp.humanresources.entity.Employee;
import com.megacorp.humanresources.repository.EmployeeRepository;
import java.util.List;
import java.util.Objects;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.extern.slf4j.Slf4j;

//Annotation
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
	 * @param employee object
	 * @return Employee object
	 */
	@Tool(name = "saveEmployee", description = "Creates a new employee based on the passed employee object.")
	@Override
	public Employee saveEmployee(Employee employee) {
		employee.setEmployeeId(null);
		return employeeRepository.save(employee);
	}

	/**
	 * Get a list of all employees
	 * 
	 * @return The employee list
	 */
	@Tool(name = "fetchEmployeeList", description = "Get a list of all employees")
	@Override
	public List<Employee> fetchEmployeeList() {
		logger.debug("Enter fetchEmployeeList()");
		List<Employee> employeesList = (List<Employee>) employeeRepository.findAll();
		return employeesList;
	}

	/**
	 * Update an employee
	 *
	 * @param employee
	 * @param employee id Long
	 * @return Employee object
	 */
	@Tool(name = "updateEmployee", description = "Updates an existing employee.")
	@Override
	public Employee updateEmployee(Employee employee, Long employeeId) {
		logger.debug("Enter updateEmployee(Employee employee:, Long employeeId) employee: " + employee + " employeeId: " + employeeId);
		return employeeRepository.save(employee);
	}

	/**
	 * Get a single employee by ID
	 *
	 * @param id The employee ID
	 * @return Employee object
	 */
	@Override
	@Tool(name = "getEmployeeById", description = "Get a single employee by ID")
	public Employee getEmployeeById(Long employeeId) {
		logger.debug("Enter getEmployeeById(Long employeeId) employeeId: " + employeeId);
		return employeeRepository.findById(employeeId).get();
	}

	/**
	 * Delete a single employee by ID
	 *
	 * @param id The employee ID
	 * @return void
	 */
	@Override
	@Tool(name = "deleteEmployeeById", description = "Delete a single employee by ID")
	public void deleteEmployeeById(Long employeeId) {
		logger.debug("Enter deleteEmployeeById(Long employeeId) employeeId: " + employeeId);
		employeeRepository.deleteById(employeeId);
	}
}
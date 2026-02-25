package com.megacorp.humanresources.controller;

import com.megacorp.humanresources.entity.Employee;
import com.megacorp.humanresources.service.EmployeeService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
	public Employee EmployeeById(@PathVariable("id") Long employeeId) {
		log.debug("Entering EmployeeById with id={}", employeeId);
		Employee employee = employeeService.getEmployeeById(employeeId);
		log.info("Employee retrieved successfully with id={}", employeeId);
		return employee;
	}
}

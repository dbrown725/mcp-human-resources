package com.megacorp.humanresources.controller;

import com.megacorp.humanresources.entity.Employee;
import com.megacorp.humanresources.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
public class EmployeeController {

	@Autowired
	private EmployeeService employeeService;

	// Save operation
	@PostMapping("/employees")
	public Employee saveEmployee(@Valid @RequestBody Employee employee) {
		return employeeService.saveEmployee(employee);
	}

	// Update operation
	@PutMapping("/employees/{id}")
	public Employee updateEmployee(@RequestBody Employee employee, @PathVariable("id") Long employeeId) {
		return employeeService.updateEmployee(employee, employeeId);
	}

	// Delete operation
	@DeleteMapping("/employees/{id}")
	public String deleteEmployeeById(@PathVariable("id") Long employeeId) {
		employeeService.deleteEmployeeById(employeeId);
		return "Deleted Successfully";
	}
	
	// Get operation
	@GetMapping("/employees/{id}")
	public Employee EmployeeById(@PathVariable("id") Long employeeId) {
		return employeeService.getEmployeeById(employeeId);
	}
}

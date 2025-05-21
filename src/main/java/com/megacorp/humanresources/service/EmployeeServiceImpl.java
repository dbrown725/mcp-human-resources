package com.megacorp.humanresources.service;

import com.megacorp.humanresources.entity.Employee;
import com.megacorp.humanresources.repository.EmployeeRepository;
import java.util.List;
import java.util.Objects;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

//Annotation
@Service

//Class
public class EmployeeServiceImpl
 implements EmployeeService {

 @Autowired
 private EmployeeRepository employeeRepository;

 // Save operation
 @Override
 public Employee saveEmployee(Employee employee)
 {
     return employeeRepository.save(employee);
 }

 /**
	 * Get a list of all employees
	 * @return The employee list
*/
 @Tool(description = "Get a list of all employees")
 @Override public List<Employee> fetchEmployeeList()
 {
	 List<Employee> employeesList = (List<Employee>)
	         employeeRepository.findAll();
     return employeesList;
 }

 // Update operation
 @Override
 public Employee
 updateEmployee(Employee employee,
                  Long employeeId)
 {
     Employee depDB
         = employeeRepository.findById(employeeId)
               .get();

     if (Objects.nonNull(employee.getName())
         && !"".equalsIgnoreCase(
             employee.getName())) {
         depDB.setName(
             employee.getName());
     }


     return employeeRepository.save(depDB);
 }
 
 /**
  * Get a single employee by ID
  *
  * @param id The employee ID
  * @return Employee object
  */
 @Tool(name = "getEmployeeById", description = "Get a single employee by ID")
 public Employee getEmployeeById(Long employeeId) {
   return employeeRepository.findById(employeeId)
           .get();
 }

 // Delete operation
 @Override
 public void deleteEmployeeById(Long employeeId)
 {
     employeeRepository.deleteById(employeeId);
 }
}
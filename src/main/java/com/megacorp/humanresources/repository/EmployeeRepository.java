package com.megacorp.humanresources.repository;

import com.megacorp.humanresources.entity.Employee;
import org.springframework.data.repository.CrudRepository;

//Interface extending CrudRepository
public interface EmployeeRepository extends CrudRepository<Employee, Long> {
}

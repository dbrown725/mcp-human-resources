package com.megacorp.humanresources.repository;

import com.megacorp.humanresources.entity.Employee;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

//Interface extending CrudRepository
public interface EmployeeRepository
 extends CrudRepository<Employee, Long> {
}

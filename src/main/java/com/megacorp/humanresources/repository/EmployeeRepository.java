package com.megacorp.humanresources.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.megacorp.humanresources.entity.Employee;

//Interface extending CrudRepository
public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {
    Page<Employee> findAll(Pageable pageable);
}

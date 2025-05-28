package com.megacorp.humanresources.specifications;

import java.util.Date;

import org.springframework.data.jpa.domain.Specification;

import com.megacorp.humanresources.entity.Employee;

public class EmployeeSpecifications {

	public static Specification<Employee> hasFirstName(String firstName) {
		return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("firstName"), firstName);
	}

	public static Specification<Employee> hasLastName(String lastName) {
		return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("lastName"), lastName);
	}

	public static Specification<Employee> ageBetween(int startAge, int endAge) {
		return (root, query, criteriaBuilder) -> criteriaBuilder.between(root.get("age"), startAge, endAge);
	}

	public static Specification<Employee> hasDepartment(String department) {
		return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("department"), department);
	}

	public static Specification<Employee> hasTitle(String title) {
		return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("title"), title);
	}

	public static Specification<Employee> hasGender(String gender) {
		return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("gender"), gender);
	}

	public static Specification<Employee> hasEthnicity(String ethnicity) {
		return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("ethnicity"), ethnicity);
	}

	public static Specification<Employee> hasBusinessUnit(String businessUnit) {
		return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("businessUnit"), businessUnit);
	}

	public static Specification<Employee> hasHireDate(String hireDate) {
		return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("hireDate"), hireDate);
	}

	public static Specification<Employee> hasHireDateBetween(Date hireDateFirst, Date hireDateLast) {
		return (root, query, criteriaBuilder) -> criteriaBuilder.between(root.get("hireDate"), hireDateFirst,
				hireDateLast);
	}

	public static Specification<Employee> hasAnnualSalary(Long annualSalary) {
		return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("annualSalary"), annualSalary);
	}
}
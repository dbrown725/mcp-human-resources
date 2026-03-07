package com.megacorp.humanresources.specifications;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

import org.springframework.data.jpa.domain.Specification;

import com.megacorp.humanresources.entity.Employee;

import jakarta.persistence.criteria.Join;

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

	private static java.sql.Date toSqlDateUtc(Date date) {
		LocalDate utcDate = Instant.ofEpochMilli(date.getTime()).atZone(ZoneOffset.UTC).toLocalDate();
		return java.sql.Date.valueOf(utcDate);
	}

	public static Specification<Employee> hasHireDate(Date hireDate) {
		java.sql.Date normalizedDate = toSqlDateUtc(hireDate);
		return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("hireDate").as(java.sql.Date.class), normalizedDate);
	}

	public static Specification<Employee> hasHireDateBetween(Date hireDateFirst, Date hireDateLast) {
		java.sql.Date firstDate = toSqlDateUtc(hireDateFirst);
		java.sql.Date lastDate = toSqlDateUtc(hireDateLast);
		return (root, query, criteriaBuilder) -> criteriaBuilder.between(root.get("hireDate").as(java.sql.Date.class), firstDate,
				lastDate);
	}

	public static Specification<Employee> hasTerminationDate(Date terminationDate) {
		java.sql.Date normalizedDate = toSqlDateUtc(terminationDate);
		return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("terminationDate").as(java.sql.Date.class), normalizedDate);
	}

	public static Specification<Employee> hasTerminationDateBetween(Date terminationDateFirst, Date terminationDateLast) {
		java.sql.Date firstDate = toSqlDateUtc(terminationDateFirst);
		java.sql.Date lastDate = toSqlDateUtc(terminationDateLast);
		return (root, query, criteriaBuilder) -> criteriaBuilder.between(root.get("terminationDate").as(java.sql.Date.class), firstDate,
				lastDate);
	}

	public static Specification<Employee> hasAnnualSalary(Long annualSalary) {
		return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("annualSalary"), annualSalary);
	}

	public static Specification<Employee> hasManagerId(Long managerId) {
		return (root, query, criteriaBuilder) -> {
            // Join the Employee entity with its manager (which is also an Employee)
            Join<Employee, Employee> managerJoin = root.join("manager");
            // Create a predicate to filter by the manager's ID
            return criteriaBuilder.equal(managerJoin.get("employeeId"), managerId);
        };
	}

	public static Specification<Employee> hasAddressId(Long addressId) {
		return (root, query, criteriaBuilder) -> {
			Join<Employee, ?> addressJoin = root.join("address");
			return criteriaBuilder.equal(addressJoin.get("addressId"), addressId);
		};
	}

}
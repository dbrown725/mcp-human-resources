package com.megacorp.humanresources.specifications;

import java.time.LocalDate;

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

	public static Specification<Employee> hasHireDate(LocalDate hireDate) {
		return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("hireDate"), hireDate);
	}

	public static Specification<Employee> hasHireDateBetween(LocalDate hireDateFirst, LocalDate hireDateLast) {
		return (root, query, criteriaBuilder) -> criteriaBuilder.between(root.get("hireDate"), hireDateFirst, hireDateLast);
	}

	public static Specification<Employee> hasTerminationDate(LocalDate terminationDate) {
		return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("terminationDate"), terminationDate);
	}

	public static Specification<Employee> hasTerminationDateBetween(LocalDate terminationDateFirst, LocalDate terminationDateLast) {
		return (root, query, criteriaBuilder) -> criteriaBuilder.between(root.get("terminationDate"), terminationDateFirst, terminationDateLast);
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

	public static Specification<Employee> hasAddressState(String state) {
		return (root, query, criteriaBuilder) -> {
			Join<Employee, ?> addressJoin = root.join("address");
			return criteriaBuilder.equal(criteriaBuilder.upper(addressJoin.get("state")), state.toUpperCase());
		};
	}

	public static Specification<Employee> hasAddressCity(String city) {
		return (root, query, criteriaBuilder) -> {
			Join<Employee, ?> addressJoin = root.join("address");
			return criteriaBuilder.equal(criteriaBuilder.upper(addressJoin.get("city")), city.toUpperCase());
		};
	}

	public static Specification<Employee> hasAddressPostalCode(String postalCode) {
		return (root, query, criteriaBuilder) -> {
			Join<Employee, ?> addressJoin = root.join("address");
			return criteriaBuilder.equal(addressJoin.get("postalCode"), postalCode);
		};
	}

}
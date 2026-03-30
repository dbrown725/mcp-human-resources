package com.megacorp.humanresources.entity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.SequenceGenerator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
	name = "employee",
	indexes = {
		@Index(name = "idx_manager_id", columnList = "MANAGER_ID")
	}
)
public class Employee {
	
	private static final Logger logger = LoggerFactory.getLogger(Employee.class);

	public Employee(String firstName, String lastName){
		this.firstName = firstName;
		this.lastName = lastName;
	 }

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "employee_seq")
	@SequenceGenerator(name = "employee_seq", sequenceName = "employee_sequence", allocationSize = 1, initialValue=6000)
	@Column(name = "EMPLOYEE_ID")
	private Long employeeId;

	@Column(name = "FIRST_NAME", nullable = false)
	private String firstName;

	@Column(name = "LAST_NAME", nullable = false)
	private String lastName;

	@Column(name = "TITLE", nullable = true)
	private String title;

	@Column(name = "DEPARTMENT", nullable = true)
	private String department;

	@Column(name = "BUSINESS_UNIT", nullable = true)
	private String businessUnit;

	@Column(name = "GENDER", nullable = true)
	private String gender;

	@Column(name = "ETHNICITY", nullable = true)
	private String ethnicity;

	@Column(name = "AGE", nullable = true)
	private Long age;

	@ManyToOne
	@JoinColumn(name = "MANAGER_ID", insertable = true, updatable = true, nullable = true)
	@com.fasterxml.jackson.annotation.JsonIgnore
	private Employee manager;

	@ManyToOne
	@JoinColumn(name = "ADDRESS_ID", insertable = true, updatable = true, nullable = true)
	private Address address;

	public void setManager(Employee manager) {
		this.manager = manager;
	}

	@JsonProperty("managerId")
	public Long getManagerId() {
		return this.manager != null ? this.manager.getEmployeeId() : null;
	}

	@JsonProperty("addressId")
	public Long getAddressId() {
		return this.address != null ? this.address.getAddressId() : null;
	}
	
	@Column(name = "HIRE_DATE", nullable = true)
	private LocalDate hireDate;

	@Column(name = "TERMINATION_DATE", nullable = true)
	private LocalDate terminationDate;
	
	@Column(name = "ANNUAL_SALARY", nullable = true)
	private Long annualSalary;

	@Override
	public String toString() {
		return "Employee{" +
				"employeeId=" + employeeId +
				", firstName='" + firstName + '\'' +
				", lastName='" + lastName + '\'' +
				", title='" + title + '\'' +
				", department='" + department + '\'' +
				", businessUnit='" + businessUnit + '\'' +
				", gender='" + gender + '\'' +
				", ethnicity='" + ethnicity + '\'' +
				", age=" + age +
				", managerId=" + (manager != null ? this.manager.getEmployeeId() : null) +
				", addressId=" + (address != null ? this.address.getAddressId() : null) +
				", hireDate=" + (hireDate != null ? hireDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")) : null) +
				", terminationDate=" + (terminationDate != null ? terminationDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")) : null) +
				", annualSalary=" + annualSalary +
				'}';
	}
}

package com.megacorp.humanresources.entity;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
// import jakarta.persistence.ManyToOne;
// import jakarta.persistence.JoinColumn;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
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

	@Column(name = "MANAGER_ID", nullable = true)
	private Long managerId;

	// Self-referencing relationship to Employee (manager)
	// This is optional, but recommended for JPA navigation
	// Uncomment if you want to use object reference instead of just ID:
	// Revisit later - I tried, but Pydantic kept spewing an error: "ERROR - Failed while agent.run: Could not find $ref definition for #"
	// @ManyToOne
	// @JoinColumn(name = "MANAGER_ID", insertable = false, updatable = false, nullable = true)
	// private Employee manager;
	
	@Temporal(TemporalType.DATE)
	@Column(name = "HIRE_DATE", nullable = true)
	private Date hireDate;
	
	@Column(name = "ANNUAL_SALARY", nullable = true)
	private Long annualSalary;
	
    public String getFormattedDate() {
		if (this.hireDate == null) {
			logger.warn("Hire date is null, returning empty string");
			return "";
		}
		SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
		String formattedDate = dateFormat.format(this.hireDate);
		logger.info("Returning formatted hire date: {}", formattedDate);
		return formattedDate;
    }

    public void setFormattedDate(String dateString) {
		logger.info("Entering setFormattedDate");
		if (dateString == null || dateString.isEmpty()) {
			this.hireDate = null;
			return;
		}
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
            this.hireDate = dateFormat.parse(dateString);
        } catch (Exception e) {
            // Handle exception, maybe log or throw a custom exception
            e.printStackTrace();
			logger.error("Error parsing date string: {}", dateString, e);
        }
		logger.info("Exiting setFormattedDate");
    }

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
				", managerId=" + managerId +
				", hireDate=" + (hireDate != null ? new SimpleDateFormat("MM/dd/yyyy").format(hireDate) : null) +
				", annualSalary=" + annualSalary +
				'}';
	}
}

package com.megacorp.humanresources.entity;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.Column;
//Importing required classes (using jakarta.persistence now)
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

public class Employee {
	
	private static final Logger logger = LoggerFactory.getLogger(Employee.class);

	public Employee(String firstName, String lastName){
		this.firstName = firstName;
		this.lastName = lastName;
	 }

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long employeeId;
	private String firstName;
	private String lastName;
	private String title;
	private String department;
	private String businessUnit;
	private String gender;
	private String ethnicity;
	private Long age;
	
	@Temporal(TemporalType.DATE)
	@Column(name = "HIRE_DATE", nullable = true)
	private Date hireDate;
	
	private Long annualSalary;
	
    public String getFormattedDate() {
		if (this.hireDate == null) {
			logger.warn("Hire date is null, returning empty string");
			return "";
		}
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        return dateFormat.format(this.hireDate);
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
}

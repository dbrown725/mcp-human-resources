package com.megacorp.humanresources.entity;

import java.text.SimpleDateFormat;
import java.util.Date;

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
	private Date hireDate;
	
	private Long annualSalary;
	
    public String getFormattedDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        return dateFormat.format(this.hireDate);
    }

    public void setFormattedDate(String dateString) {
         try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
            this.hireDate = dateFormat.parse(dateString);
        } catch (Exception e) {
            // Handle exception, maybe log or throw a custom exception
            e.printStackTrace();
        }
    }
	
}

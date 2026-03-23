package com.megacorp.humanresources.entity;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;
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
    name = "EMPLOYEE_BENEFIT",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_employee_plan", columnNames = {"EMPLOYEE_ID", "PLAN_ID"})
    },
    indexes = {
        @Index(name = "idx_eb_employee_id", columnList = "EMPLOYEE_ID"),
        @Index(name = "idx_eb_plan_id", columnList = "PLAN_ID")
    }
)
public class EmployeeBenefit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ENROLLMENT_ID")
    private Long enrollmentId;

    @ManyToOne
    @JoinColumn(name = "EMPLOYEE_ID", nullable = false)
    @JsonIgnore
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "PLAN_ID", nullable = false)
    private BenefitsConfig benefitsPlan;

    @Temporal(TemporalType.DATE)
    @Column(name = "ENROLLMENT_DATE")
    private Date enrollmentDate;

    @JsonProperty("employeeId")
    public Long getEmployeeId() {
        return employee != null ? employee.getEmployeeId() : null;
    }

    @JsonProperty("employeeName")
    public String getEmployeeName() {
        return employee != null ? employee.getFirstName() + " " + employee.getLastName() : null;
    }

    @Override
    public String toString() {
        return "EmployeeBenefit{" +
                "enrollmentId=" + enrollmentId +
                ", employeeId=" + (employee != null ? employee.getEmployeeId() : null) +
                ", planId=" + (benefitsPlan != null ? benefitsPlan.getPlanId() : null) +
                ", planName=" + (benefitsPlan != null ? benefitsPlan.getPlanName() : null) +
                ", planType=" + (benefitsPlan != null ? benefitsPlan.getPlanType() : null) +
                ", enrollmentDate=" + enrollmentDate +
                '}';
    }
}

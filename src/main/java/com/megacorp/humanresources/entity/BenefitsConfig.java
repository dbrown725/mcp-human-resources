package com.megacorp.humanresources.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
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
    name = "BENEFITS_CONFIG",
    indexes = {
        @Index(name = "idx_plan_type", columnList = "PLAN_TYPE")
    }
)
public class BenefitsConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "benefits_config_seq")
    @SequenceGenerator(name = "benefits_config_seq", sequenceName = "benefits_config_sequence", allocationSize = 1, initialValue = 1)
    @Column(name = "PLAN_ID")
    private Long planId;

    @Column(name = "PLAN_NAME", nullable = false)
    private String planName;

    @Column(name = "PLAN_TYPE", nullable = false, length = 50)
    private String planType;

    @Column(name = "PROVIDER")
    private String provider;

    @Column(name = "COVERAGE_LEVEL", length = 100)
    private String coverageLevel;

    @Column(name = "MONTHLY_PREMIUM", precision = 10, scale = 2)
    private BigDecimal monthlyPremium;

    @Column(name = "DEDUCTIBLE", precision = 10, scale = 2)
    private BigDecimal deductible;

    @Column(name = "OUT_OF_POCKET_MAX", precision = 10, scale = 2)
    private BigDecimal outOfPocketMax;

    @Column(name = "ENROLLMENT_STATUS")
    @Builder.Default
    private Boolean enrollmentStatus = true;

    @Column(name = "DESCRIPTION", length = 1000)
    private String description;

    @Override
    public String toString() {
        return "BenefitsConfig{" +
                "planId=" + planId +
                ", planName='" + planName + '\'' +
                ", planType='" + planType + '\'' +
                ", provider='" + provider + '\'' +
                ", coverageLevel='" + coverageLevel + '\'' +
                ", monthlyPremium=" + monthlyPremium +
                ", deductible=" + deductible +
                ", outOfPocketMax=" + outOfPocketMax +
                ", enrollmentStatus=" + enrollmentStatus +
                ", description='" + description + '\'' +
                '}';
    }
}

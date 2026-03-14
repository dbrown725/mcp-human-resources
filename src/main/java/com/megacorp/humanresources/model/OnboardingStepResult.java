package com.megacorp.humanresources.model;

import com.fasterxml.jackson.annotation.JsonClassDescription;

/**
 * Represents the result of a judge evaluation for an onboarding workflow step.
 * Used with Spring AI structured output to parse LLM-as-a-Judge responses.
 *
 * Rating scale:
 * 1: Step completely failed or was not executed
 * 2: Step partially completed but missing key requirements
 * 3: Step mostly completed with minor issues
 * 4: Step fully completed and meets all criteria
 *
 * A rating of successRating (configurable, default 3) or above means passed=true.
 */
@JsonClassDescription("Result of evaluating an onboarding workflow step")
public record OnboardingStepResult(
		boolean passed,
		int rating,
		String evaluation,
		String feedback) {
}

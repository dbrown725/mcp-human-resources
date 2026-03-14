package com.megacorp.humanresources.exceptions;

/**
 * Exception thrown when an onboarding workflow step fails after exhausting all retry attempts.
 * Carries the step name and failure details for building the failure report.
 */
public class OnboardingStepException extends RuntimeException {

	private final String stepName;

	public OnboardingStepException(String stepName, String message) {
		super(message);
		this.stepName = stepName;
	}

	public OnboardingStepException(String stepName, String message, Throwable cause) {
		super(message, cause);
		this.stepName = stepName;
	}

	public String getStepName() {
		return stepName;
	}
}

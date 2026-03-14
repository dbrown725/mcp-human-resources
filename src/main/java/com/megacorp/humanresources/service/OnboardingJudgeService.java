package com.megacorp.humanresources.service;

import com.megacorp.humanresources.model.OnboardingStepResult;

/**
 * LLM-as-a-Judge service for evaluating onboarding workflow steps.
 * 
 * Follows the same evaluation pattern as {@link com.megacorp.humanresources.advisors.SelfRefineEvaluationAdvisor}
 * but designed for explicit step-by-step workflow validation rather than advisor-chain integration.
 * 
 * Uses a dedicated judge ChatClient (configured via application.properties) with structured output
 * to evaluate whether each workflow step was completed successfully based on specific criteria.
 */
public interface OnboardingJudgeService {

	/**
	 * Evaluates an onboarding workflow step using LLM-as-a-Judge.
	 *
	 * @param stepDescription description of what the step was supposed to accomplish
	 * @param criteria        specific acceptance criteria that must be met
	 * @param evidence        the actual results/evidence from executing the step
	 * @return an {@link OnboardingStepResult} with rating, pass/fail, evaluation, and feedback
	 */
	OnboardingStepResult evaluate(String stepDescription, String criteria, String evidence);
}

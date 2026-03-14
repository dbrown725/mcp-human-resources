package com.megacorp.humanresources.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.megacorp.humanresources.model.OnboardingStepResult;

/**
 * LLM-as-a-Judge implementation for onboarding workflow step evaluation.
 *
 * Uses the judge model (configured via application.properties, temperature 0.0) as the judge LLM,
 * following the same evaluation pattern as {@link com.megacorp.humanresources.advisors.SelfRefineEvaluationAdvisor}.
 *
 * The judge evaluates each step by:
 * 1. Receiving a step description, acceptance criteria, and evidence of what was done
 * 2. Producing a structured {@link OnboardingStepResult} with rating (1-4), pass/fail, evaluation, and feedback
 * 3. Rating of {@code successRating} (configurable) or above means passed
 *
 * Spring AI features used:
 * - ChatClient with structured output (.entity()) for type-safe judge responses
 * - Multiple model support (judge model dedicated to evaluation)
 */
@Service
public class OnboardingJudgeServiceImpl implements OnboardingJudgeService {

	private static final Logger logger = LoggerFactory.getLogger(OnboardingJudgeServiceImpl.class);

	private static final String JUDGE_SYSTEM_PROMPT = """
			You are a quality assurance judge evaluating employee onboarding workflow steps.
			You must be thorough, precise, and fair in your evaluations.
			Always evaluate based ONLY on the provided evidence and criteria.
			Do not make assumptions about what might have happened - evaluate what actually happened.
			""";

	private static final String JUDGE_EVALUATION_TEMPLATE = """
			Evaluate whether this onboarding workflow step was completed successfully.

			STEP DESCRIPTION:
			%s

			ACCEPTANCE CRITERIA:
			%s

			EVIDENCE / ACTUAL RESULTS:
			%s

			Provide your evaluation as a JSON object with these fields:
			- "passed": true if the step meets all acceptance criteria, false otherwise
			- "rating": integer 1-4 where:
			  1 = Step completely failed or was not executed
			  2 = Step partially completed but missing key requirements
			  3 = Step mostly completed with minor issues
			  4 = Step fully completed and meets all criteria
			- "evaluation": your detailed assessment of the step
			- "feedback": specific and constructive feedback (what to fix if failed, or confirmation if passed)

			A rating of %d or above should have passed=true.
			Be strict but fair. If the evidence clearly shows the criteria are met, pass it.
			""";

	private final ChatClient judgeChatClient;
	private final int successRating;

	public OnboardingJudgeServiceImpl(
			@Qualifier("judgeModel") ChatModel judgeChatModel,
			@Value("${onboarding.judge.success-rating:3}") int successRating) {
		this.judgeChatClient = ChatClient.builder(judgeChatModel)
				.defaultSystem(JUDGE_SYSTEM_PROMPT)
				.build();
		this.successRating = successRating;
		logger.info("OnboardingJudgeService initialized with successRating={}", successRating);
	}

	@Override
	public OnboardingStepResult evaluate(String stepDescription, String criteria, String evidence) {
		logger.debug("Evaluating step: {}", stepDescription);
		logger.debug("Criteria: {}", criteria);
		logger.debug("Evidence length: {} chars", evidence != null ? evidence.length() : 0);

		try {
			String prompt = String.format(JUDGE_EVALUATION_TEMPLATE,
					stepDescription, criteria, evidence, successRating);

			OnboardingStepResult result = judgeChatClient.prompt(prompt)
					.call()
					.entity(OnboardingStepResult.class);

			if (result == null) {
				logger.error("Judge returned null result for step: {}", stepDescription);
				return new OnboardingStepResult(false, 1,
						"Judge returned null result", "The evaluation could not be completed");
			}

			logger.info("Judge evaluation for '{}': passed={}, rating={}", stepDescription, result.passed(), result.rating());
			logger.debug("Judge evaluation detail: {}", result.evaluation());

			return result;

		} catch (Exception e) {
			logger.error("Judge evaluation failed for step '{}': {}", stepDescription, e.getMessage(), e);
			return new OnboardingStepResult(false, 1,
					"Judge evaluation error: " + e.getMessage(),
					"The judge encountered an error evaluating this step. Please retry.");
		}
	}
}

/**
 *
 */
package com.iyadk.termsearch;

/**
 * @author iyad
 *
 */
public interface ExcerptFullScorerCriterionI {
	public ExcerptScorerCriterionResult score(Excerpt excerpt, String searchString);
	
	/**
	 * Sets the amount to add to the score
	 * @param penalty
	 */
	public ExcerptFullScorerCriterionI setPenalty(int penalty);

	/**
	 * Sets a flag that this scorer is the last to be evaluated
	 * @param isLast
	 */
	public ExcerptFullScorerCriterionI setLast(boolean isLast);

	/**
	 * Determines if the last flag is set
	 * @return
	 */
	public boolean isLast();
	
	/**
	 * Sets a flag that this scorer is evaluates the expression using
	 * strict parameters (i.e. pass or fail)
	 * @param isLast
	 */
	public ExcerptFullScorerCriterionI setStrict(boolean isStrict);
	
	/**
	 * Get whether the strict flag is set for this scorer.  If the strict flag is set,
	 * the scorer operates in a pass/fail mode.  Otherwise, it returns the penalty as the score function. 
	 * @param isLast
	 */
	public boolean isStrict();
}

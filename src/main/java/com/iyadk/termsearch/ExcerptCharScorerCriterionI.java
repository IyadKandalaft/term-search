package com.iyadk.termsearch;

/**
 * @author iyad
 *
 */
public interface ExcerptCharScorerCriterionI {
	public int score(char currChar);

	/**
	 * Sets the amount to add to the score
	 * @param penalty
	 */
	public ExcerptCharScorerCriterionI setPenalty(int penalty);

	/**
	 * Sets a flag that this scorer is the last to be evaluated
	 * @param isLast
	 */
	public ExcerptCharScorerCriterionI setLast(boolean isLast);

	/**
	 * Determines if the last flag is set
	 * @return
	 */
	public boolean isLast();
}

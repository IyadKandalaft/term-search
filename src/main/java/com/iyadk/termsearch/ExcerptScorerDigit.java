/**
 *
 */
package com.iyadk.termsearch;

/**
 * @author iyad
 *
 */
public class ExcerptScorerDigit implements ExcerptCharScorerCriterionI {
	private int penalty = 1;
	private boolean isLast;

	/**
	 *
	 */
	public ExcerptScorerDigit() {
		setPenalty(1);
		setLast(true);
	}

	@Override
	public int score(char currChar) {
		if(Character.isDigit(currChar)) {
			return penalty;
		}
		return 0;
	}

	@Override
	public ExcerptCharScorerCriterionI setPenalty(int penalty) {
		this.penalty = penalty;
		return this;
	}

	@Override
	public ExcerptCharScorerCriterionI setLast(boolean isLast) {
		this.isLast = isLast;
		return this;
	}

	@Override
	public boolean isLast() {
		return isLast;
	}
}

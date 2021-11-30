/**
 *
 */
package com.iyadk.termsearch;

/**
 * @author iyad
 *
 */
public class ExcerptScorerLength implements ExcerptFullScorerCriterionI {
	private int minLength;
	private int maxLength;
	private int penalty;
	private boolean strict;
	private boolean last;

	/**
	 *
	 */
	public ExcerptScorerLength(int minLength, int maxLength, boolean strict) {
		this.minLength = minLength;
		this.maxLength = maxLength;
		this.penalty = 100;
		this.strict = strict;
		this.last = false;
	}

	@Override
	public ExcerptScorerCriterionResult score(Excerpt excerpt, String searchString) {
		// Skip this match if it doesn't meet our length requirements
		if (excerpt.length() < minLength || excerpt.length() > maxLength) {
			return new ExcerptScorerCriterionResult(penalty, strict ? true : false);
		}

		return new ExcerptScorerCriterionResult(0, false);
	}

	@Override
	public ExcerptFullScorerCriterionI setPenalty(int penalty) {
		this.penalty = penalty;
		return this;
	}

	@Override
	public ExcerptFullScorerCriterionI setLast(boolean isLast) {
		this.last = isLast;
		return this;
	}

	@Override
	public boolean isLast() {
		return last;
	}

	@Override
	public ExcerptFullScorerCriterionI setStrict(boolean isStrict) {
		this.strict = isStrict;
		return this;
	}

	@Override
	public boolean isStrict() {
		return strict;
	}

}

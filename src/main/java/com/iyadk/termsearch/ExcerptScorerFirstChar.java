/**
 *
 */
package com.iyadk.termsearch;

/**
 * @author iyad
 *
 */
public class ExcerptScorerFirstChar implements ExcerptFullScorerCriterionI {
	private int penalty;
	private boolean strict;
	private boolean last;

	/**
	 *
	 */
	public ExcerptScorerFirstChar(boolean strict) {
		this.strict = strict;
		this.penalty = 100;
		this.last = false;
	}

	@Override
	public ExcerptScorerCriterionResult score(Excerpt excerpt, String searchString) {
		if ( Character.isLowerCase(excerpt.charAt(0)) ){
			return new ExcerptScorerCriterionResult(penalty, strict ? true : false);
		}
		return new ExcerptScorerCriterionResult(0, false);
	}

	public int getPenalty() {
		return penalty;
	}

	@Override
	public ExcerptFullScorerCriterionI setPenalty(int penalty) {
		this.penalty = penalty;
		return this;
	}

	@Override
	public boolean isStrict() {
		return strict;
	}

	@Override
	public ExcerptFullScorerCriterionI setStrict(boolean strict) {
		this.strict = strict;
		return this;
	}

	@Override
	public boolean isLast() {
		return last;
	}

	@Override
	public ExcerptFullScorerCriterionI setLast(boolean last) {
		this.last = last;
		return this;
	}
}

/**
 *
 */
package com.iyadk.termsearch;

/**
 * @author iyad
 *
 */
public class ExcerptScorerPunctuation implements ExcerptCharScorerCriterionI {
	private int penalty;
	private boolean isLast;
	/**
	 *
	 */
	public ExcerptScorerPunctuation() {
		setPenalty(1);
		setLast(true);
	}

	@Override
	public int score(char currChar) {
		final int charType = Character.getType(currChar);

		if(charType == Character.CONNECTOR_PUNCTUATION ||
				charType == Character.START_PUNCTUATION ||
				charType == Character.END_PUNCTUATION ||
				charType == Character.OTHER_PUNCTUATION) {
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

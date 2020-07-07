/**
 *
 */
package com.iyadk.termsearch;

/**
 * @author iyad
 *
 */
public class ExcerptScorerCriterionResult {
	public int score;
	public boolean skip;

	public ExcerptScorerCriterionResult(int score, boolean skip) {
		this.score = score;
		this.skip = skip;
	}
}

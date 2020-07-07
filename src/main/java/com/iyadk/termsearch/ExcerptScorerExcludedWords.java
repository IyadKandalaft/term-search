/**
 *
 */
package com.iyadk.termsearch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * @author iyad
 *
 */
public class ExcerptScorerExcludedWords implements ExcerptFullScorerCriterionI {
	private MatchList excludedWords;
	private int penalty;
	private boolean strict;
	private boolean last;

	/**
	 *
	 */
	public ExcerptScorerExcludedWords(Path excludedWordsFile, boolean strict) {
		setExcludedWords(excludedWordsFile);
		this.strict = strict;
		this.penalty = 100;
		this.last = false;
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

	/**
	 * Set a list of words to exclude from the results
	 * @param excludedWords A list of words to exclude from the results
	 */
	public void setExcludedWords(List<String> excludedWords) {
		this.excludedWords = new MatchList(excludedWords);
	}

	/**
	 * Set a list of words to exclude from the results
	 * @param excludedWordsFile A file with words to exclude from the results
	 */
	public void setExcludedWords(Path excludedWordsFile) {
		try {
			setExcludedWords(Files.readAllLines(excludedWordsFile));
		} catch (IOException e) {
			System.out.println("Cannot read Excluded Words file " + excludedWordsFile);
		}
	}

	@Override
	public ExcerptScorerCriterionResult score(Excerpt excerpt, String searchString) {
		if ( ! excludedWords.phraseMatch(searchString) && excludedWords.phraseMatch(excerpt.toString())) {
			return new ExcerptScorerCriterionResult(penalty, strict ? true : false);
		}

		return new ExcerptScorerCriterionResult(0, false);
	}

}

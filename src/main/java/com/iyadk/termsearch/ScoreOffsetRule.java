package com.iyadk.termsearch;

import java.util.regex.Pattern;

/**
 * Class that encapsulates a rule that determines whether a document's score is augmented or demoted by a certain values  
 * @author Iyad Kandalaft
 *
 */
public class ScoreOffsetRule {
	enum SearchTypeEnum {
		REGEX,
		EXACT
	}
	
	public SearchTypeEnum searchType;
	private Pattern lookupPattern;
	private String lookupText;
	public double scoreAdjustment;
	public boolean stopAfter;
	
	/**
	 * 
	 * @param searchType Type of search to apply: regex vs exact matching
	 * @param lookup The regex or exact text to match
	 * @param scoreAdjustment The score adjustment value 
	 * @param stopAfter	Determines whether this is the last rule to apply
	 */
	public ScoreOffsetRule(SearchTypeEnum searchType, String lookup, double scoreAdjustment, boolean stopAfter ) {
		this.searchType = searchType;
		if ( searchType == SearchTypeEnum.EXACT ) {
			this.lookupText = lookup;
		} else {
			this.lookupPattern = Pattern.compile(lookup);
		}
		this.scoreAdjustment = scoreAdjustment;
		this.stopAfter = stopAfter;
	}
	
	/**
	 * Determine if the input text matches the lookup pattern or contains the lookup string
	 * @param text
	 * @return True if the provided text matches
	 */
	public boolean applies(String text) {
		if ( searchType == SearchTypeEnum.EXACT ) {
			return text.indexOf(lookupText) > -1;
		}
		return lookupPattern.matcher(text).find();
	}
}

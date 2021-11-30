package com.iyadk.termsearch;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class FormattingRule {
	private Pattern matchPattern;
	private String replacement;
	private boolean stopAfter;
	
	public FormattingRule(String matchPattern, String replacement, boolean stopAfter) throws PatternSyntaxException {
		setMatchPattern(matchPattern);
		setReplacement(replacement);
		setStopAfter(stopAfter);
	}
	
	public FormattingRule(String matchPattern, String replacement) throws PatternSyntaxException {
		this(matchPattern, replacement, false);
	}
	
	public FormattingRule(String matchPattern, boolean stopAfter) {
		this(matchPattern, "", stopAfter);
	}
	
	public Pattern getMatchPattern() {
		return matchPattern;
	}

	public void setMatchPattern(String matchPattern) throws PatternSyntaxException {
		setMatchPattern(Pattern.compile(matchPattern));
	}
	
	public void setMatchPattern(Pattern matchPattern) {
		this.matchPattern = matchPattern;
	}

	public String getReplacement() {
		return replacement;
	}

	public void setReplacement(String replacement) {
		this.replacement = replacement;
	}

	public boolean stopAfter() {
		return stopAfter;
	}

	public void setStopAfter(boolean stopAfter) {
		this.stopAfter = stopAfter;
	}
	
	public String apply(CharSequence input){
		return matchPattern.matcher(input).replaceAll(replacement);
	}
}

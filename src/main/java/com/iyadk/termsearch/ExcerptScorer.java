package com.iyadk.termsearch;

public class ExcerptScorer {
	private static final String punctuation = ".,:;!?'\"";
	private int upperCasePenalty;
	private int punctuationPenalty;
	private int digitPenalty;
	
	public ExcerptScorer() {
		setUpperCasePenalty(1);
		setPunctuationPenalty(1);
		setDigitPenalty(1);
	}

	public int getUpperCasePenalty() {
		return upperCasePenalty;
	}

	public void setUpperCasePenalty(int upperCasePenalty) {
		this.upperCasePenalty = upperCasePenalty;
	}

	public int getPunctuationPenalty() {
		return punctuationPenalty;
	}

	public void setPunctuationPenalty(int punctuationPenalty) {
		this.punctuationPenalty = punctuationPenalty;
	}

	public int getDigitPenalty() {
		return digitPenalty;
	}

	public void setDigitPenalty(int digitPenalty) {
		this.digitPenalty = digitPenalty;
	}

	public int score(Excerpt excerpt) {
		int score = 1;

		for (int i = 0; i < excerpt.length(); i++){
		    char currChar = excerpt.charAt(i);
		    if (Character.isUpperCase(currChar)) {
		    	score += upperCasePenalty;
		    	setUpperCasePenalty(upperCasePenalty * 2);
		    } else if(punctuation.contains(String.valueOf(currChar))) {
		    	score += punctuationPenalty;
		    	setPunctuationPenalty(punctuationPenalty * 2);
		    } else if(Character.isDigit(currChar)) {
		    	score += digitPenalty;
		    	setDigitPenalty(digitPenalty * 2);
		    }
		}
		
		excerpt.setScore(score);
		
		return score;
	}

}

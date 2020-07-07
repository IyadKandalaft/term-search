package com.iyadk.termsearch;

import java.util.ArrayList;

public class ExcerptScorer {
	ArrayList<ExcerptFullScorerCriterionI> fullCriteria;
	ArrayList<ExcerptCharScorerCriterionI> charCriteria;

	public ExcerptScorer() {
		fullCriteria = new ArrayList<>();
		charCriteria = new ArrayList<>();
	}

	public ExcerptFullScorerCriterionI addFullScoringCriteria(ExcerptFullScorerCriterionI criterion) {
		fullCriteria.add(criterion);
		return criterion;
	}

	public ArrayList<ExcerptFullScorerCriterionI> getFullScoringCriteria(){
		return fullCriteria;
	}

	public ExcerptCharScorerCriterionI addCharScoringCriteria(ExcerptCharScorerCriterionI criterion) {
		charCriteria.add(criterion);
		return criterion;
	}

	public ArrayList<ExcerptCharScorerCriterionI> getCharScoringCriteria(){
		return charCriteria;
	}

	public boolean score(Excerpt excerpt, String searchStrnig) {
		int score = 1;

		for (ExcerptFullScorerCriterionI criterion : fullCriteria) {
			final ExcerptScorerCriterionResult criterionResult = criterion.score(excerpt, searchStrnig);
			if (criterionResult.skip)
				return false;
			score += criterionResult.score;
			if (criterion.isLast() && criterionResult.score > 0)
				break;
		} 
		
		for (int i = 0; i < excerpt.length(); i++){
			char currChar = excerpt.charAt(i);
			for (ExcerptCharScorerCriterionI criterion : charCriteria) {
				int delta = criterion.score(currChar);
				score += delta;
				if (delta > 0 && criterion.isLast()) {
					break;
				}
			}
		}

		excerpt.setScore(score);

		return true;
	}

}

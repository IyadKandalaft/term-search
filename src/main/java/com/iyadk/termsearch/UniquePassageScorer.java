package com.iyadk.termsearch;

import org.apache.lucene.search.uhighlight.Passage;
import org.apache.lucene.search.uhighlight.PassageScorer;

/**
 * Null passage scorer that returns the same score for every passage
 * This exists for performance reasons.
 * 
 * @author Iyad Kandalaft
 *
 */
public class UniquePassageScorer extends PassageScorer{
	public UniquePassageScorer() {
	}
	
	public float score(Passage passage, int contentLength) {
		return 1;
	}

}

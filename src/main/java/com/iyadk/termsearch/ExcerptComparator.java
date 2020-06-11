package com.iyadk.termsearch;

import java.util.Comparator;

/**
 * Compares excerpts based on score
 * 
 * @author Iyad Kandalaft
 */
public class ExcerptComparator implements Comparator<Excerpt> {
	@Override
	public int compare(Excerpt e1, Excerpt e2) {
		if (e1.getScore() > e2.getScore())
			return 1;
		else if (e1.getScore() < e2.getScore())
			return -1;
		
		return 0;
	}

}

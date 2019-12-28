package com.iyadk.termsearch;

import java.util.concurrent.ConcurrentHashMap;

public class SearchDocumentMatches {
	private static ConcurrentHashMap<Double, Double> docMatches = new ConcurrentHashMap<>();
	
	public static double getDocMatchCount(double docID) {
		if (docMatches.containsKey(docID))
			return docMatches.get(docID);
		else
			return 0;
	}
	
	public static ConcurrentHashMap<Double,Double> getAllMatches(){
		return docMatches;
	}
	
	public synchronized static double incrementDocMatchCount(double docID) {
		Double count = docMatches.put(docID, getDocMatchCount(docID) + 1);
		if (count == null)
			return 1;
		return count;
	}
}
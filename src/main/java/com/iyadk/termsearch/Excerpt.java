package com.iyadk.termsearch;

/**
 * Encapsulation class to hold document excerpts and an associated score
 * 
 * @author Iyad Kandalaft
 */
public class Excerpt {
	private String excerpt;
	private String documentTitle;
	private double docId;
	private int score;

	public Excerpt(String excerpt) {
		this.excerpt = excerpt;
		this.setScore(1);
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	public double getDocId() {
		return docId;
	}

	public void setDocId(double docId) {
		this.docId = docId;
	}

	public String getDocumentTitle() {
		return documentTitle;
	}

	public void setDocumentTitle(String documentTitle) {
		this.documentTitle = documentTitle;
	}

	public char charAt(int index) {
		return excerpt.charAt(index);
	}

	public int length() {
		return excerpt.length();
	}

	@Override
	public String toString() {
		return excerpt;
	}

	public String replaceAll(String regex, String replacement) {
		return excerpt.replaceAll(regex, replacement);
	}

}

package com.iyadk.termsearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Efficiently matches input text against a list of words or patterns to
 * determine if any of the words exists in the phrase.
 * 
 * @author Iyad Kandalaft
 */
public class MatchList {
	private ArrayList<Pattern> wordPatterns;
	private boolean useBoundaries;

	/**
	 * Instantiate a MatchList and search with word boundaries enabled
	 */
	public MatchList() {
		this.wordPatterns = new ArrayList<Pattern>();
		this.useBoundaries = true;
	}
	
	/**
	 * Instantiate a MatchList using a collection of words
	 * and search with word boundaries enabled
	 */
	public MatchList(Collection<String> wordList) {
		this.wordPatterns = new ArrayList<Pattern>();
		this.useBoundaries = true;
		addAll(wordList);
	}
	
	/**
	 * Instantiate a MatchList and set whether word boundaries are enabled
	 * @param useBoundaries Sets whether searching uses word boundaries
	 */
	public MatchList(boolean useBoundaries){
		this.wordPatterns = new ArrayList<Pattern>();
		this.useBoundaries = useBoundaries;
	}

	public boolean isUseBoundaries() {
		return useBoundaries;
	}

	/**
	 * Toggles using word boundaries during search
	 * @param useBoundaries Set to true to search using \bWORD OR PATTERN\b
	 */
	public void setUseBoundaries(boolean useBoundaries) {
		this.useBoundaries = useBoundaries;
	}

	/**
	 * Adds a new word to the search list
	 * 
	 * @param newWord
	 * @return true if the addition was successful
	 */
	public boolean add(String newWord) {
		return add(newWord, this.useBoundaries);
	}

	/**
	 * Add a word to the search list but override whether the search uses word
	 * boundaries.
	 * 
	 * @param newWord       The word to add
	 * @param useBoundaries Set to true to search using word boundaries
	 * @return true if the word was successfully added or false otherwise
	 */
	public boolean add(String newWord, boolean useBoundaries) {
		if (useBoundaries) {
			return wordPatterns.add(Pattern.compile("\\b" + newWord.toLowerCase() + "\\b"));
		}
		return wordPatterns.add(Pattern.compile(newWord.toLowerCase()));
	}

	/**
	 * Add a collection of words to the list
	 * 
	 * @param newWords
	 * @return true if the addition of words was successful or false otherwise
	 */
	public boolean addAll(Collection<String> newWords) {
		for (String word : newWords) {
			if (!add(word))
				return false;
		}
		return true;
	}

	/**
	 * Add a list of words to the existing
	 * 
	 * @param newWords
	 * @return true if the addition was successful or false otherwise
	 */
	public boolean addAll(String[] newWords) {
		for (String word : newWords) {
			if (!add(word))
				return false;
		}
		return true;
	}

	/**
	 * @return the number of words in the list to be matched against
	 */
	public int count() {
		return wordPatterns.size();
	}

	/**
	 * Determine if any of the words in the phrase match any of the words in the
	 * list
	 * 
	 * @param phrase A string that will be scanned for the words in the list
	 * @return true if a match was found or false otherwise
	 */
	public boolean phraseMatch(String phrase) {
		for (Pattern wordPattern : wordPatterns) {
			if (wordPattern.matcher(phrase).find())
				return true;
		}
		return false;
	}

}

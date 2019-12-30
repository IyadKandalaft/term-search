package com.iyadk.termsearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.EnglishPossessiveFilterFactory;
import org.apache.lucene.analysis.pattern.PatternReplaceCharFilterFactory;
import org.apache.lucene.analysis.standard.ClassicTokenizerFactory;

public class UniqueAnalyzer{
	private static UniqueAnalyzer instance;
	
	public Analyzer analyzer;
	

	private UniqueAnalyzer() throws IOException {
		// Abbreviations - exclude trailing period
		ArrayList<String> abbreviations = new ArrayList<>(Arrays.asList(
				"Dr", "Prof", "Gen", "Rep", "Sen", "St", "Hon",
				"Mr", "Mrs", "Ms", "Mme", "Messrs", 
				"Sr", "Jr",
				"Ph.D", "M.D", "B.A", "M.A", "D.D.S",
				"U.S", "U.S.A", "e.g", "i.e", "etc", "et al", "ca", "O.K",
				"A.M", "P.M","A.D", "B.C"));
		
		// Escape all "." with "\\" to prepare it for the regex 
		abbreviations.replaceAll(s -> s.replace(".", "\\."));
		// Pattern matching sentence-ending punctuation but ignores periods after common abbrev: "Mr. Author"
		String regexPattern = "(\"|!|\\?|(?<!" + String.join("|", abbreviations) + ")\\.|;|:)(\\s*)[A-Z](\\w+)[^\"']";

		HashMap<String, String> firstWordRegExpFilter = new HashMap<String,String>();
		// RegExp to remove the first word of every sentence
		//firstWordRegExpFilter.put("pattern", "([\"!.;:])(\\s*)[A-Z](\\w+)[^\"']");
		//firstWordRegExpFilter.put("replacement", "$1$2*$3");
		firstWordRegExpFilter.put("pattern", regexPattern);
		firstWordRegExpFilter.put("replacement", "$1$2*$3");

		analyzer = CustomAnalyzer.builder()
		.addCharFilter(PatternReplaceCharFilterFactory.class, firstWordRegExpFilter)
		.withTokenizer(ClassicTokenizerFactory.class)
		.addTokenFilter(EnglishPossessiveFilterFactory.class)
		.addTokenFilter(FirstWordFilterFactory.class)
		.build();
	}
	
	public static UniqueAnalyzer getInstance() throws IOException {
		if (null == instance) {
			instance = new UniqueAnalyzer();
		}
		return instance;
	}
}
package com.iyadk.termsearch;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.EnglishPossessiveFilterFactory;
import org.apache.lucene.analysis.pattern.PatternReplaceCharFilterFactory;
import org.apache.lucene.analysis.standard.ClassicTokenizerFactory;

public class UniqueAnalyzer{
	private static UniqueAnalyzer instance;
	
	public Analyzer analyzer;
	
	private UniqueAnalyzer() throws IOException {
		Map<String, String> firstWordRegExpFilter = new HashMap<String,String>();
		// RegExp to remove the first word of every sentence
		firstWordRegExpFilter.put("pattern", "([\"!.;:]\\s*)[A-Z]\\w+[^\"']");
		firstWordRegExpFilter.put("replacement", "$1");

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
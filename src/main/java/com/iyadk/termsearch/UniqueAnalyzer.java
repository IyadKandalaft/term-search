package com.iyadk.termsearch;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.EnglishPossessiveFilterFactory;
import org.apache.lucene.analysis.standard.ClassicTokenizerFactory;

public class UniqueAnalyzer{
	private static UniqueAnalyzer instance;
	
	public Analyzer analyzer;
	
	private UniqueAnalyzer() throws IOException {
		analyzer = CustomAnalyzer.builder()
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
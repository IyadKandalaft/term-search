package com.iyadk.termsearch;

import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

public class FirstWordFilterFactory extends TokenFilterFactory {

	public FirstWordFilterFactory(Map<String, String> args) {
		super(args);
	}

	@Override
	public TokenStream create(TokenStream ts) {
		return new FirstWordFilter(ts);
	}
}

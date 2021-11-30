package com.iyadk.termsearch;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/*
 * Filters out the first word of every sentence in the document
 */
public final class FirstWordFilter extends TokenFilter {
	private CharTermAttribute charTermAttr;
	private boolean firstWord = true;

	protected FirstWordFilter(TokenStream tokenStream) {
		super(tokenStream);
		this.charTermAttr = addAttribute(CharTermAttribute.class);
	}

	@Override
	public boolean incrementToken() throws IOException {
		if (!input.incrementToken()) {
			return false;
		}
		if (!firstWord) {
			return true;
		}

		charTermAttr.setEmpty();
		charTermAttr.append("");
		firstWord = false;

		return true;
	}

	@Override
	public void reset() throws IOException {
		super.reset();
		firstWord = true;
	}
}

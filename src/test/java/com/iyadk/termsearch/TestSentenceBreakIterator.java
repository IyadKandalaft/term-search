package com.iyadk.termsearch;

import java.text.BreakIterator;
import java.util.Locale;

public class TestSentenceBreakIterator {

	public TestSentenceBreakIterator() {
		// TODO Auto-generated constructor stub
	}
	
	public static void main(String[] args) {
		NaturalBreakIterator nbi = new NaturalBreakIterator(11, 50);
		String mytext = "This is the first sentence.  This is the second sentence;  This is the third sentence.  This is the Fourth sentence!  This is the fifth sentence: this is the sixth sentence.  He said: \"This is the seventh sentence!\"  And this is eigth.";
		nbi.setText(mytext);
				
		int last = nbi.first();
		int current;
		while ( (current = nbi.following(last)) != -1 ) {
			System.out.println(mytext.substring(last, current));
			last = current;
		}
		
		System.out.println(mytext.substring(nbi.preceding(20), nbi.following(20)));
	}

}

package com.iyadk.termsearch;

import java.text.BreakIterator;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Locale;

/**
 * Wraps another BreakIterator to skip past breaks that would result in passages that are too
 * short or too long. 
 */
public class NaturalBreakIterator extends BreakIterator {
	private final BreakIterator baseIterator;
	private final int minLength;
	private final int maxLength;
	private final int queryOffset;
	private int current;
	private CharacterIterator text;

	public NaturalBreakIterator(int minLength, int maxLength, int offset) {
		this.baseIterator = BreakIterator.getSentenceInstance(Locale.ENGLISH);;
		this.queryOffset = offset;
		this.minLength = minLength;
		this.maxLength = maxLength;
		this.current = baseIterator.current();
		
		assert maxLength > minLength;
	}
	
	public NaturalBreakIterator(int minLength, int maxLength) {
		this(minLength, maxLength, 0);
	}
	
	@Override
	public Object clone() {
		final NaturalBreakIterator clone = new NaturalBreakIterator(minLength, maxLength);
		clone.setText(text);
		return clone;
	}

	@Override
	public CharacterIterator getText() {
		return text;
	}

	@Override
	public void setText(String newText) {		
		baseIterator.setText(newText);
		text = baseIterator.getText();
		current = baseIterator.current();
	}

	@Override
	public void setText(CharacterIterator newText) {
		baseIterator.setText(newText);
		text = baseIterator.getText();
		current = baseIterator.current();
	}

	@Override
	public int current() {
		return current;
	}

	@Override
	public int first() {
		return current = baseIterator.first();
	}

	@Override
	public int last() {
		return current;
	}

	@Override
	public int next(int n) {
		assert false : "Not supported";
		return baseIterator.next(n); // probably wrong
	}

	// called by getSummaryPassagesNoHighlight to generate default summary.
	@Override
	public int next() {
		current = following(current());
		return current;
	}

	@Override
	public int previous() {
		assert false : "Not supported";
		return baseIterator.previous();
	}

	@Override
	public int following(int offset) {
	    final int minBreakIndex = Math.min(current + minLength + queryOffset, text.getEndIndex());
	    // final int maxBreakIndex = Math.min(current + maxLength + queryOffset + 1, text.getEndIndex());
	    final int maxBreakIndex = Math.min(current + maxLength + 1, text.getEndIndex());
	    //final int queryEndIndex = offset + queryOffset;

	    assert offset < maxBreakIndex;

	    // If the baseIterator returns an index less than our minimum length
	    // we need to get its next index
	    int baseIterIndex = baseIterator.following(offset);
	    int prevBaseIterIndex = baseIterIndex;
	    while(baseIterIndex < minBreakIndex && baseIterIndex != DONE) {
	    	prevBaseIterIndex = baseIterIndex;
	    	baseIterIndex = baseIterator.following(baseIterIndex);
	    }

	    // Returns if the minimum index criteria wasn't met but the
	    // baseIterator has no more breaks.
	    if ( baseIterIndex == DONE ) {
	    	current = baseIterator.last();
	    	return DONE;
	    }

	    // If the baseIterator returns an index less than our maximum length
	    // then the next break is good.
	    if (baseIterIndex <= maxBreakIndex)
	    	return current = baseIterIndex;

	    return current = prevBaseIterIndex;
	}

	// called at start of new Passage given first word start offset
	@Override
	public int preceding(int offset) {
    	final int minBreakIndex = Math.max(offset - maxLength + queryOffset + 1, 0);
	    //final int maxBreakIndex = Math.max(offset - minLength + queryOffset + 1, 0);

		int baseIterIndex = offset;
		while (true) {
			baseIterIndex = baseIterator.preceding(baseIterIndex);

			// If we are at the beginning of the text, we're done
			if (baseIterIndex == DONE) {
				current = baseIterator.last();
				return DONE;
			}

			// The baseIterator went past the minimum break index - that's not good
			if (baseIterIndex < minBreakIndex)
				return current = baseIterIndex;
				//break;

			// Did we go far enough back to ensure that the minimum length
			// can be met when this.following(offset) is called?
			if (baseIterIndex + minLength > text.getEndIndex())
				continue;

			char currentChar = text.setIndex(baseIterIndex);

			// If the first letter is lower case, find another starting point
			if (Character.isLowerCase(currentChar))
				continue;

			// If the first letter is a period, we might have stopped at an abbrev.
			if (currentChar == '.') {
				char nextChar = text.next();
				
				// If the next char is a comma, then it's definitely an abbreviation
				if (nextChar == ',')
					continue;

				// If the next char is not a space, the break iterator stopped at a special character
				if (nextChar != ' ')
					return current = baseIterIndex + 2;

				char prevChar = text.setIndex(baseIterIndex - 1);

				// If the previous character is an uppercase then it's probably an abbreviation
				if (Character.isUpperCase(prevChar))
					continue;
			}

			// If the first character is a comma, and previous characters are punctuation,
			// then it's a quote and we need to go back further
			if (currentChar == ',') {
				char prevChar = text.previous();
				if ("!?\"')".indexOf(prevChar) > -1)
					continue;
				//if (prevChar == '!' || prevChar == '"' || prevChar == '\'' || prevChar == '?' || prevChar == ')')
				//	continue;
			}

			return current = baseIterIndex;
		}
	}

	@Override
	public boolean isBoundary(int offset) {
		assert false : "Not supported";
		return baseIterator.isBoundary(offset);
	}
}

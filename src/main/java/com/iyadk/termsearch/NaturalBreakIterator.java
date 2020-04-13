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
	    final int queryEndIndex = offset + queryOffset;

	    assert offset < maxBreakIndex;

	    // If the baseIterator returns an index less than our minimum length
	    // we need to get its next index
	    int baseIterIndex = baseIterator.following(offset);
	    while(baseIterIndex < minBreakIndex && baseIterIndex != DONE) {
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
	    
	    // If the baseIterator returns an index that's larger than our maximum length
	    // we need to find an acceptable break before our maximum length
	    int searchIndex = maxBreakIndex;
    	text.setIndex(searchIndex);
	    	
    	// Look for the last comma, colon, or semi-colon starting with the maximum length
    	// This is a preferred break since it's within our target length range
    	char currChar = text.previous();
    	int lastSpaceIdx = -1;

    	while(searchIndex > minBreakIndex && searchIndex > queryEndIndex && currChar != ',' && currChar != ':' && currChar != ';') {
    		// Keep track of the last word boundary
    		if (currChar == ' ')
    			lastSpaceIdx = searchIndex;
    		currChar = text.previous();
    		searchIndex = text.getIndex();
    	}
	    
    	if (searchIndex > minBreakIndex && searchIndex != queryEndIndex)
    		return current = searchIndex + 1;
    	
    	// We got all the way back to the minimum within our range
    	// break on the index of the last space
	    if (lastSpaceIdx != -1) {
	    	text.setIndex(lastSpaceIdx);
	    	return current = lastSpaceIdx + 1;
	    }
	    
	    // We didn't find a word boundary, comma, semi-colon, or colon
	    // Return the maximum within our range
	    text.setIndex(maxBreakIndex);
	    return current = maxBreakIndex;
	}

	// called at start of new Passage given first word start offset
	@Override
	public int preceding(int offset) {
    	final int minBreakIndex = Math.max(offset - maxLength + queryOffset + 1, 0);
	    final int maxBreakIndex = Math.max(offset - minLength + queryOffset + 1, 0);

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
				break;

			// Did we go far enough back to ensure that the minimum length
			// can be met when this.following(offset) is called?
			if (baseIterIndex + minLength > text.getEndIndex())
				continue;

			// Did base iterator return a break that's too close to the offset?
			//if (offset - baseIterIndex < 3)
			//	continue;

			return current = baseIterIndex;
		}

		// If the baseIterator's break distance from the offset is larger than the max length
		// then the break is too far and we have to find a closer one
    	int searchIndex = offset - queryOffset * 2;
    	text.setIndex(searchIndex);
	    	
    	// Look for a break character [, : ; ] starting within the maximum length
    	// This is a preferred natural break since it's within our target length range
    	char currChar;
    	int lastSpaceIndex = -1;
		while (true) {
			currChar = text.previous();
			searchIndex = text.getIndex();
			// Keep track of the last word boundary
			if (currChar == ' ')
				lastSpaceIndex = searchIndex + 1;

			if (searchIndex < minBreakIndex)
				break;

			if (currChar == ',' || currChar == ':' || currChar == ';') {
				// The current searchIndex is at a boundary that is too close to end of the text
				if (text.getEndIndex() - searchIndex < minLength)
					continue;
				break;
			}
		}

    	// A break character was found within our range
    	if (searchIndex > minBreakIndex) {
    		text.setIndex(searchIndex + 1);
    		return current = searchIndex + 1;  // +1 to ignore the comma/colon char
    	}

    	// If we detected a space within our range and we got all the way back to the minimum within our range
    	// break on the index of the space
	    if (lastSpaceIndex != -1) {
	    	text.setIndex(lastSpaceIndex);
	    	return current = lastSpaceIndex;
	    }
	    
	    // We didn't find a word boundary, comma, semi-colon, or colon
	    // Return the maximum within our range
	    text.setIndex(maxBreakIndex);
	    return current = maxBreakIndex;
	}

	@Override
	public boolean isBoundary(int offset) {
		assert false : "Not supported";
		return baseIterator.isBoundary(offset);
	}
}

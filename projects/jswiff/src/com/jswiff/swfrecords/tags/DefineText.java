/*
 * JSwiff is an open source Java API for Macromedia Flash file generation
 * and manipulation
 *
 * Copyright (C) 2004-2005 Ralf Terdic (contact@jswiff.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.jswiff.swfrecords.tags;

import com.jswiff.io.InputBitStream;
import com.jswiff.io.OutputBitStream;
import com.jswiff.swfrecords.GlyphEntry;
import com.jswiff.swfrecords.Matrix;
import com.jswiff.swfrecords.Rect;
import com.jswiff.swfrecords.TextRecord;

import java.io.IOException;

import java.util.Vector;


/**
 * This tag defines a block of static text. The bounding box of the text as
 * well as a transform matrix can be specified. The actual text characters
 * their style and position is defined in an array of <code>TextRecord</code>
 * instances.
 *
 * @see TextRecord
 * @see DefineText2
 * @since SWF 1
 */
public final class DefineText extends DefinitionTag {
	private Rect textBounds;
	private Matrix textMatrix;
	private TextRecord[] textRecords;

	/**
	 * Creates a new DefineText tag. Supply text character ID, bounding box and
	 * transform matrix. Specify text characters, their style and position in
	 * an <code>TextRecord</code> array.
	 *
	 * @param characterId text character ID
	 * @param textBounds bounding box of text
	 * @param textMatrix transform matrix for text
	 * @param textRecords <code>TextRecord</code> array containing text
	 * 		  characters
	 */
	public DefineText(
		int characterId, Rect textBounds, Matrix textMatrix,
		TextRecord[] textRecords) {
		code				 = TagConstants.DEFINE_TEXT;
		this.characterId     = characterId;
		this.textBounds		 = textBounds;
		this.textMatrix		 = textMatrix;
		this.textRecords     = textRecords;
	}

	DefineText() {
		// empty
	}

	/**
	 * Sets the bounding box of the text, i.e. the smallest rectangle enclosing
	 * the text.
	 *
	 * @param textBounds text bounds
	 */
	public void setTextBounds(Rect textBounds) {
		this.textBounds = textBounds;
	}

	/**
	 * Returns the bounding box of the text, i.e. the smallest rectangle
	 * enclosing the text.
	 *
	 * @return text bounds
	 */
	public Rect getTextBounds() {
		return textBounds;
	}

	/**
	 * Sets the transform matrix of the text.
	 *
	 * @param textMatrix text transform matrix
	 */
	public void setTextMatrix(Matrix textMatrix) {
		this.textMatrix = textMatrix;
	}

	/**
	 * Returns the transform matrix of the text.
	 *
	 * @return text transform matrix
	 */
	public Matrix getTextMatrix() {
		return textMatrix;
	}

	/**
	 * Sets the array of <code>TextRecords</code> containing the text
	 * characters and their style and position.
	 *
	 * @param textRecords text record array
	 */
	public void setTextRecords(TextRecord[] textRecords) {
		this.textRecords = textRecords;
	}

	/**
	 * Returns the array of <code>TextRecords</code> containing the text
	 * characters and their style and position.
	 *
	 * @return text record array
	 */
	public TextRecord[] getTextRecords() {
		return textRecords;
	}

	protected void writeData(OutputBitStream outStream)
		throws IOException {
		forceLongHeader = true;
		outStream.writeUI16(characterId);
		textBounds.write(outStream);
		textMatrix.write(outStream);
		// compute glyphBits and advanceBits
		int glyphBits   = 0;
		int advanceBits = 0;
		for (int i = 0; i < textRecords.length; i++) {
			GlyphEntry[] glyphs = textRecords[i].getGlyphEntries();
			for (int j = 0; j < glyphs.length; j++) {
				glyphBits	    = Math.max(
						glyphBits,
						OutputBitStream.getUnsignedBitsLength(
							glyphs[j].getGlyphIndex()));
				advanceBits     = Math.max(
						advanceBits,
						OutputBitStream.getSignedBitsLength(
							glyphs[j].getGlyphAdvance()));
			}
		}
		outStream.writeUI8((short) glyphBits);
		outStream.writeUI8((short) advanceBits);
		for (int i = 0; i < textRecords.length; i++) {
			textRecords[i].write(
				outStream, (short) glyphBits, (short) advanceBits);
		}
		outStream.writeUI8((short) 0);
	}

	void setData(byte[] data) throws IOException {
		InputBitStream inStream = new InputBitStream(data);
		characterId     = inStream.readUI16();
		textBounds	    = new Rect(inStream);
		textMatrix	    = new Matrix(inStream);
		short glyphBits = inStream.readUI8();
		short advanceBits = inStream.readUI8();
		Vector records  = new Vector();
		do {
			if (data[(int) inStream.getOffset()] == 0) {
				break;
			}
			TextRecord record = new TextRecord(
					inStream, glyphBits, advanceBits, false);
			records.add(record);
		} while (true);
		textRecords = new TextRecord[records.size()];
		records.copyInto(textRecords);
	}
}

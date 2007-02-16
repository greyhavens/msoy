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
import com.jswiff.swfrecords.Shape;

import java.io.IOException;


/**
 * <p>
 * This tag defines the shape outlines of each glyph used in a particular font.
 * Only glyphs used by <code>DefineText</code> tags need to be defined.
 * </p>
 * 
 * <p>
 * Warning: for dynamic text, you have to use the <code>DefineFont2</code> tag.
 * </p>
 *
 * @see Shape
 * @since SWF 1
 */
public final class DefineFont extends DefinitionTag {
	private Shape[] glyphShapeTable;

	/**
	 * Creates a new DefineFont tag.
	 *
	 * @param characterId the character ID of the font
	 * @param glyphShapeTable array of <code>Shape</code> instances
	 */
	public DefineFont(int characterId, Shape[] glyphShapeTable) {
		code					 = TagConstants.DEFINE_FONT;
		this.characterId		 = characterId;
		this.glyphShapeTable     = glyphShapeTable;
	}

	DefineFont() {
		// empty
	}

	/**
	 * Sets an array of <code>Shape</code> instances used to define character
	 * glyphs.
	 *
	 * @param glyphShapeTable array of <code>Shape</code> instances
	 */
	public void setGlyphShapeTable(Shape[] glyphShapeTable) {
		this.glyphShapeTable = glyphShapeTable;
	}

	/**
	 * Returns an array of <code>Shape</code> instances used to define
	 * character glyphs.
	 *
	 * @return array of <code>Shape</code> instances
	 */
	public Shape[] getGlyphShapeTable() {
		return glyphShapeTable;
	}

	protected void writeData(OutputBitStream outStream)
		throws IOException {
		outStream.writeUI16(characterId);
		int shapeTableOffset = glyphShapeTable.length * 2; // 2 bytes * table length
		outStream.writeUI16(shapeTableOffset); // first entry of offsetTable
		OutputBitStream glyphShapeTableStream = new OutputBitStream();
		glyphShapeTable[0].write(glyphShapeTableStream); // write first shape
		for (int i = 1; i < glyphShapeTable.length; i++) {
			outStream.writeUI16(
				(int) (shapeTableOffset + glyphShapeTableStream.getOffset()));
			glyphShapeTable[i].write(glyphShapeTableStream);
		}
		outStream.writeBytes(glyphShapeTableStream.getData());
	}

	void setData(byte[] data) throws IOException {
		InputBitStream inStream = new InputBitStream(data);
		characterId = inStream.readUI16();
		int shapeTableOffset = inStream.readUI16();
		int tableSize		 = shapeTableOffset / 2;
		inStream.readBytes(shapeTableOffset - 2); // ignore rest of the offsetTable
		glyphShapeTable = new Shape[tableSize];
		for (int i = 0; i < tableSize; i++) {
			glyphShapeTable[i] = new Shape(inStream);
		}
	}
}

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
import com.jswiff.swfrecords.CXform;
import com.jswiff.swfrecords.Matrix;

import java.io.IOException;


/**
 * This tag adds a character instance to the display list. When a
 * <code>ShowFrame</code> tag is encountered, the instance is displayed at the
 * specified depth. A transform matrix affects the position, scale and
 * rotation of the character. A color effect can be applied by using an
 * (optional) color transform.
 *
 * @see PlaceObject2
 * @see ShowFrame
 * @since SWF 1
 */
public final class PlaceObject extends Tag {
	private int characterId;
	private int depth;
	private Matrix matrix;
	private CXform colorTransform;

	/**
	 * Creates a new PlaceObject tag.
	 *
	 * @param characterId ID of the character to be placed
	 * @param depth placement depth
	 * @param matrix transform matrix (for translation, scaling, rotation etc.)
	 * @param colorTransform color transform for color effects, optional (use
	 * 		  <code>null</code> if not needed)
	 */
	public PlaceObject(
		int characterId, int depth, Matrix matrix, CXform colorTransform) {
		code				    = TagConstants.PLACE_OBJECT;
		this.characterId	    = characterId;
		this.depth			    = depth;
		this.matrix			    = matrix;
		this.colorTransform     = colorTransform;
	}

	PlaceObject() {
		// empty
	}

	/**
	 * Sets the ID of the character to be placed to the display list.
	 *
	 * @param characterId ID of character to be placed
	 */
	public void setCharacterId(int characterId) {
		this.characterId = characterId;
	}

	/**
	 * Returns the ID of the character to be placed to the display list.
	 *
	 * @return character ID
	 */
	public int getCharacterId() {
		return characterId;
	}

	/**
	 * Returns the (optional) color transform.
	 *
	 * @return color transform (<code>null</code> if not specified)
	 */
	public CXform getColorTransform() {
		return colorTransform;
	}

	/**
	 * Sets the depth the character will be placed at.
	 *
	 * @param depth display depth
	 */
	public void setDepth(int depth) {
		this.depth = depth;
	}

	/**
	 * Returns the depth (i.e. the stacking order) the character instance is
	 * supposed to be placed at.
	 *
	 * @return display depth
	 */
	public int getDepth() {
		return depth;
	}

	/**
	 * Returns the transform matrix used for affine transforms like
	 * translation, scaling, rotation, shearing etc.
	 *
	 * @return transform matrix
	 */
	public Matrix getMatrix() {
		return matrix;
	}

	protected void writeData(OutputBitStream outStream)
		throws IOException {
		outStream.writeUI16(characterId);
		outStream.writeUI16(depth);
		matrix.write(outStream);
		if (colorTransform != null) {
			colorTransform.write(outStream);
		}
	}

	void setData(byte[] data) throws IOException {
		InputBitStream inStream = new InputBitStream(data);
		characterId     = inStream.readUI16();
		depth		    = inStream.readUI16();
		matrix		    = new Matrix(inStream);
		try {
			// optional
			colorTransform = new CXform(inStream);
		} catch (IOException e) {
			// nothing to do, cxform missing
		}
	}
}

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

import java.io.IOException;


/**
 * Specifies a color transform for a button defined with the
 * <code>DefineButton</code> tag.
 *
 * @since SWF 2
 */
public final class DefineButtonCXform extends DefinitionTag {
	private CXform colorTransform;

	/**
	 * Creates a new DefineButtonCXform instance.
	 *
	 * @param characterId character ID of the button
	 * @param colorTransform color transform
	 */
	public DefineButtonCXform(int characterId, CXform colorTransform) {
		code				    = TagConstants.DEFINE_BUTTON_C_XFORM;
		this.characterId	    = characterId;
		this.colorTransform     = colorTransform;
	}

	DefineButtonCXform() {
		// empty
	}

	/**
	 * Returns the color transform.
	 *
	 * @return color transform.
	 */
	public CXform getColorTransform() {
		return colorTransform;
	}

	protected void writeData(OutputBitStream outStream)
		throws IOException {
		outStream.writeUI16(characterId);
		colorTransform.write(outStream);
	}

	void setData(byte[] data) throws IOException {
		InputBitStream inStream = new InputBitStream(data);
		characterId		   = inStream.readUI16();
		colorTransform     = new CXform(inStream);
	}
}

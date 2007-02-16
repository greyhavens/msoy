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

import java.io.IOException;


/**
 * Defines a bitmap character with JPEG compression. It contains only the JPEG
 * compressed image data. The JPEG encoding data is contained in a
 * <code>JPEGTables</code> tag.
 *
 * @since SWF 1
 */
public final class DefineBits extends DefinitionTag {
	private byte[] jpegData;

	/**
	 * Creates a new DefineBits tag.
	 *
	 * @param characterId character ID of the bitmap
	 * @param jpegData image data
	 */
	public DefineBits(int characterId, byte[] jpegData) {
		code				 = TagConstants.DEFINE_BITS;
		this.characterId     = characterId;
		this.jpegData		 = jpegData;
	}

	DefineBits() {
		// nothing to do
	}

	/**
	 * Sets the byte array containing the image data.
	 *
	 * @param jpegData image data
	 */
	public void setJpegData(byte[] jpegData) {
		this.jpegData = jpegData;
	}

	/**
	 * Returns the image data as byte array.
	 *
	 * @return bitmap image data
	 */
	public byte[] getJpegData() {
		return jpegData;
	}

	protected void writeData(OutputBitStream outStream)
		throws IOException {
		outStream.writeUI16(characterId);
		outStream.writeBytes(jpegData);
	}

	void setData(byte[] data) throws IOException {
		InputBitStream inStream = new InputBitStream(data);
		characterId     = inStream.readUI16();
		jpegData	    = new byte[data.length - 2];
		System.arraycopy(data, 2, jpegData, 0, jpegData.length);
	}
}

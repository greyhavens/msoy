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
 * Defines a bitmap character with JPEG compression. It contains both the JPEG
 * encoding table and the JPEG image data, allowing multiple JPEG images with
 * differing encoding tables to be defined within a SWF file
 *
 * @since SWF 2
 */
public final class DefineBitsJPEG2 extends DefinitionTag {
	private byte[] jpegData;

	/**
	 * Creates a new DefineBitsJPEG2 tag.
	 *
	 * @param characterId character ID of the bitmap
	 * @param jpegData JPEG data (image and encoding)
	 */
	public DefineBitsJPEG2(int characterId, byte[] jpegData) {
		code				 = TagConstants.DEFINE_BITS_JPEG_2;
		this.characterId     = characterId;
		this.jpegData		 = jpegData;
	}

	DefineBitsJPEG2() {
		// empty
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
	 * Returns the JPEG data contained in the tag.
	 *
	 * @return JPEG data (image and encoding)
	 */
	public byte[] getJpegData() {
		return jpegData;
	}

	protected void writeData(OutputBitStream outStream)
		throws IOException {
		forceLongHeader = true;
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

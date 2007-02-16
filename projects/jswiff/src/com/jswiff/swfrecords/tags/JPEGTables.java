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

import com.jswiff.io.OutputBitStream;

import java.io.IOException;


/**
 * This tag contains the JPEG encoding table (the Tables/Misc segment) for all
 * JPEG images defined in the SWF file with the <code>DefineBits</code> tag.
 *
 * @since SWF 1
 */
public final class JPEGTables extends Tag {
	private byte[] jpegData;

	/**
	 * Creates a new JPEGTables instance.
	 *
	 * @param jpegData JPEG encoding data
	 */
	public JPEGTables(byte[] jpegData) {
		code			  = TagConstants.JPEG_TABLES;
		this.jpegData     = jpegData;
	}

	JPEGTables() {
		// nothing to do
	}

	/**
	 * Sets the contained JPEG encoding data.
	 *
	 * @param jpegData encoding data
	 */
	public void setJpegData(byte[] jpegData) {
		this.jpegData = jpegData;
	}

	/**
	 * Returns the contained JPEG encoding data.
	 *
	 * @return JPEG encoding data
	 */
	public byte[] getJpegData() {
		return jpegData;
	}

	protected void writeData(OutputBitStream outStream)
		throws IOException {
		outStream.writeBytes(jpegData);
	}

	void setData(byte[] data) {
		jpegData = data;
	}
}

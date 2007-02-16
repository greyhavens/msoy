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
import com.jswiff.swfrecords.AlphaBitmapData;
import com.jswiff.swfrecords.AlphaColorMapData;
import com.jswiff.swfrecords.ZlibBitmapData;

import java.io.IOException;


/**
 * <p>
 * Defines a lossless bitmap character that contains RGB and alpha channel
 * (transparency) data compressed with ZLIB. The following bitmap formats are
 * supported:
 * 
 * <ul>
 * <li>
 * <i>colormapped images</i>, which use a colormap of up to 256 RGBA values
 * accessed through an 8-bit index (<code>FORMAT_8_BIT_COLORMAPPED</code>)
 * </li>
 * <li>
 * <i>direct images</i> with 32 bit RGBA color representation
 * (<code>FORMAT_32_BIT_RGBA</code>)
 * </li>
 * </ul>
 * </p>
 *
 * @since SWF 3
 */
public final class DefineBitsLossless2 extends DefinitionTag {
	/** 8 bit colormapped image format */
	public static final short FORMAT_8_BIT_COLORMAPPED = 3;
	/** 32 bit direct image format */
	public static final short FORMAT_32_BIT_RGBA	   = 5;
	private short format;
	private int width;
	private int height;
	private ZlibBitmapData zlibBitmapData;

	/**
	 * Creates a new DefineBitsLossless2 instance.
	 *
	 * @param characterId the image's character ID
	 * @param format image format (use provided constants)
	 * @param width image width
	 * @param height image height
	 * @param zlibBitmapData image data (ZLIB compressed)
	 */
	public DefineBitsLossless2(
		int characterId, short format, int width, int height,
		ZlibBitmapData zlibBitmapData) {
		code				    = TagConstants.DEFINE_BITS_LOSSLESS_2;
		this.characterId	    = characterId;
		this.format			    = format;
		this.width			    = width;
		this.height			    = height;
		this.zlibBitmapData     = zlibBitmapData;
	}

	DefineBitsLossless2() {
		// empty
	}

	/**
	 * Sets the image format (<code>FORMAT_8_BIT_COLORMAPPED</code>,
	 * <code>FORMAT_15_BIT_RGB</code> or <code>FORMAT_24_BIT_RGB</code>)
	 *
	 * @param format image format
	 */
	public void setFormat(short format) {
		this.format = format;
	}

	/**
	 * Returns the image format (<code>FORMAT_8_BIT_COLORMAPPED</code> or
	 * <code>FORMAT_32_BIT_RGBA</code>)
	 *
	 * @return Returns the format.
	 */
	public short getFormat() {
		return format;
	}

	/**
	 * Sets the height of the image.
	 *
	 * @param height image height
	 */
	public void setHeight(int height) {
		this.height = height;
	}

	/**
	 * Returns the image height.
	 *
	 * @return image height
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * Sets the width of the image.
	 *
	 * @param width image width
	 */
	public void setWidth(int width) {
		this.width = width;
	}

	/**
	 * Returns the image width.
	 *
	 * @return image width
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * Sets the ZLIB-compressed image data.
	 *
	 * @param zlibBitmapData image data
	 */
	public void setZlibBitmapData(ZlibBitmapData zlibBitmapData) {
		this.zlibBitmapData = zlibBitmapData;
	}

	/**
	 * Returns the image data (ZLIB compressed)
	 *
	 * @return image data
	 */
	public ZlibBitmapData getZlibBitmapData() {
		return zlibBitmapData;
	}

	protected void writeData(OutputBitStream outStream)
		throws IOException {
		forceLongHeader = true;
		outStream.writeUI16(characterId);
		outStream.writeUI8(format);
		outStream.writeUI16(width);
		outStream.writeUI16(height);
		if (format == FORMAT_8_BIT_COLORMAPPED) {
			int colorTableSize = ((AlphaColorMapData) zlibBitmapData).getColorTableRGBA().length;
			outStream.writeUI8((short) (colorTableSize - 1));
		}
		OutputBitStream zStream = new OutputBitStream();
		zStream.enableCompression();
		zlibBitmapData.write(zStream);
		byte[] zData = zStream.getData();
		outStream.writeBytes(zData);
	}

	void setData(byte[] data) throws IOException {
		InputBitStream inStream = new InputBitStream(data);
		characterId     = inStream.readUI16();
		format		    = inStream.readUI8();
		width		    = inStream.readUI16();
		height		    = inStream.readUI16();
		short colorTableSize = 0;
		if (format == FORMAT_8_BIT_COLORMAPPED) {
			colorTableSize = (short) (inStream.readUI8() + 1);
		}
		int zLength  = (int) (data.length - inStream.getOffset());
		byte[] zData = new byte[zLength];
		System.arraycopy(data, (int) inStream.getOffset(), zData, 0, zLength);
		InputBitStream zStream = new InputBitStream(zData);
		zStream.enableCompression();
		switch (format) {
			case FORMAT_8_BIT_COLORMAPPED:
				zlibBitmapData = new AlphaColorMapData(
						zStream, colorTableSize, width, height);
				break;
			case FORMAT_32_BIT_RGBA:
				zlibBitmapData = new AlphaBitmapData(zStream, width, height);
				break;
			default:
				throw new IOException("Unknown bitmap format!");
		}
	}
}

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

package com.jswiff.util;

/**
 * This class provides methods for working with hexadecimal representations of data.
 */
public class HexUtils {
	private static final char[] HEX_DIGITS = {
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
			'E', 'F'
		};

	/**
	 * Returns the hexadecimal representation of a byte array.
	 * 
	 * @param data byte array
	 *
	 * @return hex representation
	 */
	public static String toHex(final byte[] data) {
		return toHex(data, 0, data.length);
	}

	/**
	 * Returns the hexadecimal representation of a part of a byte array.
	 *
	 * @param data byte array
	 * @param startPos start position
	 * @param length length of the relevant array portion
	 *
	 * @return hex representation
	 */
	public static String toHex(
		final byte[] data, final int startPos, final int length) {
		StringBuffer b = new StringBuffer();
		int endPos     = startPos + length;
		for (int i = startPos; i < endPos; i++) {
			if (i > 0) {
				b.append(' ');
			}
			int c = data[i];
			b.append(HEX_DIGITS[(c & 0xF0) >> 4]);
			b.append(HEX_DIGITS[(c & 0x0F) >> 0]);
		}
		return b.toString();
	}
}

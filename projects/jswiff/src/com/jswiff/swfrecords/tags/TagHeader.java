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

import java.io.IOException;


/**
 * This class represents a SWF tag header.
 */
public final class TagHeader {
  private short code;
  private int length;

  /*
   * Creates a new TagHeader instance.
   */
  TagHeader() {
    // nothing to do
  }

  TagHeader(InputBitStream stream) throws IOException {
    read(stream);
  }

  /**
   * Returns the code of the tag which designates its type.
   *
   * @return tag type code
   *
   * @see TagConstants
   */
  public short getCode() {
    return code;
  }

  /**
   * Returns the length of the tag.
   *
   * @return tag length
   */
  public int getLength() {
    return length;
  }

  private void read(InputBitStream stream) throws IOException {
    int codeAndLength = stream.readUI16();
    code     = (short) (codeAndLength >> 6); // upper 10 bits
    length   = codeAndLength & 0x3F; // 0x3F = 63 = 111111, lower 6 bits
    if (length == 0x3F) {
      length = (int) stream.readUI32();
    }
  }
}

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

package com.jswiff.swfrecords;

import com.jswiff.io.InputBitStream;
import com.jswiff.io.OutputBitStream;

import java.io.IOException;
import java.io.Serializable;


/**
 * A kerning record is used to adjust the distance between two glyphs.
 */
public final class KerningRecord implements Serializable {
  private char left;
  private char right;
  private short adjustment;

  /**
   * Creates a new KerningRecord instance. Specify a character pair and an
   * adjustment to the advance value (i.e. the distance between glyph
   * reference points) of the left character.
   *
   * @param left left character
   * @param right right character
   * @param adjustment adjustment relative to advance value of left character
   *        (in EM square coords)
   */
  public KerningRecord(char left, char right, short adjustment) {
    this.left         = left;
    this.right        = right;
    this.adjustment   = adjustment;
  }

  /**
   * Creates a new KerningRecord instance, reading data from a bit stream.
   *
   * @param stream source bit stream
   * @param wideCodes if <code>true</code>, 16 bits are used for character code
   *        representation (instead of 8)
   *
   * @throws IOException if an I/O error occured
   */
  public KerningRecord(InputBitStream stream, boolean wideCodes)
    throws IOException {
    if (wideCodes) {
      left    = (char) stream.readUI16();
      right   = (char) stream.readUI16();
    } else {
      left    = (char) stream.readUI8();
      right   = (char) stream.readUI8();
    }
    adjustment = stream.readSI16();
  }

  /**
   * Returns the adjustment to the advance value (i.e. the distance between
   * glyph reference points) of the character on the left.
   *
   * @return adjustment relative to advance value of left character (in EM
   *         square coords)
   */
  public short getAdjustment() {
    return adjustment;
  }

  /**
   * Returns the character on the left.
   *
   * @return left character
   */
  public char getLeft() {
    return left;
  }

  /**
   * Returns the character on the right.
   *
   * @return right character
   */
  public char getRight() {
    return right;
  }

  /**
   * Writes the instance to a bit stream.
   *
   * @param stream target bit stream
   * @param wideCodes if <code>true</code>, two bytes are used for
   *
   * @throws IOException if an I/O error occured
   */
  public void write(OutputBitStream stream, boolean wideCodes)
    throws IOException {
    if (wideCodes) {
      stream.writeUI16(left);
      stream.writeUI16(right);
    } else {
      stream.writeUI8((short) left);
      stream.writeUI8((short) right);
    }
    stream.writeSI16(adjustment);
  }
}

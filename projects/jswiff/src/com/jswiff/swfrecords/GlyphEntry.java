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
 * This class describes a single text character by referencing a glyph from the
 * text font's glyph table.
 */
public final class GlyphEntry implements Serializable {
  private int glyphIndex;
  private int glyphAdvance;

  /**
   * Creates a new GlyphEntry instance. Specify the index of the glyph in the
   * glyph table of the text font, and the advance value (i.e. the horizontal
   * distance between the reference points of current and subsequent glyph)
   *
   * @param glyphIndex index of glyph in glyph table
   * @param glyphAdvance advance in twips (1/20 px)
   */
  public GlyphEntry(int glyphIndex, int glyphAdvance) {
    this.glyphIndex     = glyphIndex;
    this.glyphAdvance   = glyphAdvance;
  }

  /**
   * Creates a new GlyphEntry instance, reading data from a bit stream.
   *
   * @param stream source bit stream
   * @param glyphBits bit count used for glyph index representation
   * @param advanceBits bit count used for advance value representation
   *
   * @throws IOException if an I/O error occured
   */
  public GlyphEntry(InputBitStream stream, short glyphBits, short advanceBits)
    throws IOException {
    glyphIndex     = (int) stream.readUnsignedBits(glyphBits);
    glyphAdvance   = (int) stream.readSignedBits(advanceBits);
  }

  /**
   * Returns the glyph's advance value, i.e. the horizontal distance between
   * the reference points of current and subsequent glyph.
   *
   * @return glyph advance in twips (1/20 px)
   */
  public int getGlyphAdvance() {
    return glyphAdvance;
  }

  /**
   * Returns this glyph's index in the glyph table of the text font.
   *
   * @return index of glyph in glyph table
   */
  public int getGlyphIndex() {
    return glyphIndex;
  }

  /**
   * Writes the instance to a bit stream.
   *
   * @param stream target bit stream
   * @param glyphBits bit count used for glyph index representation
   * @param advanceBits bit count used for advance value representation
   *
   * @throws IOException if an I/O error occured
   */
  public void write(OutputBitStream stream, short glyphBits, short advanceBits)
    throws IOException {
    stream.writeUnsignedBits(glyphIndex, glyphBits);
    stream.writeSignedBits(glyphAdvance, advanceBits);
  }
}

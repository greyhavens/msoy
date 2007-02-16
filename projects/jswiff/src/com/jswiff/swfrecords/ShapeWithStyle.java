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


/**
 * This class extends the <code>Shape</code> class by including fill and line
 * styles. Like the <code>Shape</code> class, <code>ShapeWithStyle</code> also
 * contains one or more <code>ShapeRecord</code> instances which define style
 * changes and primitives as lines and curves. Used within
 * <code>DefineShape</code>, <code>DefineShape2</code> and
 * <code>DefineShape3</code>.
 *
 * @see com.jswiff.swfrecords.tags.DefineShape
 * @see com.jswiff.swfrecords.tags.DefineShape2
 * @see com.jswiff.swfrecords.tags.DefineShape3
 */
public final class ShapeWithStyle extends Shape {
  private FillStyleArray fillStyles;
  private LineStyleArray lineStyles;

  /**
   * Creates a new ShapeWithStyle instance. Supply a fill and line style array
   * and an array of shape records. The style arrays must contain less than
   * 256 styles when used within a <code>DefineShape</code> tag.
   *
   * @param fillStyles fill style array
   * @param lineStyles line style array
   * @param shapeRecords shape record array
   */
  public ShapeWithStyle(
    FillStyleArray fillStyles, LineStyleArray lineStyles,
    ShapeRecord[] shapeRecords) {
    super(shapeRecords);
    this.fillStyles   = fillStyles;
    this.lineStyles   = lineStyles;
  }

  /**
   * Creates a new ShapeWithStyle instance, reading data from a bit stream.
   *
   * @param stream source bit stream
   * @param hasAlpha whether transparency is supported
   *
   * @throws IOException if an I/O error occured
   */
  public ShapeWithStyle(InputBitStream stream, boolean hasAlpha)
    throws IOException {
    fillStyles   = new FillStyleArray(stream, hasAlpha);
    lineStyles   = new LineStyleArray(stream, hasAlpha);
    read(stream, false, hasAlpha);
  }
  
  public ShapeWithStyle(InputBitStream stream)
  throws IOException {
  fillStyles   = new FillStyleArray(stream, true);
  lineStyles   = new LineStyleArray(stream);
  read(stream, true, true);
}

  /**
   * Returns the fill style array.
   *
   * @return fill styles
   */
  public FillStyleArray getFillStyles() {
    return fillStyles;
  }

  /**
   * Returns the line style array.
   *
   * @return line styles
   */
  public LineStyleArray getLineStyles() {
    return lineStyles;
  }

  /**
   * Writes this instance to a bit stream.
   *
   * @param stream target bit stream
   *
   * @throws IOException if an I/O error occured
   */
  public void write(OutputBitStream stream) throws IOException {
    fillStyles.write(stream);
    lineStyles.write(stream);
    super.write(stream);
  }
}

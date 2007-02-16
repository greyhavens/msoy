/*
 * JSwiff is an open source Java API for Macromedia Flash file generation
 * and manipulation
 *
 * Copyright (C) 2004-2006 Ralf Terdic (contact@jswiff.com)
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

import java.util.ArrayList;
import java.util.List;


/**
 * <p>
 * Implements an array of line styles. If used within
 * <code>DefineShape4</code>, the array contains only <code>LineStyle2</code>
 * instances. Otherwise it contains only <code>LineStyle</code> instances.
 * </p>
 * 
 * <p>
 * <b>WARNING:</b> array index starts with 1, not 0
 * </p>
 *
 * @see com.jswiff.swfrecords.tags.DefineShape4
 * @see com.jswiff.swfrecords.LineStyle
 * @see com.jswiff.swfrecords.LineStyle2
 */
public final class LineStyleArray implements Serializable {
  private List styles = new ArrayList();

  /**
   * Creates a new LineStyleArray instance.
   */
  public LineStyleArray() {
    // empty
  }

  LineStyleArray(InputBitStream stream, boolean hasAlpha)
    throws IOException {
    int styleCount = stream.readUI8();
    if (styleCount == 0xFF) {
      styleCount = stream.readUI16();
    }
    for (int i = 0; i < styleCount; i++) {
      styles.add(new LineStyle(stream, hasAlpha));
    }
  }

  LineStyleArray(InputBitStream stream) throws IOException {
    int styleCount = stream.readUI8();
    if (styleCount == 0xFF) {
      styleCount = stream.readUI16();
    }
    for (int i = 0; i < styleCount; i++) {
      styles.add(new LineStyle2(stream));
    }
  }

  /**
   * Returns the size of the line style array.
   *
   * @return array size
   */
  public int getSize() {
    return styles.size();
  }

  /**
   * <p>
   * Returns the line style at the specified position in the array. Can be
   * either a <code>LineStyle</code> or a <code>LineStyle2</code> instance.
   * </p>
   * 
   * <p>
   * <b>WARNING:</b> indexes start at 1, not at 0!
   * </p>
   *
   * @param index index starting at 1
   *
   * @return line style located at the specified position
   *
   * @see com.jswiff.swfrecords.LineStyle
   * @see com.jswiff.swfrecords.LineStyle2
   */
  public Object getStyle(int index) {
    return styles.get(index - 1);
  }

  /**
   * Returns all contained line styles as a list.
   *
   * @return all line styles
   */
  public List getStyles() {
    return styles;
  }

  /**
   * Adds a line style at the end of the array. Use either
   * <code>LineStyle</code> or <code>LineStyle2</code> instances.
   *
   * @param lineStyle a line style
   *
   * @see com.jswiff.swfrecords.LineStyle
   * @see com.jswiff.swfrecords.LineStyle2
   */
  public void addStyle(Object lineStyle) {
    styles.add(lineStyle);
  }

  void write(OutputBitStream stream) throws IOException {
    int styleCount = styles.size();
    if (styleCount >= 0xFF) {
      stream.writeUI8((short) 0xFF);
      stream.writeUI16(styleCount);
    } else {
      stream.writeUI8((short) styleCount);
    }
    for (int i = 0; i < styles.size(); i++) {
      Object lineStyle = styles.get(i);
      if (lineStyle instanceof LineStyle) {
        ((LineStyle) lineStyle).write(stream);
      } else {
        ((LineStyle2) lineStyle).write(stream);
      }
    }
  }
}

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

import java.util.ArrayList;
import java.util.List;


/**
 * <p>
 * Implements an array of fill styles.
 * </p>
 * 
 * <p>
 * <b>WARNING:</b> array index starts at 1, not 0!
 * </p>
 */
public final class FillStyleArray implements Serializable {
  private List styles = new ArrayList();

  /**
   * Creates a new FillStyleArray instance.
   */
  public FillStyleArray() {
    // empty
  }

  FillStyleArray(InputBitStream stream, boolean hasAlpha)
    throws IOException {
    int styleCount = stream.readUI8();
    if (styleCount == 0xff) {
      styleCount = stream.readUI16();
    }
    for (int i = 0; i < styleCount; i++) {
      FillStyle fillStyle = new FillStyle(stream, hasAlpha);
      styles.add(fillStyle);
    }
  }

  /**
   * Returns the size of the fill style array.
   *
   * @return array size
   */
  public int getSize() {
    return styles.size();
  }

  /**
   * <p>
   * Returns the fill style at the specified position in the array.
   * </p>
   * 
   * <p>
   * <b>WARNING:</b> indexes start at 1, not at 0!
   * </p>
   *
   * @param index index starting at 1
   *
   * @return fill style located at the specified position
   */
  public FillStyle getStyle(int index) {
    return (FillStyle) styles.get(index - 1);
  }

  /**
   * Adds a fill style at the end of the array.
   *
   * @param fillStyle a fill style
   */
  public void addStyle(FillStyle fillStyle) {
    styles.add(fillStyle);
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
      ((FillStyle) styles.get(i)).write(stream);
    }
  }
}

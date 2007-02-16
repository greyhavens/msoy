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
 * This class is used to define a line style. Contains line width and color.
 */
public class LineStyle implements Serializable {
  private int width;
  private Color color;

  /**
   * Creates a new line style. Specify the width of the line in twips (1/20 px)
   * and the line color, which can be an <code>RGBA</code> or an
   * <code>RGB</code> instance depending on whether the tag the line style is
   * contained in supports transparency or not (<code>DefineShape3</code>
   * supports transparency).
   *
   * @param width line width
   * @param color line color
   *
   * @see com.jswiff.swfrecords.tags.DefineShape3
   */
  public LineStyle(int width, Color color) {
    this.width   = width;
    this.color   = color;
  }

  LineStyle(InputBitStream stream, boolean hasAlpha) throws IOException {
    width = stream.readUI16();
    if (hasAlpha) {
      color = new RGBA(stream);
    } else {
      color = new RGB(stream);
    }
  }

  /**
   * Returns the line color.
   *
   * @return line color
   */
  public Color getColor() {
    return color;
  }

  /**
   * Returns the line width in twips (1/20 px).
   *
   * @return line width in twips
   */
  public int getWidth() {
    return width;
  }

  void write(OutputBitStream stream) throws IOException {
    stream.writeUI16(width);
    color.write(stream);
  }
}

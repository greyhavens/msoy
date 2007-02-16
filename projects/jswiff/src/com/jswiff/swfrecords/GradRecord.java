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
 * This class implements a gradient control point. A control point is defined
 * by a ratio (i.e. the position of the control point in the gradient) and a
 * color value. Depending on whether the tag this record is used in supports
 * transparency or not, the color is either an <code>RGBA</code> or an
 * <code>RGB</code> value (e.g. <code>DefineShape3</code> supports
 * transparency).
 *
 * @see Gradient
 * @see com.jswiff.swfrecords.tags.DefineShape3
 */
public final class GradRecord implements Serializable {
  private short ratio;
  private Color color;

  /**
   * <p>
   * Creates a new GradRecord (i.e. a gradient control point) instance. You
   * have to specify the ratio and the color value of the control point.
   * </p>
   * 
   * <p>
   * The ratio is a value between 0 and 255. 0 maps to the left edge of the
   * gradient square for a linear gradient, 255 to the right edge. For radial
   * gradients, 0 maps to the center of the square and 255 to the largest
   * circle fitting inside the square.
   * </p>
   * 
   * <p>
   * The color is either an <code>RGB</code> or an <code>RGBA</code> instance,
   * depending on whether the tag this record is used in supports transparency
   * or not (e.g. <code>DefineShape3</code> does).
   * </p>
   *
   * @param ratio control point ratio (from [0; 255])
   * @param color the color value of the gradient control point
   *
   * @see com.jswiff.swfrecords.tags.DefineShape3
   */
  public GradRecord(short ratio, Color color) {
    this.ratio   = ratio;
    this.color   = color;
  }

  GradRecord(InputBitStream stream, boolean hasAlpha) throws IOException {
    ratio = stream.readUI8();
    if (hasAlpha) {
      color = new RGBA(stream);
    } else {
      color = new RGB(stream);
    }
  }

  /**
   * Returns the color value of the gradient control point.
   *
   * @return color value
   */
  public Color getColor() {
    return color;
  }

  /**
   * Returns the ratio of the gradient control point (a value between 0 and
   * 255).
   *
   * @return control point ratio
   */
  public short getRatio() {
    return ratio;
  }

  void write(OutputBitStream stream) throws IOException {
    stream.writeUI8(ratio);
    color.write(stream);
  }
}

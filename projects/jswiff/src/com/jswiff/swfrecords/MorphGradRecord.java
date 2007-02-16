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
 * This class is used for defining a pair of control points for morph gradients
 * (see <code>MorphGradient</code> for details). Start and end gradients in a
 * morph must have the same number of control points, therefore these are
 * defined pairwise. A control point definition consists of a ratio (i.e. the
 * position of the control point in the gradient) and a color value.
 *
 * @see MorphGradient
 * @see MorphFillStyle
 * @see com.jswiff.swfrecords.tags.DefineMorphShape
 */
public final class MorphGradRecord implements Serializable {
  private short startRatio;
  private RGBA startColor;
  private short endRatio;
  private RGBA endColor;

  /**
   * <p>
   * Creates a new MorphGradRecord instance. Specify ratio and (RGBA) color for
   * each control point.
   * </p>
   * 
   * <p>
   * The ratio is a value between 0 and 255. 0 maps to the left edge of the
   * gradient square for a linear gradient, 255 to the right edge. For radial
   * gradients, 0 maps to the center of the square and 255 to the largest
   * circle fitting inside the square.
   * </p>
   *
   * @param startRatio ratio of control point for start gradient
   * @param startColor color of control point for start gradient
   * @param endRatio ratio of control point for end gradient
   * @param endColor color of control point for end gradient
   */
  public MorphGradRecord(
    short startRatio, RGBA startColor, short endRatio, RGBA endColor) {
    this.startRatio   = startRatio;
    this.startColor   = startColor;
    this.endRatio     = endRatio;
    this.endColor     = endColor;
  }

  MorphGradRecord(InputBitStream stream) throws IOException {
    startRatio   = stream.readUI8();
    startColor   = new RGBA(stream);
    endRatio     = stream.readUI8();
    endColor     = new RGBA(stream);
  }

  /**
   * Returns the color of the control point for the gradient used for filling
   * the morph's end shapes.
   *
   * @return end control point color
   */
  public RGBA getEndColor() {
    return endColor;
  }

  /**
   * Returns the ratio of the control point for the gradient used for filling
   * the morph's end shapes.
   *
   * @return end control point ratio
   */
  public short getEndRatio() {
    return endRatio;
  }

  /**
   * Returns the color of the control point for the gradient used for filling
   * the morph's start shapes.
   *
   * @return start control point color
   */
  public RGBA getStartColor() {
    return startColor;
  }

  /**
   * Returns the ratio of the control point for the gradient used for filling
   * the morph's start shapes.
   *
   * @return start control point ratio
   */
  public short getStartRatio() {
    return startRatio;
  }

  void write(OutputBitStream stream) throws IOException {
    stream.writeUI8(startRatio);
    startColor.write(stream);
    stream.writeUI8(endRatio);
    endColor.write(stream);
  }
}

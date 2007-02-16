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
 * This class is used for the representation of a rectangle. A rectangle is
 * defined by a minimum x- and y-coordinate and a maximum x- and y-coordinate
 * position.
 */
public final class Rect implements Serializable {
  private long xMin;
  private long xMax;
  private long yMin;
  private long yMax;

  /**
   * Creates a new Rect instance. Four coordinates must be specified.
   *
   * @param xMin minimum x in twips (1/20 px)
   * @param xMax maximum x in twips
   * @param yMin minimum y in twips
   * @param yMax maximum y in twips
   */
  public Rect(long xMin, long xMax, long yMin, long yMax) {
    this.xMin   = xMin;
    this.xMax   = xMax;
    this.yMin   = yMin;
    this.yMax   = yMax;
  }

  /*
   *
   */
  public Rect(InputBitStream stream) throws IOException {
    int nBits = (int) (stream.readUnsignedBits(5));
    xMin   = stream.readSignedBits(nBits);
    xMax   = stream.readSignedBits(nBits);
    yMin   = stream.readSignedBits(nBits);
    yMax   = stream.readSignedBits(nBits);
    stream.align();
  }

  /**
   * Returns the maximum x coordinate.
   *
   * @return max x
   */
  public long getXMax() {
    return xMax;
  }

  /**
   * Returns the minimum x coordinate.
   *
   * @return min x
   */
  public long getXMin() {
    return xMin;
  }

  /**
   * Returns the maximum y coordinate.
   *
   * @return max y
   */
  public long getYMax() {
    return yMax;
  }

  /**
   * Returns the minimum y coordinate.
   *
   * @return min y
   */
  public long getYMin() {
    return yMin;
  }

  /**
   * Returns the string representation of the rectangle.
   *
   * @return string representation
   */
  public String toString() {
    return "Rect (" + xMin + ", " + xMax + ", " + yMin + ", " + yMax + ")";
  }

  /**
   * Writes the rectangle to a bit stream.
   *
   * @param stream the target bit stream
   *
   * @throws IOException if an I/O error has occured
   */
  public void write(OutputBitStream stream) throws IOException {
    int nBits = OutputBitStream.getSignedBitsLength(xMin);
    nBits   = Math.max(nBits, OutputBitStream.getSignedBitsLength(xMax));
    nBits   = Math.max(nBits, OutputBitStream.getSignedBitsLength(yMin));
    nBits   = Math.max(nBits, OutputBitStream.getSignedBitsLength(yMax));
    stream.writeUnsignedBits(nBits, 5);
    stream.writeSignedBits(xMin, nBits);
    stream.writeSignedBits(xMax, nBits);
    stream.writeSignedBits(yMin, nBits);
    stream.writeSignedBits(yMax, nBits);
    stream.align();
  }
}

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
 * This class is used for defining quadratic Bezier curves. A Bezier curve is
 * defined by three points: two on-curve anchor points and one off-curve
 * control point. The current drawing position is considered to be the first
 * anchor point, i.e. for a curve definition it is sufficient to specify one
 * control and one (single) anchor point.
 */
public final class CurvedEdgeRecord extends EdgeRecord {
  private int controlDeltaX;
  private int controlDeltaY;
  private int anchorDeltaX;
  private int anchorDeltaY;

  /**
   * Creates a new CurvedEdgeRecord instance. Supply the control point
   * (relative to the current drawing position) and the anchor point (relative
   * to the specified control point).
   *
   * @param controlDeltaX x coordinate of control point (relative to current
   *        position, in twips)
   * @param controlDeltaY y coordinate of control point (relative to current
   *        position, in twips)
   * @param anchorDeltaX x coordinate of anchor point (relative to control
   *        point, in twips)
   * @param anchorDeltaY y coordinate of anchor point (relative to control
   *        point, in twips)
   */
  public CurvedEdgeRecord(
    int controlDeltaX, int controlDeltaY, int anchorDeltaX, int anchorDeltaY) {
    this.controlDeltaX   = controlDeltaX;
    this.controlDeltaY   = controlDeltaY;
    this.anchorDeltaX    = anchorDeltaX;
    this.anchorDeltaY    = anchorDeltaY;
  }

  /**
   * Creates a new CurvedEdgeRecord instance, reading data from a bit stream.
   *
   * @param stream source bit stream
   *
   * @throws IOException if an I/O error occured
   */
  public CurvedEdgeRecord(InputBitStream stream) throws IOException {
    int numBits = (int) stream.readUnsignedBits(4) + 2;
    controlDeltaX   = (int) stream.readSignedBits(numBits);
    controlDeltaY   = (int) stream.readSignedBits(numBits);
    anchorDeltaX    = (int) stream.readSignedBits(numBits);
    anchorDeltaY    = (int) stream.readSignedBits(numBits);
  }

  /**
   * Returns the x coordinate of the anchor point, relative to the control
   * point.
   *
   * @return x delta of anchor point in twips (1/20 px)
   */
  public int getAnchorDeltaX() {
    return anchorDeltaX;
  }

  /**
   * Returns the y coordinate of the anchor point, relative to the control
   * point.
   *
   * @return y delta of anchor point in twips (1/20 px)
   */
  public int getAnchorDeltaY() {
    return anchorDeltaY;
  }

  /**
   * Returns the x coordinate of the control point, relative to the current
   * drawing position.
   *
   * @return x delta of control point in twips (1/20 px)
   */
  public int getControlDeltaX() {
    return controlDeltaX;
  }

  /**
   * Returns the y coordinate of the control point, relative to the current
   * drawing position.
   *
   * @return y delta of control point in twips (1/20 px)
   */
  public int getControlDeltaY() {
    return controlDeltaY;
  }

  /**
   * Writes this instance to a bit stream.
   *
   * @param stream target bit stream
   *
   * @throws IOException if an I/O error occured
   */
  public void write(OutputBitStream stream) throws IOException {
    stream.writeUnsignedBits(1, 1); // edge record
    stream.writeUnsignedBits(0, 1); // curved edge
    int numBits = 2;
    numBits   = Math.max(
        numBits, OutputBitStream.getSignedBitsLength(controlDeltaX));
    numBits   = Math.max(
        numBits, OutputBitStream.getSignedBitsLength(controlDeltaY));
    numBits   = Math.max(
        numBits, OutputBitStream.getSignedBitsLength(anchorDeltaX));
    numBits   = Math.max(
        numBits, OutputBitStream.getSignedBitsLength(anchorDeltaY));
    stream.writeUnsignedBits(numBits - 2, 4);
    stream.writeSignedBits(controlDeltaX, numBits);
    stream.writeSignedBits(controlDeltaY, numBits);
    stream.writeSignedBits(anchorDeltaX, numBits);
    stream.writeSignedBits(anchorDeltaY, numBits);
  }
}

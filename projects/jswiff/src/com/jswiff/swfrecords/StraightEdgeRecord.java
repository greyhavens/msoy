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
 * This class is used for defining a line between two points. The current
 * drawing position is considered to be the first point. The second point is
 * specified as coordinates relative to the current drawing position.
 */
public final class StraightEdgeRecord extends EdgeRecord {
  private int deltaX;
  private int deltaY;

  /**
   * Creates a new StraightEdgeRecord instance. Specify the point the line is
   * supposed to be drawn to by supplying its coordinates relative to the
   * current drawing position.
   *
   * @param deltaX x coordinate relative to current position (in twips)
   * @param deltaY y coordinate relative to current position (in twips)
   */
  public StraightEdgeRecord(int deltaX, int deltaY) {
    this.deltaX   = deltaX;
    this.deltaY   = deltaY;
  }

  /**
   * Creates a new StraightEdgeRecord instance, reading data from a bit stream.
   *
   * @param stream source bit stream
   *
   * @throws IOException if an I/O error occured
   */
  public StraightEdgeRecord(InputBitStream stream) throws IOException {
    byte numBits            = (byte) stream.readUnsignedBits(4);
    boolean generalLineFlag = stream.readBooleanBit();
    if (generalLineFlag) {
      // read deltas
      deltaX   = (int) stream.readSignedBits(numBits + 2);
      deltaY   = (int) stream.readSignedBits(numBits + 2);
    } else {
      boolean vertLineFlag = stream.readBooleanBit();
      if (vertLineFlag) {
        // vertical line; read deltaY
        deltaY = (int) stream.readSignedBits(numBits + 2);
      } else {
        // horizontal line; read deltaX
        deltaX = (int) stream.readSignedBits(numBits + 2);
      }
    }
  }

  /**
   * Returns the x coordinate of the point the line is supposed to be drawn to,
   * relative to the current drawing position.
   *
   * @return x delta in twips (1/20 px)
   */
  public int getDeltaX() {
    return deltaX;
  }

  /**
   * Returns the y coordinate of the point the line is supposed to be drawn to,
   * relative to the current drawing position.
   *
   * @return y delta in twips (1/20 px)
   */
  public int getDeltaY() {
    return deltaY;
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
    stream.writeUnsignedBits(1, 1); // straight edge
    int numBits = 2;
    numBits   = Math.max(numBits, OutputBitStream.getSignedBitsLength(deltaX));
    numBits   = Math.max(numBits, OutputBitStream.getSignedBitsLength(deltaY));
    stream.writeUnsignedBits(numBits - 2, 4);
    boolean generalLineFlag = (deltaX != 0) && (deltaY != 0);
    stream.writeBooleanBit(generalLineFlag);
    if (generalLineFlag) {
      stream.writeSignedBits(deltaX, numBits);
      stream.writeSignedBits(deltaY, numBits);
    } else {
      boolean vertLineFlag = (deltaX == 0);
      stream.writeBooleanBit(vertLineFlag);
      if (vertLineFlag) {
        stream.writeSignedBits(deltaY, numBits);
      } else {
        stream.writeSignedBits(deltaX, numBits);
      }
    }
  }
}

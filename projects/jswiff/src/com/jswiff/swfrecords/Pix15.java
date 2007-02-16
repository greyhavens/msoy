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
 * This class is used for the representation of 15-bit pixel data.
 */
public final class Pix15 extends BitmapPixelData {
  private byte red;
  private byte green;
  private byte blue;

  /**
   * Creates a new Pix15 instance. Specify red, green and blue values.
   *
   * @param red red value (between 0 and 31)
   * @param green green value (between 0 and 31)
   * @param blue blue value (between 0 and 31)
   */
  public Pix15(byte red, byte green, byte blue) {
    this.red     = red;
    this.green   = green;
    this.blue    = blue;
  }

  Pix15(InputBitStream stream) throws IOException {
    stream.readUnsignedBits(1); // ignore MSB
    red     = (byte) stream.readUnsignedBits(5);
    green   = (byte) stream.readUnsignedBits(5);
    blue    = (byte) stream.readUnsignedBits(5);
  }

  /**
   * Returns the blue value.
   *
   * @return blue value (between 0 and 31)
   */
  public byte getBlue() {
    return blue;
  }

  /**
   * Returns the green value.
   *
   * @return green value (between 0 and 31)
   */
  public byte getGreen() {
    return green;
  }

  /**
   * Returns the red value.
   *
   * @return red value (between 0 and 31)
   */
  public byte getRed() {
    return red;
  }

  void write(OutputBitStream stream) throws IOException {
    stream.writeUnsignedBits(0, 1); // reserved bit
    stream.writeUnsignedBits(red, 5);
    stream.writeUnsignedBits(green, 5);
    stream.writeUnsignedBits(blue, 5);
  }
}

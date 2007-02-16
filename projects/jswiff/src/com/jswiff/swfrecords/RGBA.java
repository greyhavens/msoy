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


/**
 * This class represents a color as a 32-bit red, green, blue and alpha value.
 * The alpha value represents the transparency (or opacity). 0 means
 * completely transparent, 255 means completely opaque.
 */
public final class RGBA extends Color {
  /** Black */
  public static final RGBA BLACK = new RGBA(0, 0, 0, 255);
  /** White */
  public static final RGBA WHITE = new RGBA(255, 255, 255, 255);
  private short red;
  private short green;
  private short blue;
  private short alpha;

  /**
   * Creates a new RGBA instance.
   *
   * @param red red value (between 0 and 255)
   * @param green green value (between 0 and 255)
   * @param blue blue value (between 0 and 255)
   * @param alpha alpha value (between 0 and 255)
   */
  public RGBA(short red, short green, short blue, short alpha) {
    this.red     = red;
    this.green   = green;
    this.blue    = blue;
    this.alpha   = alpha;
  }

  /**
   * Creates a new RGBA instance. Convenience constructor which can be used to
   * avoid annoying type casts with literals like<br>
   * new RGBA((short) 0, (short) 0, (short) 0, (short) 255);
   *
   * @param red red value (between 0 and 255)
   * @param green green value (between 0 and 255)
   * @param blue blue value (between 0 and 255)
   * @param alpha alpha value (between 0 and 255)
   */
  public RGBA(int red, int green, int blue, int alpha) {
    this.red     = (short) red;
    this.green   = (short) green;
    this.blue    = (short) blue;
    this.alpha   = (short) alpha;
  }

  /**
   * Reads an instance from a bit stream.
   *
   * @param stream source bit stream
   *
   * @throws IOException if an I/O error has occured
   */
  public RGBA(InputBitStream stream) throws IOException {
    red     = stream.readUI8();
    green   = stream.readUI8();
    blue    = stream.readUI8();
    alpha   = stream.readUI8();
  }

  /**
   * Reads an instance in ARGB format from a bit stream.
   *
   * @param stream source bit stream
   *
   * @return read instance
   *
   * @throws IOException if an I/O error occured
   */
  public static RGBA readARGB(InputBitStream stream) throws IOException {
    int a = stream.readUI8();
    int r = stream.readUI8();
    int g = stream.readUI8();
    int b = stream.readUI8();
    return new RGBA(r, g, b, a);
  }

  /**
   * Returns the alpha value.
   *
   * @return alpha value (between 0 and 255)
   */
  public short getAlpha() {
    return alpha;
  }

  /**
   * Returns the blue value.
   *
   * @return blue value (between 0 and 255)
   */
  public short getBlue() {
    return blue;
  }

  /**
   * Returns the green value.
   *
   * @return green value (between 0 and 255)
   */
  public short getGreen() {
    return green;
  }

  /**
   * Returns the red value.
   *
   * @return red value (between 0 and 255)
   */
  public short getRed() {
    return red;
  }

  /**
   * Returns the string representation of the instance.
   *
   * @return string representation of color
   */
  public String toString() {
    return "RGBA (" + red + "; " + green + "; " + blue + "; " + alpha + ")";
  }

  /**
   * Writes the instance to a bit stream.
   *
   * @param stream the target bit stream
   *
   * @throws IOException if an I/O error has occured
   */
  public void write(OutputBitStream stream) throws IOException {
    stream.writeUI8(red);
    stream.writeUI8(green);
    stream.writeUI8(blue);
    stream.writeUI8(alpha);
  }

  /**
   * Writes the instance as an ARGB structure.
   *
   * @param stream the target bit stream
   *
   * @throws IOException if an I/O error has occured
   */
  public void writeARGB(OutputBitStream stream) throws IOException {
    stream.writeUI8(alpha);
    stream.writeUI8(red);
    stream.writeUI8(green);
    stream.writeUI8(blue);
  }
}

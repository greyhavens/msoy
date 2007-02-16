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
 * This class is used within <code>DefineBitsLossless2</code> tags (with 32-bit
 * RGBA images). It contains an array of pixel colors and transparency
 * information. No scanline padding is needed.
 */
public final class AlphaBitmapData extends ZlibBitmapData {
  private RGBA[] bitmapPixelData;

  /**
   * Creates a new AlphaBitmapData instance. Supply an RGBA array of size
   * [width x height]. No scanline padding is needed.
   *
   * @param bitmapPixelData RGBA array
   */
  public AlphaBitmapData(RGBA[] bitmapPixelData) {
    this.bitmapPixelData = bitmapPixelData;
  }

  /**
   * Creates a new AlphaBitmapData instance, reading data from a bit stream.
   *
   * @param stream source bit stream
   * @param width image width
   * @param height image height
   *
   * @throws IOException if an I/O error occured
   */
  public AlphaBitmapData(InputBitStream stream, int width, int height)
    throws IOException {
    int imageDataSize = width * height;
    bitmapPixelData = new RGBA[imageDataSize];
    for (int i = 0; i < imageDataSize; i++) {
      bitmapPixelData[i] = RGBA.readARGB(stream);
    }
  }

  /**
   * Returns the bitmap data, i.e. an array of pixel colors and transparency
   * information.
   *
   * @return bitmap pixel data (one RGBA value for each pixel)
   */
  public RGBA[] getBitmapPixelData() {
    return bitmapPixelData;
  }

  /**
   * Writes this instance to a bit stream.
   *
   * @param stream target bit stream
   *
   * @throws IOException if an I/O error occured
   */
  public void write(OutputBitStream stream) throws IOException {
    for (int i = 0; i < bitmapPixelData.length; i++) {
      bitmapPixelData[i].writeARGB(stream);
    }
  }
}

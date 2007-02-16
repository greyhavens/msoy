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
import com.jswiff.swfrecords.tags.DefineBitsLossless;

import java.io.IOException;


/**
 * This class is used within <code>DefineBitsLossless</code> tags (with 15-bit
 * or 24-bit RGB images). It contains an array of pixel colors.
 */
public final class BitmapData extends ZlibBitmapData {
  private BitmapPixelData[] bitmapPixelData;

  /**
   * Creates a new BitmapData instance.
   *
   * @param bitmapPixelData
   */
  public BitmapData(BitmapPixelData[] bitmapPixelData) {
    this.bitmapPixelData = bitmapPixelData;
  }

  /**
   * Creates a new BitmapData instance, reading data from a bit stream.
   *
   * @param stream source bit stream
   * @param bitmapFormat <code>DefineBitsLossless.FORMAT_15_BIT_RGB</code> or
   *        <code>DefineBitsLossless.FORMAT_24_BIT_RGB</code>
   * @param width image width in pixels (without padding!)
   * @param height image height in pixels
   *
   * @throws IOException if an I/O error occured
   */
  public BitmapData(
    InputBitStream stream, short bitmapFormat, int width, int height)
    throws IOException {
    int imageDataSize = 0;
    switch (bitmapFormat) {
      case DefineBitsLossless.FORMAT_15_BIT_RGB:
        imageDataSize = (width + getScanlinePadLength(width)) * height;
        bitmapPixelData = new Pix15[imageDataSize];
        for (int i = 0; i < imageDataSize; i++) {
          bitmapPixelData[i] = new Pix15(stream);
        }
        break;
      case DefineBitsLossless.FORMAT_24_BIT_RGB:
        imageDataSize = width * height; // no padding needed
        bitmapPixelData = new Pix24[imageDataSize];
        for (int i = 0; i < imageDataSize; i++) {
          bitmapPixelData[i] = new Pix24(stream);
        }
        break;
    }
  }

  /**
   * Computes the length of the scanline padding for a given width of a 15-bit
   * RGB image. The internal representation of the bitmap data requires a line
   * to start and end at a 32-bit boundary. This makes sense only for 15-bit
   * RGB images, as 24-bit RGB images don't need scanline padding, a 24-bit
   * RGB pixel being internally represented by four bytes. As 15-bit RGB pixel
   * data consists of two bytes per pixel, the padding can be 0 or 1 pixels
   * long, depending on the width of the image.
   *
   * @param width image width (in pixels)
   *
   * @return padding length (in bytes)
   */
  public static int getScanlinePadLength(int width) {
    return (width & 1); // if even, 0, if odd, 1
  }

  /**
   * Returns the pixel data array, for each pixel a color table index. Warning:
   * the image data may contain up to 4 pad pixels at the end of each line as
   * the internal representation of the pixel data requires lines to start and
   * end on 32-bit boundaries. Use <code>getScanLinePadLength()</code> to
   * compute the number of pad pixels per line.
   *
   * @return image data
   */
  public BitmapPixelData[] getBitmapPixelData() {
    return bitmapPixelData;
  }

  /**
   * Writes the instance to a bit stream.
   *
   * @param stream target bit stream
   *
   * @throws IOException if an I/O error occured
   */
  public void write(OutputBitStream stream) throws IOException {
    for (int i = 0; i < bitmapPixelData.length; i++) {
      bitmapPixelData[i].write(stream);
    }
  }
}

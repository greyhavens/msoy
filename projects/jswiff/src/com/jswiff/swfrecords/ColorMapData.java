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
 * <p>
 * This class is used within <code>DefineBitsLossless</code> tags (with 8-bit
 * colormapped images). It contains a color table and an array of pixel data.
 * The color table contains a palette of up to 256 RGB colors. The pixel data
 * array contains color table indices. Its size is the product of padded image
 * width and image height.
 * </p>
 * 
 * <p>
 * Each line is padded with a scanline pad which makes sure the internal
 * representation starts and ends at a 32-bit boundary. Use
 * <code>getScanlinePadLength()</code> to compute this padding length
 * depending on the width of the image. The computed number of pixels must be
 * added as pad to the end of each image line. The color of the pad pixels is
 * ignored.
 * </p>
 */
public final class ColorMapData extends ZlibBitmapData {
  private RGB[] colorTableRGB;
  private short[] colorMapPixelData;

  /**
   * Creates a new AlphaColorMapData instance. Supply a color table (of up to
   * 256 RGB values) and an array of pixel data of size [paddedWidth x
   * height]. The pixel data consists of color table indices.
   *
   * @param colorTableRGB color table, i.e. an array of up to 256 RGB values
   * @param colorMapPixelData array of color table indices
   */
  public ColorMapData(RGB[] colorTableRGB, short[] colorMapPixelData) {
    this.colorTableRGB       = colorTableRGB;
    this.colorMapPixelData   = colorMapPixelData;
  }

  /**
   * Creates a new ColorMapData instance, reading data from a bit stream.
   *
   * @param stream source bit stream
   * @param colorTableSize color of table size (up to 256)
   * @param width image width in pixels (without padding!)
   * @param height image height in pixels
   *
   * @throws IOException if an I/O error occured
   */
  public ColorMapData(
    InputBitStream stream, short colorTableSize, int width, int height)
    throws IOException {
    colorTableRGB = new RGB[colorTableSize];
    for (int i = 0; i < colorTableSize; i++) {
      colorTableRGB[i] = new RGB(stream);
    }
    int imageDataSize = (width + getScanlinePadLength(width)) * height;
    colorMapPixelData = new short[imageDataSize];
    for (int i = 0; i < imageDataSize; i++) {
      colorMapPixelData[i] = stream.readUI8();
    }
  }

  /**
   * Computes the length of the scanline padding for a given image width. The
   * internal representation of the bitmap data requires a line to start and
   * end at a 32-bit boundary. As pixel data consists of one byte per pixel,
   * the padding can be 0, 1, 2 or 3 pixels long, depending on the width of
   * the image.
   *
   * @param width image width (in pixels)
   *
   * @return padding length (in bytes)
   */
  public static int getScanlinePadLength(int width) {
    int pad = 0;
    if ((width & 3) != 0) {
      pad = 4 - (width & 3); // 1, 2 or 3 pad bytes
    }
    return pad;
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
  public short[] getColorMapPixelData() {
    return colorMapPixelData;
  }

  /**
   * Returns the color table which contains up to 256 RGB values which can be
   * referenced by indices contained in the image data array.
   *
   * @return color table (as RGB array)
   */
  public RGB[] getColorTableRGB() {
    return colorTableRGB;
  }

  /**
   * Writes the instance to a bit stream.
   *
   * @param stream target bit stream
   *
   * @throws IOException if an I/O error occured
   */
  public void write(OutputBitStream stream) throws IOException {
    for (int i = 0; i < colorTableRGB.length; i++) {
      colorTableRGB[i].write(stream);
    }
    for (int i = 0; i < colorMapPixelData.length; i++) {
      stream.writeUI8(colorMapPixelData[i]);
    }
  }
}

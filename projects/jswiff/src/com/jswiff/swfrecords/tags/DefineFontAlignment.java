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

package com.jswiff.swfrecords.tags;

import com.jswiff.io.InputBitStream;
import com.jswiff.io.OutputBitStream;
import com.jswiff.swfrecords.AlignmentZone;

import java.io.IOException;


/**
 * @since SWF 8
 */
public class DefineFontAlignment extends Tag {
  /** TODO: Comments */
  public static final byte THIN          = 0;
  /** TODO: Comments */
  public static final byte MEDIUM        = 1;
  /** TODO: Comments */
  public static final byte THICK         = 2;
  private int fontId;
  private byte thickness;
  private AlignmentZone[] alignmentZones;

  /**
   * Creates a new DefineFontAlignment instance.
   *
   * @param fontId TODO: Comments
   * @param thickness TODO: Comments
   * @param alignmentZones TODO: Comments
   */
  public DefineFontAlignment(
    int fontId, byte thickness, AlignmentZone[] alignmentZones) {
    this.fontId           = fontId;
    this.thickness        = thickness;
    this.alignmentZones   = alignmentZones;
    code                  = TagConstants.DEFINE_FONT_ALIGNMENT;
  }

  DefineFontAlignment() {
    super();
  }

  /**
   * TODO: Comments
   *
   * @param alignmentZones TODO: Comments
   */
  public void setAlignmentZones(AlignmentZone[] alignmentZones) {
    this.alignmentZones = alignmentZones;
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public AlignmentZone[] getAlignmentZones() {
    return alignmentZones;
  }

  /**
   * TODO: Comments
   *
   * @param fontId TODO: Comments
   */
  public void setFontId(int fontId) {
    this.fontId = fontId;
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public int getFontId() {
    return fontId;
  }

  /**
   * TODO: Comments
   *
   * @param thickness THIN, MEDIUM or THICK
   */
  public void setThickness(byte thickness) {
    this.thickness = thickness;
  }

  /**
   * TODO: Comments
   *
   * @return THIN, MEDIUM or THICK
   */
  public byte getThickness() {
    return thickness;
  }

  protected void writeData(OutputBitStream outStream) throws IOException {
    outStream.writeUI16(fontId);
    outStream.writeUnsignedBits(thickness, 2);
    outStream.writeUnsignedBits(0, 6);
    if (alignmentZones != null) {
      for (int i=0; i<alignmentZones.length; i++) {
        alignmentZones[i].write(outStream);
      }
    }
  }

  void setData(byte[] data) throws IOException {
    InputBitStream inStream = new InputBitStream(data);
    fontId      = inStream.readUI16();
    thickness   = (byte) inStream.readUnsignedBits(2);
    inStream.readUnsignedBits(6);
    int glyphCount = inStream.available() / 10;
    alignmentZones = new AlignmentZone[glyphCount];
    for (int i = 0; i < glyphCount; i++) {
      alignmentZones[i] = new AlignmentZone(inStream);
    }
  }
}

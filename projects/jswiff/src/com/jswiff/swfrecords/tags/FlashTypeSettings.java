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

import java.io.IOException;


/**
 * @since SWF 8
 */
public final class FlashTypeSettings extends Tag {
  /** TODO: Comments */
  public static final byte GRID_FIT_NONE     = 0;
  /** TODO: Comments */
  public static final byte GRID_FIT_PIXEL    = 1;
  /** TODO: Comments */
  public static final byte GRID_FIT_SUBPIXEL = 2;
  private int textId;
  private boolean flashType;
  private byte gridFit;
  private float thickness;
  private float sharpness;

  /**
   * Creates a new FlashTypeSettings instance.
   *
   * @param textId TODO: Comments
   * @param flashType TODO: Comments
   */
  public FlashTypeSettings(int textId, boolean flashType) {
    code         = TagConstants.FLASHTYPE_SETTINGS;
    this.textId      = textId;
    this.flashType   = flashType;
  }

  FlashTypeSettings() {
    // empty
  }
  
  /**
   * TODO: Comments
   *
   * @param flashType TODO: Comments
   */
  public void setFlashType(boolean flashType) {
    this.flashType = flashType;
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public boolean isFlashType() {
    return flashType;
  }

  /**
   * TODO: Comments
   *
   * @param gridFit TODO: Comments
   */
  public void setGridFit(byte gridFit) {
    this.gridFit = gridFit;
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public byte getGridFit() {
    return gridFit;
  }

  /**
   * TODO: Comments
   *
   * @param sharpness TODO: Comments
   */
  public void setSharpness(float sharpness) {
    this.sharpness = sharpness;
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public double getSharpness() {
    return sharpness;
  }

  /**
   * TODO: Comments
   *
   * @param textId TODO: Comments
   */
  public void setTextId(int textId) {
    this.textId = textId;
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public int getTextId() {
    return textId;
  }

  /**
   * TODO: Comments
   *
   * @param thickness TODO: Comments
   */
  public void setThickness(float thickness) {
    this.thickness = thickness;
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public double getThickness() {
    return thickness;
  }

  protected void writeData(OutputBitStream outStream) throws IOException {
    outStream.writeUI16(textId);
    outStream.writeUnsignedBits(flashType ? 1 : 0, 2);
    outStream.writeUnsignedBits(gridFit, 3);
    outStream.writeUnsignedBits(0, 3);
    outStream.writeFloat(thickness);
    outStream.writeFloat(sharpness);
    outStream.writeUI8((short) 0);
  }

  void setData(byte[] data) throws IOException {
    InputBitStream inStream = new InputBitStream(data);
    textId      = inStream.readUI16();
    flashType   = (inStream.readUnsignedBits(2) == 1);
    gridFit     = (byte) inStream.readUnsignedBits(3);
    inStream.readUnsignedBits(3);
    thickness   = inStream.readFloat();
    sharpness   = inStream.readFloat();
    inStream.readUI8();
  }
}

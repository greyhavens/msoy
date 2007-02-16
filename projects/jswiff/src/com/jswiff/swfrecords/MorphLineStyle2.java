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
 * This class is used to define line styles used in morph sequences. Line
 * styles are defined in pairs: for start and for end shapes.
 *
 * @see MorphLineStyles
 * @see com.jswiff.swfrecords.tags.DefineMorphShape
 * @since SWF 8
 */
public final class MorphLineStyle2 extends EnhancedStrokeStyle {
  private int startWidth;
  private int endWidth;
  private byte startCapStyle       = CAPS_ROUND;
  private byte endCapStyle         = CAPS_ROUND;
  private byte jointStyle          = JOINT_ROUND;
  private boolean pixelHinting;
  private boolean close            = true;
  private byte scaleStroke         = SCALE_BOTH;
  private double miterLimit        = 3;
  private RGBA startColor;
  private RGBA endColor;
  private MorphFillStyle fillStyle;

  /**
   * Creates a new MorphLineStyle2 instance. Supply width and RGBA color for
   * lines in start and end shapes.
   *
   * @param startWidth width of line in start shape (in twips = 1/20 px)
   * @param startColor color of line in start shape
   * @param endWidth width of line in start shape (in twips = 1/20 px)
   * @param endColor color of line in end shape
   */
  public MorphLineStyle2(
    int startWidth, RGBA startColor, int endWidth, RGBA endColor) {
    this.startWidth   = startWidth;
    this.startColor   = startColor;
    this.endWidth     = endWidth;
    this.endColor     = endColor;
  }

  /**
   * Creates a new MorphLineStyle2 instance. Supply start and end width and
   * fill style.
   *
   * @param startWidth width of line in start shape (in twips = 1/20 px)
   * @param endWidth width of line in start shape (in twips = 1/20 px)
   * @param fillStyle color of line in start shape
   */
  public MorphLineStyle2(
    int startWidth, int endWidth, MorphFillStyle fillStyle) {
    this.startWidth   = startWidth;
    this.endWidth     = endWidth;
    this.fillStyle    = fillStyle;
  }

  MorphLineStyle2(InputBitStream stream) throws IOException {
    startWidth      = stream.readUI16();
    endWidth        = stream.readUI16();
    startCapStyle   = (byte) stream.readUnsignedBits(2);
    jointStyle      = (byte) stream.readUnsignedBits(2);
    boolean hasFill = stream.readBooleanBit();
    boolean noHScale = stream.readBooleanBit();
    boolean noVScale = stream.readBooleanBit();
    scaleStroke     = (byte) ((noHScale ? 0 : SCALE_HORIZONTAL) |
      (noVScale ? 0 : SCALE_VERTICAL));
    pixelHinting    = stream.readBooleanBit();
    stream.readUnsignedBits(5);
    close         = !stream.readBooleanBit();
    endCapStyle   = (byte) stream.readUnsignedBits(2);
    if (jointStyle == JOINT_MITER) {
      miterLimit = stream.readFP16();
    }
    if (hasFill) {
      fillStyle = new MorphFillStyle(stream);
    } else {
      startColor   = new RGBA(stream);
      endColor     = new RGBA(stream);
    }
  }

  /**
   * TODO: Comments
   *
   * @param close TODO: Comments
   */
  public void setClose(boolean close) {
    this.close = close;
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public boolean isClose() {
    return close;
  }

  /**
   * TODO: Comments
   *
   * @param endCapStyle TODO: Comments
   */
  public void setEndCapStyle(byte endCapStyle) {
    this.endCapStyle = endCapStyle;
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public byte getEndCapStyle() {
    return endCapStyle;
  }

  /**
   * TODO: Comments
   *
   * @param endColor TODO: Comments
   */
  public void setEndColor(RGBA endColor) {
    this.endColor = endColor;
    if (endColor != null) {
      fillStyle = null;
    }
  }

  /**
   * Returns the color of lines used in the end shape of the morph sequence.
   *
   * @return RGBA color of lines in end shape
   */
  public RGBA getEndColor() {
    return endColor;
  }

  /**
   * TODO: Comments
   *
   * @param endWidth TODO: Comments
   */
  public void setEndWidth(int endWidth) {
    this.endWidth = endWidth;
  }

  /**
   * Returns the width of lines used in the end shape of the morph sequence.
   *
   * @return width of lines in end shape in twips (1/20 px)
   */
  public int getEndWidth() {
    return endWidth;
  }

  /**
   * TODO: Comments
   *
   * @param fillStyle TODO: Comments
   */
  public void setFillStyle(MorphFillStyle fillStyle) {
    this.fillStyle = fillStyle;
    if (fillStyle != null) {
      startColor   = null;
      endColor     = null;
    }
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public MorphFillStyle getFillStyle() {
    return fillStyle;
  }

  /**
   * TODO: Comments
   *
   * @param jointStyle TODO: Comments
   */
  public void setJointStyle(byte jointStyle) {
    this.jointStyle = jointStyle;
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public byte getJointStyle() {
    return jointStyle;
  }

  /**
   * TODO: Comments
   *
   * @param miterLimit TODO: Comments
   */
  public void setMiterLimit(double miterLimit) {
    this.miterLimit = miterLimit;
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public double getMiterLimit() {
    return miterLimit;
  }

  /**
   * TODO: Comments
   *
   * @param pixelHinting TODO: Comments
   */
  public void setPixelHinting(boolean pixelHinting) {
    this.pixelHinting = pixelHinting;
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public boolean isPixelHinting() {
    return pixelHinting;
  }

  /**
   * TODO: Comments
   *
   * @param scaleStroke TODO: Comments
   */
  public void setScaleStroke(byte scaleStroke) {
    this.scaleStroke = scaleStroke;
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public byte getScaleStroke() {
    return scaleStroke;
  }

  /**
   * TODO: Comments
   *
   * @param startCapStyle TODO: Comments
   */
  public void setStartCapStyle(byte startCapStyle) {
    this.startCapStyle = startCapStyle;
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public byte getStartCapStyle() {
    return startCapStyle;
  }

  /**
   * TODO: Comments
   *
   * @param startColor TODO: Comments
   */
  public void setStartColor(RGBA startColor) {
    this.startColor = startColor;
    if (startColor != null) {
      fillStyle = null;
    }
  }

  /**
   * Returns the color of lines used in the start shape of the morph sequence.
   *
   * @return RGBA color of lines in start shape
   */
  public RGBA getStartColor() {
    return startColor;
  }

  /**
   * TODO: Comments
   *
   * @param startWidth TODO: Comments
   */
  public void setStartWidth(int startWidth) {
    this.startWidth = startWidth;
  }

  /**
   * Returns the width of lines used in the start shape of the morph sequence.
   *
   * @return width of lines in start shape in twips (1/20 px)
   */
  public int getStartWidth() {
    return startWidth;
  }

  void write(OutputBitStream stream) throws IOException {
    stream.writeUI16(startWidth);
    stream.writeUI16(endWidth);
    stream.writeUnsignedBits(startCapStyle, 2);
    stream.writeUnsignedBits(jointStyle, 2);
    boolean hasFill = fillStyle != null;
    stream.writeBooleanBit(hasFill);
    stream.writeBooleanBit(
      (scaleStroke == SCALE_VERTICAL) || (scaleStroke == SCALE_NONE));
    stream.writeBooleanBit(
      (scaleStroke == SCALE_HORIZONTAL) || (scaleStroke == SCALE_NONE));
    stream.writeBooleanBit(pixelHinting);
    stream.writeUnsignedBits(0, 5);
    stream.writeBooleanBit(!close);
    stream.writeUnsignedBits(endCapStyle, 2);
    if (jointStyle == JOINT_MITER) {
      stream.writeFP16(miterLimit);
    }
    if (hasFill) {
      fillStyle.write(stream);
    } else {
      startColor.write(stream);
      endColor.write(stream);
    }
  }
}

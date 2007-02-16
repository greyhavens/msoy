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
 * This class is used to define a line style. Contains line width and color.
 */
public final class LineStyle2 extends EnhancedStrokeStyle {
  private int width;
  private byte startCapStyle   = CAPS_ROUND;
  private byte endCapStyle     = CAPS_ROUND;
  private byte jointStyle      = JOINT_ROUND;
  private boolean pixelHinting;
  private boolean close        = true;
  private byte scaleStroke     = SCALE_BOTH;
  private double miterLimit    = 3;
  private RGBA color           = RGBA.BLACK;
  private FillStyle fillStyle;

  /**
   * Creates a new line style. Specify the width of the line in twips (1/20
   * px).
   *
   * @param width line width
   */
  public LineStyle2(int width) {
    this.width = width;
  }

  LineStyle2(InputBitStream stream) throws IOException {
    width           = stream.readUI16();
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
      fillStyle   = new FillStyle(stream, true);
      color       = null;
    } else {
      color = new RGBA(stream);
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
   * @param color TODO: Comments
   */
  public void setColor(RGBA color) {
    this.color = color;
  }

  /**
   * Returns the line color.
   *
   * @return line color
   */
  public RGBA getColor() {
    return color;
  }

  /**
   * TODO: Comments
   *
   * @param endCapStyle TODO: Comments
   *
   * @see EnhancedStrokeStyle
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
   * @param fillStyle TODO: Comments
   */
  public void setFillStyle(FillStyle fillStyle) {
    this.fillStyle = fillStyle;
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public FillStyle getFillStyle() {
    return fillStyle;
  }

  /**
   * TODO: Comments
   *
   * @param jointStyle TODO: Comments
   *
   * @see EnhancedStrokeStyle
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
   *
   * @see EnhancedStrokeStyle
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
   * @param width TODO: Comments
   */
  public void setWidth(int width) {
    this.width = width;
  }

  /**
   * Returns the line width in twips (1/20 px).
   *
   * @return line width in twips
   */
  public int getWidth() {
    return width;
  }

  void write(OutputBitStream stream) throws IOException {
    stream.writeUI16(width);
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
      color.write(stream);
    }
  }
}

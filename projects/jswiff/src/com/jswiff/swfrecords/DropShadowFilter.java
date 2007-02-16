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
 * TODO: Comments
 */
public final class DropShadowFilter extends Filter {
  private RGBA color;
  private double x;
  private double y;
  private double angle;
  private double distance;
  private double strength;
  private int quality;
  private boolean inner;
  private boolean knockout;
  private boolean hideObject;

  /**
   * Creates a new DropShadowFilter instance.
   */
  public DropShadowFilter() {
    initDefaults();
  }

  /**
   * Creates a new DropShadowFilter instance.
   *
   * @param stream TODO: Comments
   *
   * @throws IOException TODO: Comments
   */
  public DropShadowFilter(InputBitStream stream) throws IOException {
    color        = new RGBA(stream);
    x            = stream.readFP32();
    y            = stream.readFP32();
    angle        = stream.readFP32();
    distance     = stream.readFP32();
    strength     = stream.readFP16();
    inner        = stream.readBooleanBit();
    knockout     = stream.readBooleanBit();
    hideObject   = !stream.readBooleanBit();
    quality      = (int) stream.readUnsignedBits(5);
  }

  /**
   * TODO: Comments
   *
   * @param angle TODO: Comments
   */
  public void setAngle(double angle) {
    this.angle = angle;
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public double getAngle() {
    return angle;
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
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public RGBA getColor() {
    return color;
  }

  /**
   * TODO: Comments
   *
   * @param distance TODO: Comments
   */
  public void setDistance(double distance) {
    this.distance = distance;
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public double getDistance() {
    return distance;
  }

  /**
   * TODO: Comments
   *
   * @param hideObject TODO: Comments
   */
  public void setHideObject(boolean hideObject) {
    this.hideObject = hideObject;
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public boolean isHideObject() {
    return hideObject;
  }

  /**
   * TODO: Comments
   *
   * @param inner TODO: Comments
   */
  public void setInner(boolean inner) {
    this.inner = inner;
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public boolean isInner() {
    return inner;
  }

  /**
   * TODO: Comments
   *
   * @param knockout TODO: Comments
   */
  public void setKnockout(boolean knockout) {
    this.knockout = knockout;
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public boolean isKnockout() {
    return knockout;
  }

  /**
   * TODO: Comments
   *
   * @param quality TODO: Comments
   */
  public void setQuality(int quality) {
    this.quality = quality;
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public int getQuality() {
    return quality;
  }

  /**
   * TODO: Comments
   *
   * @param strength TODO: Comments
   */
  public void setStrength(double strength) {
    this.strength = strength;
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public double getStrength() {
    return strength;
  }

  /**
   * TODO: Comments
   *
   * @param x TODO: Comments
   */
  public void setX(double x) {
    this.x = x;
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public double getX() {
    return x;
  }

  /**
   * TODO: Comments
   *
   * @param y TODO: Comments
   */
  public void setY(double y) {
    this.y = y;
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public double getY() {
    return y;
  }

  /**
   * TODO: Comments
   *
   * @param stream TODO: Comments
   *
   * @throws IOException TODO: Comments
   */
  public void write(OutputBitStream stream) throws IOException {
    color.write(stream);
    stream.writeFP32(x);
    stream.writeFP32(y);
    stream.writeFP32(angle);
    stream.writeFP32(distance);
    stream.writeFP16(strength);
    stream.writeBooleanBit(inner);
    stream.writeBooleanBit(knockout);
    stream.writeBooleanBit(true);
    stream.writeUnsignedBits(quality, 5);
  }

  private void initDefaults() {
    color      = RGBA.BLACK;
    x          = 4;
    y          = 4;
    angle      = Math.PI / 4;
    distance   = 4;
    strength   = 1;
    quality    = 1;
  }
}

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
import java.io.Serializable;

import java.util.List;


/**
 * <p>
 * A button record defines a character to be displayed in one or more button
 * states. Each button has four states:
 * 
 * <ul>
 * <li>
 * up: the initial state of the button (e.g. when the movie starts playing)
 * </li>
 * <li>
 * over: active when mouse is moved inside the button area
 * </li>
 * <li>
 * down: active when button is clicked
 * </li>
 * <li>
 * hit: invisible state, defines the area of the button that responds to the
 * mouse
 * </li>
 * </ul>
 * 
 * The state flags indicate which states the character belongs to.
 * </p>
 * 
 * <p>
 * Further, you can specify the depth the character will we displayed at, a
 * transformation matrix and a color transform.
 * </p>
 */
public final class ButtonRecord implements Serializable {
  private boolean hitState;
  private boolean downState;
  private boolean overState;
  private boolean upState;
  private int characterId;
  private int placeDepth;
  private Matrix placeMatrix;
  private CXformWithAlpha colorTransform;
  private boolean hasBlendMode;
  private boolean hasFilters;
  private List filters;
  private short blendMode;

  /**
   * Creates a new ButtonRecord instance.
   *
   * @param characterId ID of the character to be displayed
   * @param placeDepth depth the character will be displayed at
   * @param placeMatrix transformation matrix (for placement)
   * @param upState up state flag
   * @param overState over state flag
   * @param downState down state flag
   * @param hitState hit state flag
   *
   * @throws IllegalArgumentException if no state flag is set
   */
  public ButtonRecord(
    int characterId, int placeDepth, Matrix placeMatrix, boolean upState,
    boolean overState, boolean downState, boolean hitState) {
    if (!(upState || overState || downState || hitState)) {
      throw new IllegalArgumentException(
        "At least one of the button state flags must be set!");
    }
    this.characterId   = characterId;
    this.placeDepth    = placeDepth;
    this.placeMatrix   = placeMatrix;
    this.upState       = upState;
    this.overState     = overState;
    this.downState     = downState;
    this.hitState      = hitState;
  }

  /**
   * Reads a ButtonRecord from a bit stream.
   *
   * @param stream source bit stream
   * @param hasColorTransform indicates whether a color transform is present
   *
   * @throws IOException if an I/O error has occured
   */
  public ButtonRecord(InputBitStream stream, boolean hasColorTransform)
    throws IOException {
    stream.readUnsignedBits(2);
    hasBlendMode   = stream.readBooleanBit();
    hasFilters     = stream.readBooleanBit();
    hitState       = stream.readBooleanBit();
    downState      = stream.readBooleanBit();
    overState      = stream.readBooleanBit();
    upState        = stream.readBooleanBit();
    characterId    = stream.readUI16();
    placeDepth     = stream.readUI16();
    placeMatrix    = new Matrix(stream);
    if (hasColorTransform) {
      colorTransform = new CXformWithAlpha(stream);
    }
    if (hasFilters) {
      filters = Filter.readFilters(stream);
    }
    if (hasBlendMode) {
      blendMode = stream.readUI8();
      if (blendMode == 0) {
        blendMode = BlendMode.NORMAL;
      }
    }
  }

  /**
   * TODO: Comments
   *
   * @param blendMode TODO: Comments
   */
  public void setBlendMode(short blendMode) {
    this.blendMode   = blendMode;
    hasBlendMode     = true;
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public short getBlendMode() {
    return blendMode;
  }

  /**
   * Returns the ID of the character to be displayed.
   *
   * @return character ID
   */
  public int getCharacterId() {
    return characterId;
  }

  /**
   * Sets the transform applied to the color space and the alpha channel of the
   * character to be displayed.
   *
   * @param colorTransform color transform
   */
  public void setColorTransform(CXformWithAlpha colorTransform) {
    this.colorTransform = colorTransform;
  }

  /**
   * Returns the transform applied to the color space and the alpha channel of
   * the character to be displayed.
   *
   * @return color transform
   */
  public CXformWithAlpha getColorTransform() {
    return colorTransform;
  }

  /**
   * Checks if the down state flag is checked.
   *
   * @return <code>true</code> if character is displayed in down state
   */
  public boolean isDownState() {
    return downState;
  }

  /**
   * TODO: Comments
   *
   * @param filters TODO: Comments
   */
  public void setFilters(List filters) {
    this.filters   = filters;
    hasFilters     = (filters != null);
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public List getFilters() {
    return filters;
  }

  /**
   * Checks if the hit state flag is checked.
   *
   * @return <code>true</code> if character is displayed in hit state
   */
  public boolean isHitState() {
    return hitState;
  }

  /**
   * Checks if the over state flag is checked.
   *
   * @return <code>true</code> if character is displayed in over state
   */
  public boolean isOverState() {
    return overState;
  }

  /**
   * Returns the depth the character is displayed at.
   *
   * @return place depth
   */
  public int getPlaceDepth() {
    return placeDepth;
  }

  /**
   * Returns the transformation matrix used when placing the character.
   *
   * @return place matrix
   */
  public Matrix getPlaceMatrix() {
    return placeMatrix;
  }

  /**
   * Checks if the up state flag is checked.
   *
   * @return <code>true</code> if character is displayed in up state
   */
  public boolean isUpState() {
    return upState;
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public boolean hasBlendMode() {
    return hasBlendMode;
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public boolean hasFilters() {
    return hasFilters;
  }

  /**
   * Writes the button record to a bit stream.
   *
   * @param stream target bit stream
   * @param hasColorTransform indicates whether a color transform is present
   *
   * @throws IOException if an I/O error has occured
   */
  public void write(OutputBitStream stream, boolean hasColorTransform)
    throws IOException {
    stream.writeUnsignedBits(0, 2);
    stream.writeBooleanBit(hasBlendMode);
    stream.writeBooleanBit(hasFilters);
    stream.writeBooleanBit(hitState);
    stream.writeBooleanBit(downState);
    stream.writeBooleanBit(overState);
    stream.writeBooleanBit(upState);
    stream.writeUI16(characterId);
    stream.writeUI16(placeDepth);
    placeMatrix.write(stream);
    if (hasColorTransform) {
      if (colorTransform != null) {
        colorTransform.write(stream);
      } else {
        new CXformWithAlpha().write(stream);
      }
    }
    if (hasFilters) {
      Filter.writeFilters(filters, stream);
    }
    if (hasBlendMode) {
      stream.writeUI8(blendMode);
    }
  }
}

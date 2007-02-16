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


/**
 * This class is used to define fill styles used in a morph sequence (as array
 * within a <code>DefineMorphShape</code> tag. Three basic types of shape
 * fills are available:
 * 
 * <ul>
 * <li>
 * solid fill (with / without transparency)
 * </li>
 * <li>
 * gradient fill (linear / radial)
 * </li>
 * <li>
 * bitmap fill (clipped / tiled, smoothed / non-smoothed)
 * </li>
 * </ul>
 * 
 *
 * @see MorphFillStyles
 * @see com.jswiff.swfrecords.tags.DefineMorphShape
 * @see FillStyle
 */
public final class MorphFillStyle implements Serializable {
  /** Solid fill type */
  public static final short TYPE_SOLID                      = 0x00;
  /** Linear gradient fill type */
  public static final short TYPE_LINEAR_GRADIENT            = 0x10;
  /** Radial gradient fill type */
  public static final short TYPE_RADIAL_GRADIENT            = 0x12;
  /** Focal radial gradient fill type */
  public static final short TYPE_FOCAL_RADIAL_GRADIENT      = 0x13;
  /** Tiled bitmap fill type */
  public static final short TYPE_TILED_BITMAP               = 0x40;
  /** Clipped bitmap fill type */
  public static final short TYPE_CLIPPED_BITMAP             = 0x41;
  /** Nonsmoothed tiled bitmap fill type */
  public static final short TYPE_NONSMOOTHED_TILED_BITMAP   = 0x42;
  /** Nonsmoothed clipped bitmap fill type */
  public static final short TYPE_NONSMOOTHED_CLIPPED_BITMAP = 0x43;
  private short type;
  private RGBA startColor;
  private RGBA endColor;
  private Matrix startGradientMatrix;
  private Matrix endGradientMatrix;
  private MorphGradient gradient;
  private int bitmapId;
  private Matrix startBitmapMatrix;
  private Matrix endBitmapMatrix;

  /**
   * Creates a new solid morph fill style. Specify fill colors with
   * transparency (RGBA) for start and end state.
   *
   * @param startColor start color
   * @param endColor end color
   */
  public MorphFillStyle(RGBA startColor, RGBA endColor) {
    this.startColor   = startColor;
    this.endColor     = endColor;
    type              = TYPE_SOLID;
  }

  /**
   * <p>
   * Creates a new gradient morph fill style.
   * </p>
   * 
   * <p>
   * Shapes can be filled with different gradients in the morph's start and end
   * state. A gradient contains several control points; the fill color is
   * interpolated between these point's colors. Start and end gradients must
   * have the same number of control points. Control points of start and end
   * gradients as well as their colors are defined in a
   * <code>MorphGradient</code> instance.
   * </p>
   * 
   * <p>
   * Gradients are defined in the <i>gradient square</i>: (-16384, -16384,
   * 16384, 16384). The gradient matrix is used to map the gradient from the
   * gradient square to the display surface. Supply gradient matrices for
   * start and end state.
   * </p>
   * 
   * <p>
   * Linear and circular gradients are supported. Use either
   * <code>TYPE_LINEAR_GRADIENT</code> or <code>TYPE_CIRCULAR_GRADIENT</code>
   * as gradient type.
   * </p>
   *
   * @param gradient a morph gradient
   * @param startGradientMatrix start gradient matrix
   * @param endGradientMatrix end gradient matrix
   * @param type gradient type
   *
   * @throws IllegalArgumentException if specified gradient type is not
   *         supported
   */
  public MorphFillStyle(
    MorphGradient gradient, Matrix startGradientMatrix, Matrix endGradientMatrix,
    short type) {
    if (
      (type != TYPE_LINEAR_GRADIENT) && (type != TYPE_RADIAL_GRADIENT) &&
          (type != TYPE_FOCAL_RADIAL_GRADIENT)) {
      throw new IllegalArgumentException("Illegal gradient type!");
    }
    this.gradient              = gradient;
    this.startGradientMatrix   = startGradientMatrix;
    this.endGradientMatrix     = endGradientMatrix;
    this.type                  = type;
  }

  /**
   * Creates a new bitmap morph fill style. You have to specify the character
   * ID of a previously defined bitmap, two transform matrices used for
   * mapping the bitmap to the filled (start and end) shapes, and the bitmap
   * type (one of the constants <code>TYPE_TILED_BITMAP</code>,
   * <code>TYPE_CLIPPED_BITMAP</code>,
   * <code>TYPE_NONSMOOTHED_TILED_BITMAP</code> or
   * <code>TYPE_NONSMOOTHED_CLIPPED_BITMAP</code>).
   *
   * @param bitmapId character ID of the bitmap
   * @param startBitmapMatrix transform matrix for start state
   * @param endBitmapMatrix transform matrix for end state
   * @param type bitmap type
   *
   * @throws IllegalArgumentException if an illegal bitmap type has been
   *         specified
   */
  public MorphFillStyle(
    int bitmapId, Matrix startBitmapMatrix, Matrix endBitmapMatrix, short type) {
    if (
      (type != TYPE_TILED_BITMAP) && (type != TYPE_CLIPPED_BITMAP) &&
          (type != TYPE_NONSMOOTHED_TILED_BITMAP) &&
          (type != TYPE_NONSMOOTHED_CLIPPED_BITMAP)) {
      throw new IllegalArgumentException("Illegal bitmap type");
    }
    this.bitmapId            = bitmapId;
    this.startBitmapMatrix   = startBitmapMatrix;
    this.endBitmapMatrix     = endBitmapMatrix;
    this.type                = type;
  }

  MorphFillStyle(InputBitStream stream) throws IOException {
    type = stream.readUI8();
    switch (type) {
      case TYPE_SOLID:
        startColor = new RGBA(stream);
        endColor = new RGBA(stream);
        break;
      case TYPE_LINEAR_GRADIENT:
      case TYPE_RADIAL_GRADIENT:
        startGradientMatrix = new Matrix(stream);
        endGradientMatrix = new Matrix(stream);
        gradient = new MorphGradient(stream);
        break;
      case TYPE_FOCAL_RADIAL_GRADIENT:
        startGradientMatrix = new Matrix(stream);
        endGradientMatrix = new Matrix(stream);
        gradient = new FocalMorphGradient(stream);
        break;
      case TYPE_TILED_BITMAP:
      case TYPE_CLIPPED_BITMAP:
      case TYPE_NONSMOOTHED_TILED_BITMAP:
      case TYPE_NONSMOOTHED_CLIPPED_BITMAP:
        bitmapId = stream.readUI16();
        startBitmapMatrix = new Matrix(stream);
        endBitmapMatrix = new Matrix(stream);
        break;
      default:
        throw new IOException("Illegal morph fill type: " + type);
    }
  }

  /**
   * Returns the character ID of the bitmap used for filling. Supported only
   * for bitmap fills.
   *
   * @return bitmap character ID
   */
  public int getBitmapId() {
    return bitmapId;
  }

  /**
   * Returns the bitmap matrix used to map the bitmap to the filled shape in
   * the end state of the morph sequence. Supported only for bitmap fills.
   *
   * @return end bitmap matrix
   */
  public Matrix getEndBitmapMatrix() {
    return endBitmapMatrix;
  }

  /**
   * Returns the fill color of the shape in the end state of the morph
   * sequence. Supported only for solid fills.
   *
   * @return end color
   */
  public RGBA getEndColor() {
    return endColor;
  }

  /**
   * Returns the transform matrix used to map the gradient from the gradient
   * square to the display surface in the end state of the morph sequence.
   * Supported only for gradient fills.
   *
   * @return end gradient matrix
   */
  public Matrix getEndGradientMatrix() {
    return endGradientMatrix;
  }

  /**
   * Returns the morph gradient used for filling. Contains gradient control
   * points as well as their colors, both for start and end morph state.
   * Supported only for gradient fills.
   *
   * @return morph gradient
   */
  public MorphGradient getGradient() {
    return gradient;
  }

  /**
   * Returns the bitmap matrix used to map the bitmap to the filled shape in
   * the start state of the morph sequence. Supported only for bitmap fills.
   *
   * @return start bitmap matrix
   */
  public Matrix getStartBitmapMatrix() {
    return startBitmapMatrix;
  }

  /**
   * Returns the fill color of the shape in the start state of the morph
   * sequence. Supported only for solid fills.
   *
   * @return start color
   */
  public RGBA getStartColor() {
    return startColor;
  }

  /**
   * Returns the transform matrix used to map the gradient from the gradient
   * square to the display surface in the start state of the morph sequence.
   * Supported only for gradient fills.
   *
   * @return start gradient matrix
   */
  public Matrix getStartGradientMatrix() {
    return startGradientMatrix;
  }

  /**
   * Returns the type of the morph fill style (one of the <code>TYPE_...</code>
   * constants).
   *
   * @return fill type
   */
  public short getType() {
    return type;
  }

  void write(OutputBitStream stream) throws IOException {
    stream.writeUI8(type);
    switch (type) {
      case TYPE_SOLID:
        startColor.write(stream);
        endColor.write(stream);
        break;
      case TYPE_LINEAR_GRADIENT:
      case TYPE_RADIAL_GRADIENT:
      case TYPE_FOCAL_RADIAL_GRADIENT:
        startGradientMatrix.write(stream);
        endGradientMatrix.write(stream);
        gradient.write(stream);
        break;
      case TYPE_TILED_BITMAP:
      case TYPE_CLIPPED_BITMAP:
      case TYPE_NONSMOOTHED_TILED_BITMAP:
      case TYPE_NONSMOOTHED_CLIPPED_BITMAP:
        stream.writeUI16(bitmapId);
        startBitmapMatrix.write(stream);
        endBitmapMatrix.write(stream);
    }
  }
}

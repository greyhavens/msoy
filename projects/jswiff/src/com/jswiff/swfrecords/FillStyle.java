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
 * This class is used to define a fill style. Three basic types of shape fills
 * are available:
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
 */
public final class FillStyle implements Serializable {
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
  private Color color;
  private Matrix gradientMatrix;
  private Gradient gradient;
  private int bitmapId;
  private Matrix bitmapMatrix;

  /**
   * <p>
   * Creates a new gradient fill style. You have to specify a gradient, a
   * gradient matrix and a gradient type.
   * </p>
   * 
   * <p>
   * The gradient contains several control points. The fill color is
   * interpolated between these point's colors.
   * </p>
   * 
   * <p>
   * Gradients are defined in the <i>gradient square</i>: (-16384, -16384,
   * 16384, 16384). The gradient matrix is used to map the gradient from the
   * gradient square to the display surface.
   * </p>
   * 
   * <p>
   * Linear and circular gradients are supported. Use either
   * <code>TYPE_LINEAR_GRADIENT</code> or <code>TYPE_CIRCULAR_GRADIENT</code>
   * as gradient type.
   * </p>
   *
   * @param gradient a gradient
   * @param gradientMatrix gradient matrix
   * @param type gradient type
   *
   * @throws IllegalArgumentException if specified gradient type is not
   *         supported
   */
  public FillStyle(Gradient gradient, Matrix gradientMatrix, short type) {
    if (
      (type != TYPE_LINEAR_GRADIENT) && (type != TYPE_RADIAL_GRADIENT) &&
          (type != TYPE_FOCAL_RADIAL_GRADIENT)) {
      throw new IllegalArgumentException("Illegal gradient type!");
    }
    this.type             = type;
    this.gradient         = gradient;
    this.gradientMatrix   = gradientMatrix;
  }

  /**
   * Creates a new solid fill style. Specify a fill color. This can be either
   * an <code>RGBA</code> or an <code>RGB instance</code>, depending on
   * whether the tag the style is contained in supports transparency or not.
   * <code>DefineShape3</code> supports transparency.
   *
   * @param color fill color
   *
   * @see com.jswiff.swfrecords.tags.DefineShape3
   */
  public FillStyle(Color color) {
    this.color   = color;
    type         = TYPE_SOLID;
  }

  /**
   * Creates a new bitmap fill style. You have to specify the character ID of a
   * previously defined bitmap, a transform matrix used for mapping the bitmap
   * to the filled shape, and the bitmap type (one of the constants
   * <code>TYPE_TILED_BITMAP</code>, <code>TYPE_CLIPPED_BITMAP</code>,
   * <code>TYPE_NONSMOOTHED_TILED_BITMAP</code> or
   * <code>TYPE_NONSMOOTHED_CLIPPED_BITMAP</code>).
   *
   * @param bitmapId character ID of the bitmap
   * @param bitmapMatrix transform matrix
   * @param type bitmap type
   *
   * @throws IllegalArgumentException if an illegal bitmap type has been
   *         specified
   */
  public FillStyle(int bitmapId, Matrix bitmapMatrix, short type) {
    if (
      (type != TYPE_TILED_BITMAP) && (type != TYPE_CLIPPED_BITMAP) &&
          (type != TYPE_NONSMOOTHED_TILED_BITMAP) &&
          (type != TYPE_NONSMOOTHED_CLIPPED_BITMAP)) {
      throw new IllegalArgumentException("Illegal bitmap type");
    }
    this.bitmapId       = bitmapId;
    this.bitmapMatrix   = bitmapMatrix;
    this.type           = type;
  }

  FillStyle(InputBitStream stream, boolean hasAlpha) throws IOException {
    type = stream.readUI8();
    switch (type) {
      case TYPE_SOLID:
        if (hasAlpha) {
          color = new RGBA(stream);
        } else {
          color = new RGB(stream);
        }
        break;
      case TYPE_LINEAR_GRADIENT:
      case TYPE_RADIAL_GRADIENT:
        gradientMatrix = new Matrix(stream);
        gradient = new Gradient(stream, hasAlpha);
        break;
      case TYPE_FOCAL_RADIAL_GRADIENT:
        gradientMatrix = new Matrix(stream);
        gradient = new FocalGradient(stream);
        break;
      case TYPE_TILED_BITMAP:
      case TYPE_CLIPPED_BITMAP:
      case TYPE_NONSMOOTHED_TILED_BITMAP:
      case TYPE_NONSMOOTHED_CLIPPED_BITMAP:
        bitmapId = stream.readUI16();
        bitmapMatrix = new Matrix(stream);
        break;
      default:
        throw new IOException("Illegal fill type: " + type);
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
   * Returns the bitmap matrix used to map the bitmap to the filled shape.
   * Supported only for bitmap fills.
   *
   * @return bitmap matrix
   */
  public Matrix getBitmapMatrix() {
    return bitmapMatrix;
  }

  /**
   * Returns the fill color. Supported only for solid fills.
   *
   * @return Returns the color.
   */
  public Color getColor() {
    return color;
  }

  /**
   * Returns the gradient used for filling. Supported only for gradient fills.
   *
   * @return fill gradient
   */
  public Gradient getGradient() {
    return gradient;
  }

  /**
   * Returns the transform matrix used to map the gradient from the gradient
   * square to the display surface. Supported only for gradient fills.
   *
   * @return gradient matrix
   */
  public Matrix getGradientMatrix() {
    return gradientMatrix;
  }

  /**
   * Returns the type of the fill style (one of the <code>TYPE_...</code>
   * constants)
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
        color.write(stream);
        break;
      case TYPE_LINEAR_GRADIENT:
      case TYPE_RADIAL_GRADIENT:
      case TYPE_FOCAL_RADIAL_GRADIENT:
        gradientMatrix.write(stream);
        gradient.write(stream);
        break;
      case TYPE_TILED_BITMAP:
      case TYPE_CLIPPED_BITMAP:
      case TYPE_NONSMOOTHED_TILED_BITMAP:
      case TYPE_NONSMOOTHED_CLIPPED_BITMAP:
        stream.writeUI16(bitmapId);
        bitmapMatrix.write(stream);
    }
  }
}

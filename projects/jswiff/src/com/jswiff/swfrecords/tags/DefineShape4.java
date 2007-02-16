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
import com.jswiff.swfrecords.LineStyle2;
import com.jswiff.swfrecords.LineStyleArray;
import com.jswiff.swfrecords.Rect;
import com.jswiff.swfrecords.ShapeWithStyle;

import java.io.IOException;


/**
 * Used to define shapes, similar to <code>DefineShape</code>,
 * <code>DefineShape2</code> and <code>DefineShape3</code>. Unlike older shape
 * definition tags, <code>DefineShape4</code> can contain
 * <code>LineStyle2</code> structures. Additionally, it allows to define edge
 * bounds and to specify flags for stroke hinting.
 *
 * @see com.jswiff.swfrecords.tags.DefineShape
 * @see com.jswiff.swfrecords.tags.DefineShape2
 * @see com.jswiff.swfrecords.tags.DefineShape3
 * @see com.jswiff.swfrecords.LineStyle2
 * @since SWF 8
 */
public final class DefineShape4 extends DefinitionTag {
  private Rect shapeBounds;
  private Rect edgeBounds;
  private ShapeWithStyle shapes;
  private boolean hasScalingStrokes;
  private boolean hasNonscalingStrokes;

  /**
   * Creates a new DefineShape4 tag. Supply the character ID of the shape, its
   * shape and edge bounding box and its primitives and styles.
   *
   * @param characterId character ID of shape
   * @param shapeBounds bounding box of shape
   * @param edgeBounds edge bounding box
   * @param shapes shape's primitives and styles
   */
  public DefineShape4(
    int characterId, Rect shapeBounds, Rect edgeBounds, ShapeWithStyle shapes) {
    code               = TagConstants.DEFINE_SHAPE_4;
    this.characterId   = characterId;
    this.shapeBounds   = shapeBounds;
    this.edgeBounds    = edgeBounds;
    this.shapes        = shapes;
  }

  DefineShape4() {
    // empty
  }

  /**
   * TODO: Comments
   *
   * @param edgeBounds TODO: Comments
   */
  public void setEdgeBounds(Rect edgeBounds) {
    this.edgeBounds = edgeBounds;
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public Rect getEdgeBounds() {
    return edgeBounds;
  }

  /**
   * Sets the bounding box of the shape, i.e. the rectangle that completely
   * encloses it.
   *
   * @param shapeBounds shape's bounds
   */
  public void setShapeBounds(Rect shapeBounds) {
    this.shapeBounds = shapeBounds;
  }

  /**
   * Returns the bounding box of the shape, i.e. the rectangle that completely
   * encloses it.
   *
   * @return shape's bounds
   */
  public Rect getShapeBounds() {
    return shapeBounds;
  }

  /**
   * Sets the shape's primitives and styles (i.e. lines and curves) in a
   * <code>ShapeWithStyle</code> instance.
   *
   * @param shapes shape's primitives and styles
   */
  public void setShapes(ShapeWithStyle shapes) {
    this.shapes = shapes;
  }

  /**
   * Returns the shape's primitives and styles (i.e. lines and curves) in a
   * <code>ShapeWithStyle</code> instance.
   *
   * @return shape's primitives and styles
   */
  public ShapeWithStyle getShapes() {
    return shapes;
  }

  protected void writeData(OutputBitStream outStream) throws IOException {
    outStream.writeUI16(characterId);
    shapeBounds.write(outStream);
    edgeBounds.write(outStream);
    outStream.writeUnsignedBits(0, 6);
    checkStrokeScaling();
    outStream.writeBooleanBit(hasNonscalingStrokes);
    outStream.writeBooleanBit(hasScalingStrokes);
    shapes.write(outStream);
  }

  void setData(byte[] data) throws IOException {
    InputBitStream inStream = new InputBitStream(data);
    characterId   = inStream.readUI16();
    shapeBounds   = new Rect(inStream);
    edgeBounds    = new Rect(inStream);
    inStream.readUI8(); // 6 reserved bits and 2 flags we can ignore
    shapes = new ShapeWithStyle(inStream);
  }

  private void checkStrokeScaling() {
    if (shapes == null) {
      return;
    }
    hasNonscalingStrokes   = false;
    hasScalingStrokes      = false;
    LineStyleArray lineStyles = shapes.getLineStyles();
    for (int i = 1; i <= lineStyles.getSize(); i++) {
      LineStyle2 style = (LineStyle2) lineStyles.getStyle(i);
      if (style.getScaleStroke() == LineStyle2.SCALE_NONE) {
        hasNonscalingStrokes = true;
      } else {
        hasScalingStrokes = true;
      }
      if (hasNonscalingStrokes && hasScalingStrokes) {
        break;
      }
    }
  }
}

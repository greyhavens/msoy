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
import com.jswiff.swfrecords.EdgeRecord;
import com.jswiff.swfrecords.LineStyle2;
import com.jswiff.swfrecords.MorphFillStyles;
import com.jswiff.swfrecords.MorphLineStyle2;
import com.jswiff.swfrecords.MorphLineStyles;
import com.jswiff.swfrecords.Rect;
import com.jswiff.swfrecords.Shape;
import com.jswiff.swfrecords.ShapeRecord;

import java.io.IOException;


/**
 * DOCUMENT ME!
 *
 * @since SWF 8
 */
public final class DefineMorphShape2 extends DefinitionTag {
  private Rect startShapeBounds;
  private Rect endShapeBounds;
  private Rect startEdgeBounds;
  private Rect endEdgeBounds;
  private MorphFillStyles morphFillStyles;
  private MorphLineStyles morphLineStyles;
  private Shape startShape;
  private Shape endShape;
  private boolean hasNonscalingStrokes;
  private boolean hasScalingStrokes;

  /**
   * Creates a new DefineMorphShape2 instance.
   *
   * @param characterId TODO: Comments
   * @param startShapeBounds TODO: Comments
   * @param endShapeBounds TODO: Comments
   * @param startEdgeBounds TODO: Comments
   * @param endEdgeBounds TODO: Comments
   * @param morphFillStyles TODO: Comments
   * @param morphLineStyles TODO: Comments
   * @param startShape TODO: Comments
   * @param endShape TODO: Comments
   *
   * @throws IllegalArgumentException TODO: Comments
   */
  public DefineMorphShape2(
    int characterId, Rect startShapeBounds, Rect endShapeBounds,
    Rect startEdgeBounds, Rect endEdgeBounds, MorphFillStyles morphFillStyles,
    MorphLineStyles morphLineStyles, Shape startShape, Shape endShape)
    throws IllegalArgumentException {
    code                    = TagConstants.DEFINE_MORPH_SHAPE_2;
    this.characterId        = characterId;
    this.startShapeBounds   = startShapeBounds;
    this.endShapeBounds     = endShapeBounds;
    this.startEdgeBounds    = startEdgeBounds;
    this.endEdgeBounds      = endEdgeBounds;
    this.morphFillStyles    = morphFillStyles;
    this.morphLineStyles    = morphLineStyles;
    checkEdges(startShape, endShape);
    this.startShape   = startShape;
    this.endShape     = endShape;
  }

  DefineMorphShape2() {
    // empty
  }

  /**
   * TODO: Comments
   *
   * @param endEdgeBounds TODO: Comments
   */
  public void setEndEdgeBounds(Rect endEdgeBounds) {
    this.endEdgeBounds = endEdgeBounds;
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public Rect getEndEdgeBounds() {
    return endEdgeBounds;
  }

  /**
   * Sets the shape displayed in the final state of the morph sequence.
   *
   * @param endShape end shape
   */
  public void setEndShape(Shape endShape) {
    this.endShape = endShape;
  }

  /**
   * Returns the shape displayed in the final state of the morph sequence.
   *
   * @return end shape
   */
  public Shape getEndShape() {
    return endShape;
  }

  /**
   * Sets the bounding box of the end shape.
   *
   * @param endBounds end shape bounds
   */
  public void setEndShapeBounds(Rect endBounds) {
    this.endShapeBounds = endBounds;
  }

  /**
   * Returns the bounding box of the end shape.
   *
   * @return end shape bounds
   */
  public Rect getEndShapeBounds() {
    return endShapeBounds;
  }

  /**
   * Sets the fill styles of the morph sequence.
   *
   * @param morphFillStyles morph fill styles
   */
  public void setMorphFillStyles(MorphFillStyles morphFillStyles) {
    this.morphFillStyles = morphFillStyles;
  }

  /**
   * Returns the fill styles of the morph sequence.
   *
   * @return morph fill styles
   */
  public MorphFillStyles getMorphFillStyles() {
    return morphFillStyles;
  }

  /**
   * Sets the line styles of the morph sequence.
   *
   * @param morphLineStyles morph line styles
   */
  public void setMorphLineStyles(MorphLineStyles morphLineStyles) {
    this.morphLineStyles = morphLineStyles;
  }

  /**
   * Returns the line styles of the morph sequence.
   *
   * @return morph line styles
   */
  public MorphLineStyles getMorphLineStyles() {
    return morphLineStyles;
  }

  /**
   * TODO: Comments
   *
   * @param startEdgeBounds TODO: Comments
   */
  public void setStartEdgeBounds(Rect startEdgeBounds) {
    this.startEdgeBounds = startEdgeBounds;
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public Rect getStartEdgeBounds() {
    return startEdgeBounds;
  }

  /**
   * Sets the shape displayed in the initial state of the morph sequence.
   *
   * @param startShape start shape
   */
  public void setStartShape(Shape startShape) {
    this.startShape = startShape;
  }

  /**
   * Returns the shape displayed in the initial state of the morph sequence.
   *
   * @return start shape
   */
  public Shape getStartShape() {
    return startShape;
  }

  /**
   * Sets the bounding box of the start shape.
   *
   * @param startBounds start shape bounds
   */
  public void setStartShapeBounds(Rect startBounds) {
    this.startShapeBounds = startBounds;
  }

  /**
   * Returns the bounding box of the start shape.
   *
   * @return start shape bounds
   */
  public Rect getStartShapeBounds() {
    return startShapeBounds;
  }

  protected void writeData(OutputBitStream outStream) throws IOException {
    outStream.writeUI16(characterId);
    startShapeBounds.write(outStream);
    endShapeBounds.write(outStream);
    startEdgeBounds.write(outStream);
    endEdgeBounds.write(outStream);
    outStream.writeUnsignedBits(0, 6);
    checkStrokeScaling();
    outStream.writeBooleanBit(hasNonscalingStrokes);
    outStream.writeBooleanBit(hasScalingStrokes);
    if (
      (startShape == null) && (endShape == null) && (morphFillStyles == null) &&
          (morphLineStyles == null)) {
      // zero offset "feature"
      outStream.writeUI32(0); // zero offset
      outStream.writeUI16(0); // two zeroes for empty styles 
      outStream.writeUI32(0); // four zeroes for empty shapes 
      return;
    }
    OutputBitStream bitStream = new OutputBitStream();
    morphFillStyles.write(bitStream);
    morphLineStyles.write(bitStream);
    startShape.write(bitStream);
    byte[] bitStreamData = bitStream.getData();
    outStream.writeUI32(bitStreamData.length); // offset to endShape
    outStream.writeBytes(bitStreamData);
    endShape.write(outStream);
  }

  void setData(byte[] data) throws IOException {
    InputBitStream inStream = new InputBitStream(data);
    characterId        = inStream.readUI16();
    startShapeBounds   = new Rect(inStream);
    endShapeBounds     = new Rect(inStream);
    startEdgeBounds    = new Rect(inStream);
    endEdgeBounds      = new Rect(inStream);
    inStream.readUI8(); // ignore stroke scaling flags
    long endEdgesOffset = inStream.readUI32();
    if (endEdgesOffset == 0) {
      // the Flash authoring tool sometimes generates such morphs
      return;
    }
    endEdgesOffset += inStream.getOffset();
    morphFillStyles   = new MorphFillStyles(inStream);
    morphLineStyles   = new MorphLineStyles(inStream, true);
    long startEdgesOffset = inStream.getOffset();
    byte[] startEdgesBuffer = new byte[(int) (endEdgesOffset -
      startEdgesOffset)];
    System.arraycopy(
      data, (int) startEdgesOffset, startEdgesBuffer, 0, startEdgesBuffer.length);
    startShape = new Shape(new InputBitStream(startEdgesBuffer));
    byte[] endEdgesBuffer = new byte[(int) (data.length - endEdgesOffset)];
    System.arraycopy(
      data, (int) endEdgesOffset, endEdgesBuffer, 0, endEdgesBuffer.length);
    endShape = new Shape(new InputBitStream(endEdgesBuffer));
  }

  private void checkEdges(Shape edges1, Shape edges2) {
    if ((edges1 == null) || (edges2 == null)) {
      return; // zero offset bug
    }
    ShapeRecord[] startShapeRecs = edges1.getShapeRecords();
    ShapeRecord[] endShapeRecs   = edges1.getShapeRecords();
    if (startShapeRecs.length != endShapeRecs.length) {
      throw new IllegalArgumentException(
        "Start and end shapes must have the same number of shape records!");
    }
    for (int i = 0; i < startShapeRecs.length; i++) {
      ShapeRecord startRec = startShapeRecs[i];
      ShapeRecord endRec   = endShapeRecs[i];
      if (startRec instanceof EdgeRecord) {
        if (endRec instanceof EdgeRecord) {
          continue;
        }
        throw new IllegalArgumentException(
          "Edge record in start shape must have corresponding record in end shape!");
      }
      if (!(endRec instanceof EdgeRecord)) {
        continue;
      }
      throw new IllegalArgumentException(
        "Style change record in start shape must have corresponding record in end shape!");
    }
  }

  private void checkStrokeScaling() {
    hasNonscalingStrokes   = false;
    hasScalingStrokes      = false;
    if (morphLineStyles == null) {
      return;
    }
    for (int i = 1; i <= morphLineStyles.getSize(); i++) {
      MorphLineStyle2 style = (MorphLineStyle2) morphLineStyles.getStyle(i);
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

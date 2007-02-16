/*
 * JSwiff is an open source Java API for Macromedia Flash file generation
 * and manipulation
 *
 * Copyright (C) 2004-2005 Ralf Terdic (contact@jswiff.com)
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
import com.jswiff.swfrecords.MorphFillStyles;
import com.jswiff.swfrecords.MorphLineStyles;
import com.jswiff.swfrecords.Rect;
import com.jswiff.swfrecords.Shape;
import com.jswiff.swfrecords.ShapeRecord;

import java.io.IOException;


/**
 * <p>
 * This tag is used to define the start and end states of a morph sequence.
 * After definition, a snapshot of the sequence can be displayed with the
 * <code>PlaceObject2</code> tag. Use several <code>PlaceObject2</code> tags
 * with <code>ratio</code> value increasing from 0 to 65535 to achieve a
 * smooth rendering of the morph. This is all you have to do to define and
 * render a morph. Flash Player is resposible for generating intermediary
 * states through interpolation.
 * </p>
 * 
 * <p>
 * Shapes belonging to a morph sequence are defined within a single
 * <code>DefineMorphShape</code> tag and are independent of previously defined
 * shapes. Accordingly, character definitions preceding this tag cannot be
 * used.
 * </p>
 *
 * @see PlaceObject2
 * @since SWF 3
 */
public final class DefineMorphShape extends DefinitionTag {
	private Rect startBounds;
	private Rect endBounds;
	private MorphFillStyles morphFillStyles;
	private MorphLineStyles morphLineStyles;
	private Shape startShape;
	private Shape endShape;

	/**
	 * <p>
	 * Creates a new DefineMorphShape tag. Supply the character ID of the morph
	 * sequence, bounding boxes for the shapes at start and end of morph, and
	 * morph fill and line styles. Finally, provide the start and the end
	 * shape.
	 * </p>
	 * 
	 * <p>
	 * The shapes must have identical structures, i.e. a style change record in
	 * the start shape must have a corresponding style change record in the
	 * end shape. Edge records in the start shape must have matching edge
	 * records in the end shape. The edge record type does not matter, since
	 * straight edge records can be regarded as special cases of curved edge
	 * records.
	 * </p>
	 *
	 * @param characterId character ID if morph sequence
	 * @param startBounds bounding box at morph start
	 * @param endBounds bounding box at morph end
	 * @param morphFillStyles array of fill styles used in morph sequence
	 * @param morphLineStyles array of line styles used in morph sequence
	 * @param startShape start shape
	 * @param endShape end shape
	 *
	 * @throws IllegalArgumentException if start and end shapes are differently
	 * 		   structured
	 */
	public DefineMorphShape(
		int characterId, Rect startBounds, Rect endBounds,
		MorphFillStyles morphFillStyles, MorphLineStyles morphLineStyles,
		Shape startShape, Shape endShape) throws IllegalArgumentException {
		code					 = TagConstants.DEFINE_MORPH_SHAPE;
		this.characterId		 = characterId;
		this.startBounds		 = startBounds;
		this.endBounds			 = endBounds;
		this.morphFillStyles     = morphFillStyles;
		this.morphLineStyles     = morphLineStyles;
		checkEdges(startShape, endShape);
		this.startShape     = startShape;
		this.endShape	    = endShape;
	}

	DefineMorphShape() {
		// empty
	}

	/**
	 * Sets the bounding box of the end shape.
	 *
	 * @param endBounds end shape bounds
	 */
	public void setEndBounds(Rect endBounds) {
		this.endBounds = endBounds;
	}

	/**
	 * Returns the bounding box of the end shape.
	 *
	 * @return end shape bounds
	 */
	public Rect getEndBounds() {
		return endBounds;
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
	 * Sets the bounding box of the start shape.
	 *
	 * @param startBounds start shape bounds
	 */
	public void setStartBounds(Rect startBounds) {
		this.startBounds = startBounds;
	}

	/**
	 * Returns the bounding box of the start shape.
	 *
	 * @return start shape bounds
	 */
	public Rect getStartBounds() {
		return startBounds;
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

	protected void writeData(OutputBitStream outStream)
		throws IOException {
		outStream.writeUI16(characterId);
		startBounds.write(outStream);
		endBounds.write(outStream);
		if (
			(startShape == null) && (endShape == null) &&
				(morphFillStyles == null) && (morphLineStyles == null)) {
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
		characterId     = inStream.readUI16();
		startBounds     = new Rect(inStream);
		endBounds	    = new Rect(inStream);
		long endEdgesOffset = inStream.readUI32();
		if (endEdgesOffset == 0) {
			// the Flash authoring tool sometimes generates such morphs
			return;
		}
		endEdgesOffset += inStream.getOffset();
		morphFillStyles     = new MorphFillStyles(inStream);
		morphLineStyles     = new MorphLineStyles(inStream, false);
		long startEdgesOffset = inStream.getOffset();
		byte[] startEdgesBuffer = new byte[(int) (endEdgesOffset -
			startEdgesOffset)];
		System.arraycopy(
			data, (int) startEdgesOffset, startEdgesBuffer, 0,
			startEdgesBuffer.length);
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
}

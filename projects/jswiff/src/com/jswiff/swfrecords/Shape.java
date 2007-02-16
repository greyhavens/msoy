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

package com.jswiff.swfrecords;

import com.jswiff.io.InputBitStream;
import com.jswiff.io.OutputBitStream;

import java.io.IOException;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


/**
 * This class is used for defining shapes, e.g. by the <code>DefineFont</code>
 * tag in order to define the character glyphs of a font, or within a
 * <code>DefineMorphShape</code> tag. Shapes contain one or more
 * <code>ShapeRecord</code> instances which define style changes and
 * primitives as lines and curves.
 *
 * @see com.jswiff.swfrecords.tags.DefineFont
 * @see com.jswiff.swfrecords.tags.DefineMorphShape
 */
public class Shape implements Serializable {
  // Byte number of fill/line styles - these are used as initial values for parsing,
  // StyleChangeRecords may contain new values used for further parsing.
  // However, these values aren't overwritten!
  protected byte numFillBits;
  protected byte numLineBits;
  protected ShapeRecord[] shapeRecords;

  /**
   * Creates a new Shape instance.
   *
   * @param shapeRecords an array of one or more shape records
   */
  public Shape(ShapeRecord[] shapeRecords) {
    this.shapeRecords = shapeRecords;
  }

  /*
   */
  public Shape(InputBitStream stream) throws IOException {
    read(stream, false, false);
  }

  Shape() {
    // empty
  }

  /**
   * Returns the contained shape record array.
   *
   * @return shape records
   */
  public ShapeRecord[] getShapeRecords() {
    return shapeRecords;
  }

  /*
   */
  public void write(OutputBitStream stream) throws IOException {
    computeNumBits();
    byte currentNumFillBits = numFillBits;
    byte currentNumLineBits = numLineBits;
    stream.writeUnsignedBits(currentNumFillBits, 4);
    stream.writeUnsignedBits(currentNumLineBits, 4);
    for (int i = 0; i < shapeRecords.length; i++) {
      ShapeRecord record = shapeRecords[i];
      if (record instanceof StyleChangeRecord) {
        StyleChangeRecord changeRecord = (StyleChangeRecord) record;
        changeRecord.write(stream, currentNumFillBits, currentNumLineBits);
        if (changeRecord.hasNewStyles()) {
          // following StyleChangeRecords are stored using num*Bits
          // contained in this StyleChangeRecord
          currentNumFillBits   = changeRecord.getNumFillBits();
          currentNumLineBits   = changeRecord.getNumLineBits();
        }
      } else if (record instanceof StraightEdgeRecord) {
        ((StraightEdgeRecord) record).write(stream);
      } else {
        // record is CurvedEdgeRecord
        ((CurvedEdgeRecord) record).write(stream);
      }
    }

    // now write EndShapeRecord
    stream.writeUnsignedBits(0, 6);
    stream.align();
  }

  protected void read(InputBitStream stream, boolean useNewLineStyle, boolean hasAlpha)
    throws IOException {
    numFillBits   = (byte) stream.readUnsignedBits(4);
    numLineBits   = (byte) stream.readUnsignedBits(4);
    // use values from Shape/ShapeWithStyle as initial values for parsing
    // StyleChangeRecords may change these values
    byte currentNumFillBits = numFillBits;
    byte currentNumLineBits = numLineBits;
    Vector shapeRecordVector = new Vector();
    do {
      // read type flag - edge record or style change? check TypeFlag
      int typeFlag = (int) stream.readUnsignedBits(1);
      if (typeFlag == 0) {
        byte flags = (byte) stream.readUnsignedBits(5);
        if (flags == 0) {
          // EndShapeRecord, exit loop
          break;
        }

        // we have a style change record here
        StyleChangeRecord record = new StyleChangeRecord(
            stream, flags, currentNumFillBits, currentNumLineBits, useNewLineStyle, hasAlpha);
        
        // bit number may have changed (if stateNewStyles=true) 
        currentNumFillBits   = record.getNumFillBits();
        currentNumLineBits   = record.getNumLineBits();
        shapeRecordVector.add(record);
      } else {
        // we have an edge record here - curved or straight? check StraightFlag
        int straightFlag = (int) stream.readUnsignedBits(1);
        if (straightFlag == 1) {
          StraightEdgeRecord record = new StraightEdgeRecord(stream);
          shapeRecordVector.add(record);
        } else {
          CurvedEdgeRecord record = new CurvedEdgeRecord(stream);
          shapeRecordVector.add(record);
        }
      }
    } while (true);
    stream.align();
    shapeRecords = new ShapeRecord[shapeRecordVector.size()];
    shapeRecordVector.copyInto(shapeRecords);
  }

  void setNumFillBits(byte numFillBits) {
    this.numFillBits = numFillBits;
  }

  byte getNumFillBits() {
    return numFillBits;
  }

  void setNumLineBits(byte numLineBits) {
    this.numLineBits = numLineBits;
  }

  byte getNumLineBits() {
    return numLineBits;
  }

  private void computeNumBits() {
    // Iterate over shapeRecords, filter StyleChangeRecord instances.
    List changeRecords = new ArrayList();
    for (int i = 0; i < shapeRecords.length; i++) {
      ShapeRecord record = shapeRecords[i];
      if (record instanceof StyleChangeRecord) {
        changeRecords.add(shapeRecords[i]);
      }
    }
    if (changeRecords.size() == 0) {
      return;
    }

    // Form groups of StyleChangeRecords, separated by StyleChangeRecord
    // with StateNewStyles set. For each group, determine the number of
    // bits needed to store fillStyle0, fillStyle1 and lineStyle
    // (numFillBits and numLineBits).
    // Set num*Bits of the shape = num*Bits of the first group.
    // Set num*Bits of each StyleChangeRecord with StateNewStyles=true
    // to num*Bits of the group it belongs to.
    byte fillBits       = 0;
    byte lineBits       = 0;
    int groupStartIndex = -1;
    for (int i = 0; i < changeRecords.size(); i++) {
      StyleChangeRecord record = (StyleChangeRecord) changeRecords.get(i);

      // compute num*Bits
      if (record.hasFillStyle0()) {
        fillBits = (byte) Math.max(
            fillBits,
            OutputBitStream.getUnsignedBitsLength(record.getFillStyle0()));
      }
      if (record.hasFillStyle1()) {
        fillBits = (byte) Math.max(
            fillBits,
            OutputBitStream.getUnsignedBitsLength(record.getFillStyle1()));
      }
      if (record.hasLineStyle()) {
        lineBits = (byte) Math.max(
            lineBits,
            OutputBitStream.getUnsignedBitsLength(record.getLineStyle()));
      }

      // check StateNewStyles
      if (record.hasNewStyles()) {
        // new group starts, store num*Bits for current group
        storeNumBits(groupStartIndex, fillBits, lineBits, changeRecords);
        groupStartIndex   = i; // store record index
        fillBits          = 0;
        lineBits          = 0;
      }
    }

    // store num*Bits for last group (no group start to toggle storing for last group)
    storeNumBits(groupStartIndex, fillBits, lineBits, changeRecords);
    // CAUTION: to store StyleChangeRecords with StateNewStyles=true,
    // use num*Bits of preceding group
    // (or of the shape instance for the first group), not their own num*Bits!
  }

  private void storeNumBits(
    int groupStartIndex, byte fillBits, byte lineBits, List changeRecords) {
    if (groupStartIndex > -1) {
      // store in first change record of group
      StyleChangeRecord groupStartRecord = (StyleChangeRecord) changeRecords.get(
          groupStartIndex);
      groupStartRecord.setNumFillBits(fillBits);
      groupStartRecord.setNumLineBits(lineBits);
    } else {
      // first group - store in shape instance
      this.numFillBits   = fillBits;
      this.numLineBits   = lineBits;
    }
  }
}

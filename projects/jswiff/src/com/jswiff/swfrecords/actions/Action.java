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

package com.jswiff.swfrecords.actions;

import com.jswiff.io.OutputBitStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


/**
 * This is the base class for action records.
 */
public abstract class Action implements Serializable {
  protected short code;
  private int offset; // offset in stream, relative to beginning of action record array
  private String label;
  
  /**
   * Returns the code of the action record.
   *
   * @return the code
   */
  public short getCode() {
    return code;
  }
  
  /**
   * Creates a deep copy of this action record. Useful if you want to clone a part of a
   * SWF document.
   *
   * @return a copy of the action record
   */
  public Action copy() {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos     = new ObjectOutputStream(baos);
      oos.writeObject(this);
      ByteArrayInputStream bais = new ByteArrayInputStream(
          baos.toByteArray());
      ObjectInputStream ois     = new ObjectInputStream(bais);
      return (Action) ois.readObject();
    } catch (Exception e) {
      // actually, this should never happen (everything serializable??)
      // this will eventually be removed
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  /**
   * Sets the label of this action record. The label is used for jumps within
   * an action block. Warning: use a label only once within an action block!
   *
   * @param label label string, unique within an action block
   */
  public void setLabel(String label) {
    this.label = label;
  }

  /**
   * Returns this action record's label, which is used for jumps within an
   * action block.
   *
   * @return the action's label
   */
  public String getLabel() {
    return label;
  }

  /**
   * Returns this action record's offset within the action block this action
   * record is contained in (in bytes).
   *
   * @return the offset in bytes from the beginning of the containing action
   *         block
   */
  public int getOffset() {
    return offset;
  }

  /**
   * <p>
   * Returns the size of this action record in bytes. Can be used to compute
   * the size of an action block (e.g. for implementing jumps, function
   * definitions etc.).
   * </p>
   * 
   * <p>
   * The size of action records with <code>code &lt;= 0x80</code> is always 1
   * byte (the action code). Other actions may contain data. Their size is of
   * at least 3 bytes (1 byte action code, 2 bytes data length).
   * </p>
   *
   * @return size of action record (in bytes)
   */
  public int getSize() {
    return 1;
  }

  /*
   * This method should be overwritten in action classes which contain data
   * (code >= 0x80). Write action data to dataStream and action (sub-)blocks
   * to mainStream.
   */
  protected void writeData(
    OutputBitStream dataStream, OutputBitStream mainStream)
    throws IOException {
    throw new IOException(
      "Override this method for actions with code >= 0x80!");
  }

  void setOffset(int offset) {
    this.offset = offset;
  }

  void write(OutputBitStream stream) throws IOException {
    stream.writeUI8(code);
    if (code >= 0x80) {
      OutputBitStream dataStream = new OutputBitStream();
      dataStream.setANSI(stream.isANSI());
      dataStream.setShiftJIS(stream.isShiftJIS());
      // we simulate mainStream here, as we don't want
      // action blocks between beginning and end of action...
      OutputBitStream mainStream = new OutputBitStream();
      mainStream.setANSI(stream.isANSI());
      mainStream.setShiftJIS(stream.isShiftJIS());
      writeData(dataStream, mainStream);
      byte[] actionData = dataStream.getData();
      stream.writeUI16(actionData.length);
      stream.writeBytes(actionData);
      // now we write the action blocks to main stream
      stream.writeBytes(mainStream.getData());
    }
  }
}

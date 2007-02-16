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

import com.jswiff.io.InputBitStream;
import com.jswiff.io.OutputBitStream;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * <p>
 * Creates a new constant pool. The constants defined here can be referenced in
 * <code>Push</code> as <code>constant8</code> if there are less than 256
 * constants in the pool, otherwise as <code>constant16</code>.
 * </p>
 * 
 * <p>
 * Performed stack operations: none
 * </p>
 * 
 * <p>
 * ActionScript equivalent: none
 * </p>
 *
 * @since SWF 5
 */
public final class ConstantPool extends Action {
  private List constants = new ArrayList();

  /**
   * Creates a new ConstantPool action.
   */
  public ConstantPool() {
    code = ActionConstants.CONSTANT_POOL;
  }

  /*
   * Creates a new ConstantPool action. Data is read from a bit stream.
   *
   * @param stream bit stream containing the action data
   *
   * @throws IOException if an I/O error has occured
   */
  ConstantPool(InputBitStream stream) throws IOException {
    code = ActionConstants.CONSTANT_POOL;
    int count = stream.readUI16();
    if (count > 0) {
      for (int i = 0; i < count; i++) {
        constants.add(stream.readString());
      }
    }
  }

  /**
   * Returns the constants of the pool.
   *
   * @return list containing all constants as String instances
   */
  public List getConstants() {
    return constants;
  }

  /**
   * Returns the size of this action record in bytes.
   *
   * @return size of this record
   *
   * @see Action#getSize()
   */
  public int getSize() {
    int size = 5; // 1 (code) + 2 (data length) + 2 (# of constants)
    try {
      for (Iterator i = constants.iterator(); i.hasNext();) {
        size += (((String) i.next()).getBytes("UTF-8").length + 1); // Unicode, null-terminated
      }
    } catch (UnsupportedEncodingException e) {
      // UTF-8 should be available..
    }
    return size;
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"ConstantPool"</code> and number of constants in pool
   */
  public String toString() {
    return "ConstantPool (" + constants.size() + " constants)";
  }

  protected void writeData(
    OutputBitStream dataStream, OutputBitStream mainStream)
    throws IOException {
    dataStream.writeUI16(constants.size());
    for (int i = 0; i < constants.size(); i++) {
      dataStream.writeString((String) constants.get(i));
    }
  }
}

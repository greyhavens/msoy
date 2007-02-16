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


/**
 * <p>
 * Reads the next item from the stack and stores it in the register with the
 * specified number. Up to 4 registers can be used (within
 * <code>DefineFunction2</code> up to 256).
 * </p>
 * 
 * <p>
 * Performed stack operations: none (item is read without being removed from
 * stack)
 * </p>
 * 
 * <p>
 * ActionScript equivalent: none
 * </p>
 *
 * @since SWF 5
 */
public final class StoreRegister extends Action {
  private short number;

  /**
   * Creates a new StoreRegister action. Up to 4 registers can be used (within
   * <code>DefineFunction2</code> up to 256).
   *
   * @param number a register number.
   */
  public StoreRegister(short number) {
    code          = ActionConstants.STORE_REGISTER;
    this.number   = number;
  }

  StoreRegister(InputBitStream stream) throws IOException {
    code     = ActionConstants.STORE_REGISTER;
    number   = stream.readUI8();
  }

  /**
   * Returns the register number.
   *
   * @return register number
   */
  public short getNumber() {
    return number;
  }

  /**
   * Returns the size of this action record in bytes.
   *
   * @return size of this record
   *
   * @see Action#getSize()
   */
  public int getSize() {
    return 4;
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"StoreRegister", number</code>
   */
  public String toString() {
    return "StoreRegister " + number;
  }

  protected void writeData(
    OutputBitStream dataStream, OutputBitStream mainStream)
    throws IOException {
    dataStream.writeUI8(number);
  }
}

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
import com.jswiff.swfrecords.actions.Action;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;


/**
 * This class is used by <code>DefineFunction2</code> to specify the function's
 * parameters. These can be either variables or registers.
 */
public final class RegisterParam implements Serializable {
  private short register;
  private String paramName;

  /**
   * Creates a new RegisterParam instance. If you use 0 as register number, the
   * parameter can be referenced as a variable within the function (this
   * variable's name is contained in <code>paramName</code>). If the register
   * number is greater than 0, the parameter is copied into the corresponding
   * register.
   *
   * @param register register number
   * @param paramName variable name
   */
  public RegisterParam(short register, String paramName) {
    this.register    = register;
    this.paramName   = paramName;
  }

  /**
   * Reads an instance from a bit stream.
   *
   * @param stream source bit stream
   *
   * @throws IOException if an I/O error has occured
   */
  public RegisterParam(InputBitStream stream) throws IOException {
    register    = stream.readUI8();
    paramName   = stream.readString();
  }

  /**
   * Returns the parameter (i.e. the variable's) name
   *
   * @return variable name
   */
  public String getParamName() {
    return paramName;
  }

  /**
   * Returns the register number.
   *
   * @return register number
   */
  public short getRegister() {
    return register;
  }

  /**
   * Returns the size of this action record in bytes.
   *
   * @return size of this record
   *
   * @see Action#getSize()
   */
  public int getSize() {
    // null-terminated unicode + 1 byte
    int size = 2;
    try {
      size += paramName.getBytes("UTF-8").length;
    } catch (UnsupportedEncodingException e) {
      // UTF-8 should always be available
    }
    return size;
  }

  /**
   * Writes the instance to a bit stream.
   *
   * @param stream the target bit stream
   *
   * @throws IOException if an I/O error has occured
   */
  public void write(OutputBitStream stream) throws IOException {
    stream.writeUI8(register);
    stream.writeString(paramName);
  }
}

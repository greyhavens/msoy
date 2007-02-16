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


/**
 * <p>
 * Instructs Flash Player to change the context of subsequent actions, so they
 * apply to an object with the specified name. This action can be used e.g. to
 * control the timeline of a sprite object.
 * </p>
 * 
 * <p>
 * Note: as of SWF 5, this action is deprecated. Use <code>With</code> instead.
 * </p>
 * 
 * <p>
 * Performed stack operations: none
 * </p>
 * 
 * <p>
 * ActionScript equivalent: <code>tellTarget()</code>
 * </p>
 *
 * @since SWF 3
 */
public final class SetTarget extends Action {
  private String name;

  /**
   * Creates a new SetTarget action. The target object's name is passed as a
   * string.
   *
   * @param name target object name
   */
  public SetTarget(String name) {
    code        = ActionConstants.SET_TARGET;
    this.name   = name;
  }

  /*
   * Reads a SetTarget action from a bit stream.
   */
  SetTarget(InputBitStream stream) throws IOException {
    code   = ActionConstants.SET_TARGET;
    name   = stream.readString();
  }

  /**
   * Returns the name of the target object.
   *
   * @return target object's name
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the size of this action record in bytes.
   *
   * @return size of this record
   *
   * @see Action#getSize()
   */
  public int getSize() {
    int size = 4;
    try {
      size += name.getBytes("UTF-8").length;
    } catch (UnsupportedEncodingException e) {
      // UTF-8 should be available
    }
    return size;
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"SetTarget"</code>
   */
  public String toString() {
    return "SetTarget " + name;
  }

  protected void writeData(
    OutputBitStream dataStream, OutputBitStream mainStream)
    throws IOException {
    dataStream.writeString(name);
  }
}

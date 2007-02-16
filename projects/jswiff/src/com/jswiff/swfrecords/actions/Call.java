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


/**
 * <p>
 * Executes the script attached to a specified frame. The argument can be a
 * frame number or a frame label.<br>
 * This action is deprecated since SWF 5. Use <code>CallFunction</code> where
 * possible.
 * </p>
 * 
 * <p>
 * Performed stack operations:<br><code>pop frame</code> (number or label)
 * </p>
 * 
 * <p>
 * ActionScript equivalent: the <code>&&</code> operator
 * </p>
 *
 * @since SWF 4
 */
public final class Call extends Action {
  /**
   * Creates a new Call action.
   */
  public Call() {
    code = ActionConstants.CALL;
  }

  /**
   * <p>
   * Returns the size of this action record in bytes. Can be used to compute
   * the size of an action block (e.g. for implementing jumps, function
   * definitions etc.).
   * </p>
   *
   * @return size of action record (in bytes)
   */
  public int getSize() {
    return 3; // 1 (code) + 2 (size - always 0 ...)
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"Call"</code>
   */
  public String toString() {
    return "Call";
  }

  protected void writeData(
    OutputBitStream dataStream, OutputBitStream mainStream) {
    // don't do anything
  }
}

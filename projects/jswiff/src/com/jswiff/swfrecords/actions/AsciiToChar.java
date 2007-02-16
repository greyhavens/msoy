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

/**
 * <p>
 * Converts an ASCII character code to a character. Deprecated as of SWF 5, use
 * String.fromCharCode() where possible.
 * </p>
 * 
 * <p>
 * Performed stack operations:<br>
 * <code>pop code<br>
 * push char</code> (Character with the ASCII code <code>code</code>)
 * </p>
 * 
 * <p>
 * ActionScript equivalent: <code>chr()</code>
 * </p>
 *
 * @since SWF 4
 */
public final class AsciiToChar extends Action {
  /**
   * Creates a new AsciiToChar action.
   */
  public AsciiToChar() {
    code = ActionConstants.ASCII_TO_CHAR;
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"AsciiToChar"</code>
   */
  public String toString() {
    return "AsciiToChar";
  }
}

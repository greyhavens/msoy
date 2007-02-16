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
 * Performs a bitwise right shift (<code>&gt;&gt;</code>). The value argument
 * is interpreted as a 32-bit integer (signed integer for a negative number,
 * otherwise unsigned integer). The shift count is treated as an integer from
 * 0 to 31 (only lower 5 bits are considered). The result is interpreted as
 * signed 32-bit integer.
 * </p>
 * 
 * <p>
 * Performed stack operations:<br>
 * <code>pop count<br>
 * pop value<br>
 * push [value &gt;&gt; count]</code>
 * </p>
 * 
 * <p>
 * ActionScript equivalent: the <code>&gt;&gt;</code> operator
 * </p>
 *
 * @since SWF 5
 */
public final class BitRShift extends Action {
  /**
   * Creates a new BitRShift action.
   */
  public BitRShift() {
    code = ActionConstants.BIT_R_SHIFT;
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"BitRShift"</code>
   */
  public String toString() {
    return "BitRShift";
  }
}

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
 * Tests whether a number is greater than another number, taking account of
 * data types (according to ECMA-262 spec).
 * </p>
 * 
 * <p>
 * Performed stack operations:<br>
 * <code>pop b</code><br>
 * <code>pop a</code><br>
 * <code>push [a &gt; b]</code> (<code>true</code> if a&gt;b, otherwise
 * <code>false</code>)<br>
 * </p>
 * 
 * <p>
 * ActionScript equivalent: <code>&gt;</code> operator
 * </p>
 *
 * @since SWF 6
 */
public final class Greater extends Action {
  /**
   * Creates a new Greater action.
   */
  public Greater() {
    code = ActionConstants.GREATER;
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"Greater"</code>
   */
  public String toString() {
    return "Greater";
  }
}

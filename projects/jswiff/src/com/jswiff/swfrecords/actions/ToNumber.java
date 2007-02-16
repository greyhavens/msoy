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
 * Converts an item into a number.
 * </p>
 * 
 * <p>
 * Performed stack operations:<br>
 * <code>pop item</code> (the item to be converted - for objects,
 * <code>valueOf()</code> is used for conversion)<br>
 * <code>push number</code> (the conversion result as number)<br>
 * </p>
 * 
 * <p>
 * ActionScript equivalent: <code>Number()</code>
 * </p>
 *
 * @since SWF 5
 */
public final class ToNumber extends Action {
  /**
   * Creates a new ToNumber action.
   */
  public ToNumber() {
    code = ActionConstants.TO_NUMBER;
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"ToNumber"</code>
   */
  public String toString() {
    return "ToNumber";
  }
}

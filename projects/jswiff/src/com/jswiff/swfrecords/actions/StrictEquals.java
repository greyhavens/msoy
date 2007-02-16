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
 * Similar to <code>Equals2</code>, but the two arguments must be of the same
 * type in order to be considered equal (i.e. data types are not converted).
 * </p>
 * 
 * <p>
 * Performed stack operations:<br>
 * <code>pop item2</code> (first number)<br>
 * <code>pop item1</code> (second number)<br>
 * <code>push [item1 === item2]</code> (<code>true</code> if items and types
 * are equal, else <code>false</code>)
 * </p>
 * 
 * <p>
 * ActionScript equivalents: <code>===</code> operator
 * </p>
 *
 * @since SWF 6
 */
public final class StrictEquals extends Action {
  /**
   * Creates a new StrictEquals action.
   */
  public StrictEquals() {
    code = ActionConstants.STRICT_EQUALS;
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"StrictEquals"</code>
   */
  public String toString() {
    return "StrictEquals";
  }
}

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
 * Iterates over all properties of an object and pushes their names to the
 * stack. The difference to <code>Enumerate</code> is that
 * <code>Enumerate2</code> uses a stack argument of object type rather than
 * the object's name as string.
 * </p>
 * 
 * <p>
 * Performed stack operations:<br>
 * <code>pop obj</code> (object instance)<br>
 * <code>push null</code> (indicates the end of the property list)<br>
 * <code>push prop1</code> (1st property name)<br>
 * <code>push prop2</code> (2nd property name)<br>
 * <code>...</code><br>
 * <code>push propn</code> (n-th property name)<br>
 * </p>
 * 
 * <p>
 * ActionScript equivalent: <code>for..in</code> loop
 * </p>
 *
 * @since SWF 6
 */
public final class Enumerate2 extends Action {
  /**
   * Creates a new Enumerate2 action.
   */
  public Enumerate2() {
    code = ActionConstants.ENUMERATE_2;
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"Enumerate2"</code>
   */
  public String toString() {
    return "Enumerate2";
  }
}

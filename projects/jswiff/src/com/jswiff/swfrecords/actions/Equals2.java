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
 * Tests two items for equality. Unlike <code>Equals</code>,
 * <code>Equals2</code> takes account of data types.
 * </p>
 * 
 * <p>
 * Performed stack operations:<br>
 * <code>pop item1</code> (first item)<br>
 * <code>pop item2</code> (second item)<br>
 * <code>push [item2 == item1]</code> (<code>true</code> if equal, else
 * <code>false</code>)
 * </p>
 * 
 * <p>
 * ActionScript equivalents: <code>==</code> operator
 * </p>
 *
 * @since SWF 5
 */
public final class Equals2 extends Action {
  /**
   * Creates a new Equals2 actions.
   */
  public Equals2() {
    code = ActionConstants.EQUALS_2;
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"Equals2"</code>
   */
  public String toString() {
    return "Equals2";
  }
}

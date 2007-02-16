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
 * This action is used to implement the addition operator according to the
 * ECMAScript specification, i.e. it performs either string concatenation or
 * numeric addition, depending on the data types of the operands.
 * </p>
 * 
 * <p>
 * Performed stack operations:<br><code>pop b<br> pop a<br> push [a + b]</code>
 * </p>
 * 
 * <p>
 * ActionScript equivalent: the <code>+</code> operator
 * </p>
 *
 * @since SWF 5
 */
public final class Add2 extends Action {
  /**
   * Creates a new Add2 action.
   */
  public Add2() {
    code = ActionConstants.ADD_2;
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"Add2"</code>
   */
  public String toString() {
    return "Add2";
  }
}

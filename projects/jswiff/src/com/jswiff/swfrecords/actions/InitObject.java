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
 * Creates an object and initializes it with values from the stack.
 * </p>
 * 
 * <p>
 * Performed stack operations:<br>
 * <code> pop n</code> (property number)<br>
 * <code> pop value_1</code> (1st property value)<br>
 * <code> pop name_1</code> (1st property name)<br>
 * <code> pop value_2</code> (2nd property value)<br>
 * <code> pop name_2</code> (2nd property name)<br>
 * <code> ...<br>
 * pop value_n</code> (n-th property value)<br>
 * <code> pop name_n</code> (n-th property name)<br>
 * <code> push obj</code> (the new object)
 * </p>
 * 
 * <p>
 * ActionScript equivalent: object literal (e.g. <code>{width: 150, height:
 * 100}</code>)
 * </p>
 *
 * @since SWF 5
 */
public final class InitObject extends Action {
  /**
   * Creates a new InitObject action.
   */
  public InitObject() {
    code = ActionConstants.INIT_OBJECT;
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"InitObject"</code>
   */
  public String toString() {
    return "InitObject";
  }
}

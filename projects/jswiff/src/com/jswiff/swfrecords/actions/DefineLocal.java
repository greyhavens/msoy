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
 * Defines a local variable and sets its value. If the variable already exists,
 * the value is set to the newly specified value.
 * </p>
 * 
 * <p>
 * Performed stack operations:<br>
 * <code> pop value</code> (initial value)<br>
 * <code> pop varName</code> (variable name)<br>
 * </p>
 * 
 * <p>
 * ActionScript equivalent: variable declaration with initialization (e.g.
 * <code>var a = 2;)</code>
 * </p>
 *
 * @since SWF 5
 */
public final class DefineLocal extends Action {
  /**
   * Creates a new DefineLocal action.
   */
  public DefineLocal() {
    code = ActionConstants.DEFINE_LOCAL;
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"DefineLocal"</code>
   */
  public String toString() {
    return "DefineLocal";
  }
}

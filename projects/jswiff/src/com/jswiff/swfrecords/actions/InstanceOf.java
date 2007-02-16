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
 * This action determines if an object is an instance of a specified class (or
 * interface as of SWF 7).
 * </p>
 * 
 * <p>
 * Performed stack operations:<br>
 * <code>pop class</code><br>
 * <code>pop ref</code> (reference to the object to be checked)<br>
 * <code>push result</code> (<code>true</code> or <code>false</code>)
 * </p>
 * 
 * <p>
 * ActionScript equivalent: <code>instanceof</code> operator
 * </p>
 *
 * @since SWF 6
 */
public final class InstanceOf extends Action {
  /**
   * Creates a new InstanceOf action.
   */
  public InstanceOf() {
    code = ActionConstants.INSTANCE_OF;
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"InstanceOf"</code>
   */
  public String toString() {
    return "InstanceOf";
  }
}

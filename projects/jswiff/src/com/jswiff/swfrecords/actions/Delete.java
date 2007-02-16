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
 * Deletes an object's property. Can be used to free memory. After deletion,
 * the property has the value <code>undefined</code>.
 * </p>
 * 
 * <p>
 * Performed stack operations:<br>
 * <code>pop ref</code> (reference to object to be altered)<br>
 * <code>pop prop</code> (property to be deleted)<br>
 * <code>push success</code> (<code>true</code> if the operation succeeded,
 * otherwise <code>false</code>)<br>
 * </p>
 * 
 * <p>
 * ActionScript equivalent: the delete operator (e.g. <code>delete
 * ref.prop;</code>)
 * </p>
 *
 * @since SWF 5
 */
public final class Delete extends Action {
  /**
   * Creates a new Delete action.
   */
  public Delete() {
    code = ActionConstants.DELETE;
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"Delete"</code>
   */
  public String toString() {
    return "Delete";
  }
}

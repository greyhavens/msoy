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
 * This action allows casting from one data type to another.
 * </p>
 * 
 * <p>
 * Performed stack operations:<br>
 * <code>pop Type<br>
 * pop obj<br>
 * push [(Type)obj] </code> (object <code>obj</code> cast to type
 * <code>Type</code>)
 * </p>
 * 
 * <p>
 * Note: push <code>Type</code> this way to the stack:<br>
 * <code>push 'Type'<br>
 * GetVar </code>
 * </p>
 * 
 * <p>
 * ActionScript equivalent: type cast (<code>Type(obj)</code>)
 * </p>
 *
 * @since SWF 6
 */
public final class CastOp extends Action {
  /**
   * Creates a new CastOp action.
   */
  public CastOp() {
    code = ActionConstants.CAST_OP;
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"CastOp"</code>
   */
  public String toString() {
    return "CastOp";
  }
}

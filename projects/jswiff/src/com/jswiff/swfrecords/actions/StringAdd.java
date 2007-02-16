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
 * Concatenates two strings. Replaced by <code>Add2</code> as of SWF 5.
 * </p>
 * 
 * <p>
 * Performed stack operations:<br>
 * <code>pop str1<br>
 * pop str2<br>
 * push [str2 & str1]</code> (concatenation of str2 and str1)
 * </p>
 * 
 * <p>
 * ActionScript equivalent: <code>&</code> operator in SWF 4 (as of SWF 5, this
 * operator has a different meaning!)
 * </p>
 *
 * @since SWF 4
 */
public final class StringAdd extends Action {
  /**
   * Creates a new StringAdd action.
   */
  public StringAdd() {
    code = ActionConstants.STRING_ADD;
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"StringAdd"</code>
   */
  public String toString() {
    return "StringAdd";
  }
}

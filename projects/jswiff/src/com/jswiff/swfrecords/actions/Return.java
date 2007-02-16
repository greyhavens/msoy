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
 * Causes the execution to immediately return to the calling function.
 * </p>
 * 
 * <p>
 * Performed stack operations: none
 * </p>
 * 
 * <p>
 * Stack precondition:  the function result must be pushed to stack prior to
 * this action's invocation, as <code>CallMethod</code> and
 * <code>CallFunction</code> implicitly pop the function result off the stack.
 * If the function has no result, use <code>undefined</code> as result.
 * </p>
 * 
 * <p>
 * ActionScript equivalent: <code>return</code> statement
 * </p>
 *
 * @since SWF 5
 */
public final class Return extends Action {
  /**
   * Creates a new Return action.
   */
  public Return() {
    code = ActionConstants.RETURN;
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"Return"</code>
   */
  public String toString() {
    return "Return";
  }
}

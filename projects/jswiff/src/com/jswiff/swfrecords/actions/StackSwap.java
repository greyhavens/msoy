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
 * Swaps the top two items on the stack.
 * </p>
 * 
 * <p>
 * Performed stack operations:<br>
 * <code>pop item1</code><br>
 * <code>pop item2</code><br>
 * <code>push item1</code><br>
 * <code>push item2</code><br>
 * </p>
 * 
 * <p>
 * ActionScript equivalent: none
 * </p>
 *
 * @since SWF 5
 */
public final class StackSwap extends Action {
  /**
   * Creates a new StackSwap action.
   */
  public StackSwap() {
    code = ActionConstants.STACK_SWAP;
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"StackSwap"</code>
   */
  public String toString() {
    return "StackSwap";
  }
}

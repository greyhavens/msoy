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
 * Invokes a function, e.g. a user-defined ActionScript function (defined with
 * <code>DefineFunction</code> or <code>DefineFunction2</code>), or a native
 * function, and pushes its result to the stack.
 * </p>
 * 
 * <p>
 * Performed stack operations (for a function <code>myFunction(arg1, arg2, ..., argn))</code>:<br>
 * <code>pop function</code> (the function to be called, either as function
 * name or as anonymous function declaration)<br>
 * <code>pop n</code> (the number of arguments as an integer)<br>
 * <code>pop arg1</code> (first argument)<br>
 * <code>pop arg2</code> (second argument)<br>
 * <code>...<br>
 * pop argn</code> (nth argument)<br>
 * <code>push result<br></code>
 * </p>
 * 
 * <p>
 * Use <code>Return</code> to push the function's result when declaring the
 * function. Otherwise, the function has no result, and <code>undefined</code>
 * is pushed. In this case, use <code>Pop</code> to discard it after the
 * function call.
 * </p>
 * 
 * <p>
 * ActionScript equivalents:
 * 
 * <ul>
 * <li>
 * standard function call, e.g.<br><code>parseInt(numString));</code><br>
 * </li>
 * <li>
 * anonymous function call, e.g.<br><code>function (x) { x + 3 } (1);</code>
 * </li>
 * </ul>
 * </p>
 *
 * @since SWF 5
 */
public final class CallFunction extends Action {
  /**
   * Creates a new CallFunction action.
   */
  public CallFunction() {
    code = ActionConstants.CALL_FUNCTION;
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"CallFunction"</code>
   */
  public String toString() {
    return "CallFunction";
  }
}

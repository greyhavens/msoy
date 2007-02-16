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
 * Invokes a method and pushes its result to the stack.
 * </p>
 * 
 * <p>
 * Performed stack operations (e.g. for a method <code>myObject.myMethod(arg1,
 * arg2, ..., argn))</code>:<br>
 * <code>pop instance</code> (the instance this method is invoked on, e.g. <code>myObject</code>)<br>
 * <code>pop name</code> (the method name, e.g. <code>"myMethod"</code>)<br>
 * <code>pop n</code> (the number of arguments as an integer)<br>
 * <code>pop arg1</code> (first argument)<br>
 * <code>pop arg2</code> (second argument)<br>
 * <code>...<br>
 * pop argn</code> (nth argument)<br>
 * <code> push result<br>
 * </code> The instance the method is invoked on has to be pushed to the stack
 * before method invocation e.g. like this:<br>
 * <code>push &quot;myObject&quot;<br>
 * GetVariable<br>
 * </code> If the method has no result, <code>undefined</code> is pushed. In
 * this case, use <code>Pop</code> to discard it.
 * </p>
 * 
 * <p>
 * ActionScript equivalent: method invocation using dot syntax
 * </p>
 *
 * @since SWF 5
 */
public final class CallMethod extends Action {
  /**
   * Creates a new CallMethod action.
   */
  public CallMethod() {
    code = ActionConstants.CALL_METHOD;
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"CallMethod"</code>
   */
  public String toString() {
    return "CallMethod";
  }
}

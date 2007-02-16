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
 * This action specifies the interfaces a class implements.
 * </p>
 * 
 * <p>
 * Performed stack operations:<br>
 * <code>pop constructor</code> (the constructor of the new class)<br>
 * <code>pop n</code> (number of interfaces this class implements)<br>
 * <code>pop i1</code> (1st interface)<br>
 * <code>pop i2</code> (2nd interface)<br>
 * <code>...<br>
 * pop in</code> (n-th interface)<br>
 * </p>
 * 
 * <p>
 * ActionScript equivalent: <code>implements</code> keyword
 * </p>
 *
 * @since SWF 7
 */
public final class ImplementsOp extends Action {
  /**
   * Creates a new ImplementsOp action.
   */
  public ImplementsOp() {
    code = ActionConstants.IMPLEMENTS_OP;
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"ImplementsOp"</code>
   */
  public String toString() {
    return "ImplementsOp";
  }
}

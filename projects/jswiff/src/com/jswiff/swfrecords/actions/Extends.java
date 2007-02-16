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
 * Extends creates an inheritance relationship between two classes (instead of
 * classes, interfaces can also be used, since inheritance between interfaces
 * is also possible).
 * </p>
 * 
 * <p>
 * Performed stack operations:<br>
 * <code>pop superClass</code> (the class to be inherited)<br>
 * <code>pop subClassConstructor</code> (the constructor of the new class)<br>
 * </p>
 * 
 * <p>
 * ActionScript equivalent: <code>extends</code> keyword
 * </p>
 *
 * @since SWF 7
 */
public final class Extends extends Action {
  /**
   * Creates a new Extends action.
   */
  public Extends() {
    code = ActionConstants.EXTENDS;
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"Extends"</code>
   */
  public String toString() {
    return "Extends";
  }
}

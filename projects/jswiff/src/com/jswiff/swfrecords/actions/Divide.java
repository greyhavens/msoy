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
 * Divides two numbers.
 * </p>
 * 
 * <p>
 * Performed stack operations:<br><code>pop b<br> pop a<br> push [a / b]</code>
 * </p>
 * 
 * <p>
 * Note: in SWF 5 and later, if <code>a</code> or <code>b</code> are not (or
 * cannot be converted to) floating point numbers, the result is
 * <code>NaN</code> (or <code>Double.NaN</code>); if <code>b</code> is 0, the
 * result is <code>Infinity</code> or <code>-Infinity</code>
 * (<code>Double.POSITIVE_INFINITY</code> or
 * <code>Double.NEGATIVE_INFINITY</code>), depending on <code>a</code>'s sign.
 * Before SWF 5, these results were not IEEE-754 compliant.
 * </p>
 * 
 * <p>
 * ActionScript equivalent: the <code>/</code> operator
 * </p>
 *
 * @since SWF 4
 */
public final class Divide extends Action {
  /**
   * Creates a new Divide action.
   */
  public Divide() {
    code = ActionConstants.DIVIDE;
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"Divide"</code>
   */
  public String toString() {
    return "Divide";
  }
}

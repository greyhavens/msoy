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
 * Instructs Flash Player to change the context of subsequent actions, so they
 * apply to an object with the specified name. This action can be used e.g. to
 * control the timeline of a sprite object. Unlike <code>SetTarget</code>,
 * <code>SetTarget2</code> pops the target off the stack.
 * </p>
 * 
 * <p>
 * Note: as of SWF 5, this action is deprecated. Use <code>With</code> instead.
 * </p>
 * 
 * <p>
 * Performed stack operations:<br><code>pop target</code>
 * </p>
 * 
 * <p>
 * ActionScript equivalent: <code>tellTarget()</code>
 * </p>
 *
 * @since SWF 4
 */
public final class SetTarget2 extends Action {
  /**
   * Creates a new SetTarget2 action.
   */
  public SetTarget2() {
    code = ActionConstants.SET_TARGET_2;
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"SetTarget2"</code>
   */
  public String toString() {
    return "SetTarget2";
  }
}

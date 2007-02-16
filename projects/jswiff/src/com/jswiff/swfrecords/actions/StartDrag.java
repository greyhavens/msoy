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
 * This action makes the target sprite draggable, i.e. it makes sure users can
 * drag it to another location.
 * </p>
 * 
 * <p>
 * Note: Only one sprite can be dragged at a time. The sprite remains draggable
 * until it is explicitly stopped by <code>ActionStopDrag</code> or until
 * <code>StartDrag</code> is called for another sprite.
 * </p>
 * 
 * <p>
 * Performed stack operations:<br>
 * <code>pop sprite</code> (the sprite to be dragged)<br>
 * <code>pop lockCenter</code> (if nonzero, mouse is locked to sprite center,
 * otherwise it is locked to the mouse position at the time the dragging started)<br>
 * <code>pop constrain</code> (if nonzero, four values which define a
 * constraint window are popped off the stack)<br>
 * <code>pop y2</code> (bottom constraint coordinate)<br>
 * <code>pop x2</code> (right constraint coordinate)<br>
 * <code>pop y1</code> (top constraint coordinate)<br>
 * <code>pop x1</code> (left constraint coordinate)<br>
 * Constraint values are relative to the coordinates of the sprite's parent.
 * </p>
 * 
 * <p>
 * ActionScript equivalents: <code>startDrag()</code>,
 * <code>MovieClip.startDrag()</code>
 * </p>
 *
 * @since SWF 5
 */
public final class StartDrag extends Action {
  /**
   * Creates a new StartDrag action.
   */
  public StartDrag() {
    code = ActionConstants.START_DRAG;
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"StartDrag"</code>
   */
  public String toString() {
    return "StartDrag";
  }
}

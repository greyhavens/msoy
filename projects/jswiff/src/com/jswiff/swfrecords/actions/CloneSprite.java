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
 * Duplicates a sprite, creating a new sprite instance at a given depth.
 * </p>
 * 
 * <p>
 * Performed stack operations:<br>
 * <code> pop depth </code>(depth at which the clone sprite will be created)<br>
 * <code> pop name</code> (instance name of the clone)<br>
 * <code> pop sprite</code> (sprite to be cloned)<br>
 * </p>
 * 
 * <p>
 * Note: use values between 16384 and 1064959 for <code>depth</code>, as this
 * range is reserved for dynamic use (otherwise - among other problems - you
 * won't be able to remove the created sprite).
 * </p>
 * 
 * <p>
 * ActionScript equivalent: <code>duplicateMovieClip()</code>. The Macromedia
 * Flash compiler internally adds 16384 for convenience to the depth passed as
 * parameter.
 * </p>
 *
 * @since SWF 4
 */
public final class CloneSprite extends Action {
  /**
   * Creates a new CloneSprite action.
   */
  public CloneSprite() {
    code = ActionConstants.CLONE_SPRITE;
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"CloneSprite"</code>
   */
  public String toString() {
    return "CloneSprite";
  }
}

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

package com.jswiff.swfrecords.tags;

import com.jswiff.io.OutputBitStream;


/**
 * This tag instructs Flash Player to display all characters added  (either
 * with <code>PlaceObject</code> or <code>PlaceObject2</code>) to the display
 * list. The display list is cleared, and the movie is paused for the duration
 * of a single frame (which is the reciprocal of the SWF frame rate).
 *
 * @since SWF 1
 */
public final class ShowFrame extends Tag {
  /**
   * Creates a new ShowFrame tag.
   */
  public ShowFrame() {
    code = TagConstants.SHOW_FRAME;
  }

  protected void writeData(OutputBitStream outStream) {
    // tag contains no data
  }

  void setData(byte[] data) {
    // tag contains no data
  }
}

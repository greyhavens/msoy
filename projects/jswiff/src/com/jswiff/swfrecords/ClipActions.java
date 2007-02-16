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

package com.jswiff.swfrecords;

import com.jswiff.io.InputBitStream;
import com.jswiff.io.OutputBitStream;

import java.io.IOException;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * This class is used for defining event handlers for sprites. Used within the
 * <code>PlaceObject2</code> tag.
 *
 * @see com.jswiff.swfrecords.tags.PlaceObject2
 */
public final class ClipActions implements Serializable {
  private ClipEventFlags eventFlags;
  private List clipActionRecords = new ArrayList();

  /**
   * Creates a new ClipActions instance. Supply event flags and handlers.
   *
   * @param eventFlags all events used in the clip actions
   * @param clipActionRecords list of one or more event handlers
   *        (<code>ClipActionRecord</code> instances)
   *
   * @see ClipActionRecord
   */
  public ClipActions(ClipEventFlags eventFlags, List clipActionRecords) {
    this.eventFlags          = eventFlags;
    this.clipActionRecords   = clipActionRecords;
  }

  /**
   * Creates a new ClipActions instance, reading data from a bit stream.
   *
   * @param stream source bit stream
   * @param swfVersion swf version used
   *
   * @throws IOException if an I/O error has occured
   */
  public ClipActions(InputBitStream stream, short swfVersion)
    throws IOException {
    stream.readUI16(); // reserved, =0
    eventFlags = new ClipEventFlags(stream, swfVersion);
    while (true) {
      int available = stream.available();
      if (
        ((swfVersion <= 5) && (available == 2)) ||
            ((swfVersion > 5) && (available == 4))) {
        // ClipActionEndFlag is UI16 for pre-MX, UI32 for MX and higher
        break;
      }
      ClipActionRecord record = new ClipActionRecord(stream, swfVersion);
      clipActionRecords.add(record);
    }
  }

  /**
   * Returns a list containing the event handlers (as
   * <code>ClipActionRecord</code> instance).
   *
   * @return list with event handlers
   *
   * @see ClipActionRecord
   */
  public List getClipActionRecords() {
    return clipActionRecords;
  }

  /**
   * Returns all event flags for this sprite.
   *
   * @return event flags
   */
  public ClipEventFlags getEventFlags() {
    return eventFlags;
  }

  /**
   * Writes this instance to a bit stream.
   *
   * @param stream target bit stream
   * @param swfVersion used
   *
   * @throws IOException if an I/O error has occured
   */
  public void write(OutputBitStream stream, short swfVersion)
    throws IOException {
    stream.writeUI16(0); // reserved
    eventFlags.write(stream, swfVersion);
    for (Iterator iter = clipActionRecords.iterator(); iter.hasNext();) {
      ClipActionRecord record = (ClipActionRecord) iter.next();
      record.write(stream, swfVersion);
    }

    // write clipActionEndFlag (0, UI16 for flash <= 5, else UI32!)
    if (swfVersion <= 5) {
      stream.writeUI16(0);
    } else {
      stream.writeUI32(0);
    }
  }
}

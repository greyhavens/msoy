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
import com.jswiff.swfrecords.actions.ActionBlock;

import java.io.IOException;
import java.io.Serializable;


/**
 * This class defines an event handler for a sprite. Used within
 * <code>ClipActions</code>.
 *
 * @see ClipActions
 */
public final class ClipActionRecord implements Serializable {
  private ClipEventFlags eventFlags;
  private short keyCode;
  private ActionBlock actions;

  /**
   * Creates a new ClipActionRecord instance.
   *
   * @param eventFlags event flags defining the events this handler is supposed
   *        to react upon.
   */
  public ClipActionRecord(ClipEventFlags eventFlags) {
    this.eventFlags   = eventFlags;
    actions           = new ActionBlock();
  }

  /**
   * Creates a new ClipActionRecord instance, reading data from a bit stream.
   *
   * @param stream source bit stream
   * @param swfVersion SWF version
   *
   * @throws IOException if an I/O error has occured
   */
  public ClipActionRecord(InputBitStream stream, short swfVersion)
    throws IOException {
    eventFlags = new ClipEventFlags(stream, swfVersion);
    int actionRecordSize = (int) stream.readUI32();
    if ((swfVersion >= 6) && eventFlags.isKeyPress()) {
      keyCode = stream.readUI8();
      actionRecordSize--;
    }
    InputBitStream actionStream = new InputBitStream(
        stream.readBytes(actionRecordSize));
    actionStream.setANSI(stream.isANSI());
    actionStream.setShiftJIS(stream.isShiftJIS());
    actions = new ActionBlock(actionStream);
  }

  /**
   * Returns the action block to be performed when an event occurs. Use this
   * method to add actions to the action block.
   *
   * @return action block contained in handler
   */
  public ActionBlock getActions() {
    return actions;
  }

  /**
   * Returns the event flags indicating events this handler is supposed to
   * react upon.
   *
   * @return event flags
   */
  public ClipEventFlags getEventFlags() {
    return eventFlags;
  }

  /**
   * <p>
   * Sets the key code to trap. The keyPress event flag must be set, otherwise
   * this value is ignored.
   * </p>
   * 
   * <p>
   * For special keys (e.g. escape), use the constants provided in
   * <code>KeyCodes</code> (e.g. KEY_ESCAPE). For ASCII keys, use their ASCII
   * code.
   * </p>
   *
   * @param keyCode key code to trap
   */
  public void setKeyCode(short keyCode) {
    this.keyCode = keyCode;
  }

  /**
   * Returns the code of the key to trap. The keyPress event flag must be set,
   * otherwise this value is ignored.
   *
   * @return key code to trap
   */
  public short getKeyCode() {
    return keyCode;
  }

  /**
   * Writes this instance to a bit stream.
   *
   * @param stream target bit stream
   * @param swfVersion SWF version
   *
   * @throws IOException if an I/O error has occured
   */
  public void write(OutputBitStream stream, short swfVersion)
    throws IOException {
    eventFlags.write(stream, swfVersion);
    OutputBitStream actionStream = new OutputBitStream();
    actionStream.setANSI(stream.isANSI());
    actionStream.setShiftJIS(stream.isShiftJIS());
    actions.write(actionStream, true);
    byte[] actionBuffer  = actionStream.getData();
    int actionRecordSize = actionBuffer.length;
    if (eventFlags.isKeyPress()) {
      actionRecordSize++; // because of keyCode
    }
    stream.writeUI32(actionRecordSize);
    if (eventFlags.isKeyPress()) {
      stream.writeUI8(keyCode);
    }
    stream.writeBytes(actionBuffer);
  }
}

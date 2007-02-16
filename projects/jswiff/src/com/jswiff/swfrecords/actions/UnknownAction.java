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

import com.jswiff.io.InputBitStream;
import com.jswiff.io.OutputBitStream;

import java.io.IOException;


/**
 * This class implements a placeholder for unknown action records.
 */
public final class UnknownAction extends Action {
  private InputBitStream inStream;
  private byte[] actionData;

  /**
   * Creates a new UnknownAction instance.
   *
   * @param code action code indicationg the action type
   * @param data data contained in the action record
   */
  public UnknownAction(short code, byte[] data) {
    this.code    = code;
    actionData   = data;
  }

  UnknownAction(InputBitStream stream, short code) throws IOException {
    inStream    = stream;
    this.code   = code;
    if (inStream != null) {
      actionData = inStream.readBytes(inStream.available());
    } else {
      actionData = new byte[0];
    }
  }

  /**
   * Returns the record data as a byte array.
   *
   * @return record data
   */
  public byte[] getData() {
    return actionData;
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"Unknown action", code, data length</code>
   */
  public String toString() {
    return "Unknown action (code: " + getCode() + ", length: " +
    actionData.length + ")";
  }

  /*
   * Copies data from the input bit stream to the output bit stream. This can
   * be used to copy actions with unknown structure.
   */
  protected void writeData(
    OutputBitStream dataStream, OutputBitStream mainStream)
    throws IOException {
    dataStream.writeBytes(actionData);
  }
}

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

import com.jswiff.io.OutputBitStream;

import java.io.IOException;

import java.util.List;


/*
 * This class provides methods for writing action records.
 */
final class ActionWriter {
  private ActionWriter() {
    // prohibits instantiation
  }

  /*
   * Writes an action record to a bit stream.
   *
   * @param action a action record
   * @param stream a bit stream
   *
   * @throws IOException if an I/O error has occured
   */
  static void writeRecord(Action action, OutputBitStream stream)
    throws IOException {
    action.write(stream);
  }

  /*
   * Writes a list containing action records to a bit stream.
   *
   * @param actions a List containing Action instances
   * @param stream a bit stream
   *
   * @throws IOException if an I/O error has occured
   */
  static void writeRecords(List actions, OutputBitStream stream)
    throws IOException {
    for (int i = 0; i < actions.size(); i++) {
      Action record = (Action) actions.get(i);
      record.write(stream);
    }
    stream.writeUI8((short) 0); // ActionEndFlag
  }
}

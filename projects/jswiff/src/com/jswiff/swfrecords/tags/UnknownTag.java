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

import java.io.IOException;


/**
 * This class implements a container for tag data which cannot be interpreted
 * because the tag type is unknown (e.g. for new Flash versions).
 */
public class UnknownTag extends Tag {
  private byte[] inData;

  /**
   * Creates a new UnknownTag instance.
   *
   * @param code tag code (indicating the tag type)
   * @param data tag data
   */
  public UnknownTag(short code, byte[] data) {
    this.code   = code;
    inData      = data;
  }

  UnknownTag() {
  }

  /**
   * Returns the data contained in the tag.
   *
   * @return tag data
   */
  public byte[] getData() {
    return inData;
  }

  /**
   * Returns the string representation of the tag, containing tag code and data
   * size.
   *
   * @return string representation
   */
  public String toString() {
    return "Unknown tag (tag code: " + code + "; data size: " + getData().length +
    " bytes)";
  }

  protected void writeData(OutputBitStream outStream) throws IOException {
    outStream.writeBytes(inData);
  }

  void setData(byte[] data) {
    inData = data;
  }
}

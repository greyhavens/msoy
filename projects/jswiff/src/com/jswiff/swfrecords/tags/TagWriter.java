/*
 * JSwiff is an open source Java API for Macromedia Flash file generation
 * and manipulation
 *
 * Copyright (C) 2004-2006 Ralf Terdic (contact@jswiff.com)
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

import java.util.Iterator;
import java.util.List;


/**
 * This class contains methods used for writing tag headers and tags.
 */
public final class TagWriter {
  private TagWriter() {
    // prohibits instantiation
  }

  /**
   * Writes a tag to a bit stream.
   *
   * @param stream target bit stream
   * @param tag tag to be written
   * @param swfVersion SWF version
   *
   * @throws IOException if an I/O error occured
   */
  public static void writeTag(
    OutputBitStream stream, Tag tag, short swfVersion)
    throws IOException {
    tag.setSWFVersion(swfVersion);
    tag.write(stream);
  }

  /**
   * Writes a tag to a data buffer.
   *
   * @param tag tag to be written
   * @param swfVersion SWF version
   *
   * @return byte array containing tag
   *
   * @throws IOException if an I/O error occured
   */
  public static byte[] writeTag(Tag tag, short swfVersion)
    throws IOException {
    OutputBitStream stream = new OutputBitStream();
    writeTag(stream, tag, swfVersion);
    return stream.getData();
  }

  /**
   * Writes the tags from the passed list to a byte array. Additionally, an end
   * tag is written at the end.
   *
   * @param tags list of tags to be written
   * @param swfVersion SWF version
   * @param japanese specifies whether japanese encoding is to be used for
   *        strings
   *
   * @return byte array containing tags
   *
   * @throws IOException if an I/O error occured
   */
  public static byte[] writeTags(List tags, short swfVersion, boolean japanese)
    throws IOException {
    OutputBitStream stream = new OutputBitStream();
    if (swfVersion < 6) {
      if (japanese) {
        stream.setShiftJIS(true);
      } else {
        stream.setANSI(true);
      }
    }
    writeTags(stream, tags, swfVersion);
    return stream.getData();
  }

  /**
   * Writes the tags from the passed list to a bit stream. Additionally, an end
   * tag is written at the end.
   *
   * @param stream target bit stream
   * @param tags list of tags to be written
   * @param swfVersion SWF version
   *
   * @throws IOException if an I/O error occured
   */
  public static void writeTags(
    OutputBitStream stream, List tags, short swfVersion)
    throws IOException {
    for (Iterator i = tags.iterator(); i.hasNext();) {
      Tag tag = (Tag) i.next();
      writeTag(stream, tag, swfVersion);
    }

    // write end tag
    stream.writeUI16(0);
  }
}

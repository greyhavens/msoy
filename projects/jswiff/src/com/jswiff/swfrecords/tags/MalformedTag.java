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
 * This tag is used as container for malformed tag data which could not be
 * interpreted. The exception thrown while parsing the malformed tag is also
 * contained herein and can be used for error tracing.
 */
public final class MalformedTag extends Tag {
  private byte[] data;
  private TagHeader tagHeader;
  private Exception exception;

  /**
   * Creates a new MalformedTag instance. It makes no sense to add MalformedTag
   * instances to a SWF document, as SWF writers don't write their contents.
   *
   * @param tagHeader tag header
   * @param data raw tag data
   * @param exception exception thrown while parsing the tag
   */
  public MalformedTag(TagHeader tagHeader, byte[] data, Exception exception) {
    code             = TagConstants.MALFORMED;
    this.tagHeader   = tagHeader;
    this.data        = data;
    this.exception   = exception;
  }

  /**
   * Returns the raw data of the tag.
   *
   * @return tag data
   */
  public byte[] getData() {
    return data;
  }

  /**
   * Returns the exception which occured at parsing time.
   *
   * @return exception thrown while parsing tag
   */
  public Exception getException() {
    return exception;
  }

  /**
   * Returns the header of the tag.
   *
   * @return tag header
   */
  public TagHeader getTagHeader() {
    return tagHeader;
  }

  protected void writeData(OutputBitStream outStream) {
    // do nothing
  }

  void setData(byte[] data) {
    this.data = data;
  }

  void write(OutputBitStream stream) {
    // don't write malformed tags...
  }
}

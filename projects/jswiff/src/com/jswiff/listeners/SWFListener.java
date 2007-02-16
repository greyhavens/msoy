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

package com.jswiff.listeners;

import com.jswiff.swfrecords.SWFHeader;
import com.jswiff.swfrecords.tags.Tag;
import com.jswiff.swfrecords.tags.TagHeader;


/**
 * Base class for SWF listeners, which can be passed to an
 * <code>SWFReader</code>, offering a flexible way to define it's behaviour
 * before, during and after the parsing process.
 *
 * @see com.jswiff.SWFReader
 */
public abstract class SWFListener {
  /**
   * Contains code executed after parsing (after reading the end tag). Does
   * nothing by default, override in subclass to change this behavior.
   */
  public void postProcess() {
    // empty on purpose
  }

  /**
   * Contains code executed before parsing (before reading the SWF file
   * header). Does nothing by default, override in subclass to change this
   * behavior.
   */
  public void preProcess() {
    // empty on purpose
  }

  /**
   * Contains processing code for the SWF header. By default, this method
   * doesn't do anything, override in subclass to change this behavior.
   *
   * @param header the header of the SWF file
   */
  public void processHeader(SWFHeader header) {
    // empty on purpose
  }

  /**
   * Processes errors which occur during header parsing. By default, this
   * method prints the stack trace.
   *
   * @param e the exception which occured while parsing the header
   */
  public void processHeaderReadError(Exception e) {
    e.printStackTrace();
  }

  /**
   * <p>
   * Contains tag processing code. Doesn't do anything by default.
   * </p>
   * 
   * <p>
   * End tags are ignored. However, there is no need to process end tags, they
   * are implicitly read and written.
   * </p>
   *
   * @param tag the current tag read by the <code>SWFReader</code>
   * @param streamOffset the current stream offset
   */
  public void processTag(Tag tag, long streamOffset) {
    // empty on purpose
  }

  /**
   * <p>
   * Contains tag header processing code. By default, this method does nothing,
   * override in subclass to change this behavior.
   * </p>
   * 
   * <p>
   * End tag headers are ignored. However, there is no need to process end
   * tags, they are implicitly read and written.
   * </p>
   *
   * @param tagHeader the tag header
   */
  public void processTagHeader(TagHeader tagHeader) {
    // empty on purpose
  }

  /**
   * <p>
   * Contains error processing for tag header parsing. After invoking this
   * method on all registered listeners, the reader stops, not being aware of
   * the current tag's length and thus being unable to continue parsing.
   * </p>
   * 
   * <p>
   * By default, this method prints the exception's stack trace to the console.
   * </p>
   *
   * @param e the exception which occured during tag header parsing
   */
  public void processTagHeaderReadError(Exception e) {
    e.printStackTrace();
  }

  /**
   * <p>
   * Processes a tag parsing error. Returns a break condition which tells the
   * reader (<code>SWFReader</code>) whether to stop reading the rest of the
   * file. However, the reader invokes this method on all registered listeners
   * before stopping. In case the reader isn't supposed to stop, it creates a
   * <code>MalformedTag</code> instance from the tag header and data and
   * invokes <code>processTag()</code> before parsing the next tag.
   * </p>
   * 
   * <p>
   * By default, this method prints the tag header and the exception's stack
   * trace to the console and returns <code>true</code> (telling the reader to
   * stop).
   * </p>
   *
   * @param tagHeader header of the malformed tag
   * @param tagData the tag data as byte array
   * @param e the exception which occured while parsing the tag
   *
   * @return <code>true</code> if reader is supposed to stop reading further
   *         tags after error processing, else <code>false</code>
   */
  public boolean processTagReadError(
    TagHeader tagHeader, byte[] tagData, Exception e) {
    System.err.println(
      "Malformed tag (code: " + tagHeader.getCode() + ", length: " +
      tagHeader.getLength() + ")");
    e.printStackTrace();
    return true;
  }
}

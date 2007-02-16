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
import java.io.UnsupportedEncodingException;


/**
 * <p>
 * Instructs Flash Player to get a specified URL (e.g. an HTML file, an image
 * or another SWF movie) and to display it using a particular target (either a
 * browser frame or a level in Flash Player).
 * </p>
 * 
 * <p>
 * Several protocols are supported:
 * 
 * <ul>
 * <li>
 * conventional internet protocols (<code>http, https, ftp, mailto,
 * telnet</code>)
 * </li>
 * <li>
 * <code>file:///drive:/filename</code> for local file access
 * </li>
 * <li>
 * <code>print</code> used for printing a movie clip
 * </li>
 * <li>
 * <code>javascript</code> and  <code>vbscript</code> to execute script code in
 * the browser
 * </li>
 * <li>
 * <code>event</code> and <code>lingo</code> for Macromedia Director
 * interaction
 * </li>
 * </ul>
 * </p>
 * 
 * <p>
 * Usually, the specified target directs the URL content to a particular
 * browser frame (e.g. <code>_self</code>, <code>_parent</code>,
 * <code>_blank</code>). If the URL points to an SWF, the target can be a
 * string specifying the name of a movie clip instance or a document level
 * (e.g. <code>_level1</code>).
 * </p>
 * 
 * <p>
 * Performed stack operations: none
 * </p>
 * 
 * <p>
 * ActionScript equivalent: <code>getURL(), loadMovie()</code> operator
 * </p>
 *
 * @since SWF 3
 */
public final class GetURL extends Action {
  private String url;
  private String target;

  /**
   * Creates a new GetURL action. The <code>url</code> content will be
   * displayed at the specified <code>target</code>.
   *
   * @param url the URL to be loaded
   * @param target the target used to display the URL
   */
  public GetURL(String url, String target) {
    code          = ActionConstants.GET_URL;
    this.url      = url;
    this.target   = target;
  }

  /*
   * Creates a new GetURL action from a bit stream
   */
  GetURL(InputBitStream stream) throws IOException {
    code     = ActionConstants.GET_URL;
    url      = stream.readString();
    target   = stream.readString();
  }

  /**
   * Returns the size of this action record in bytes.
   *
   * @return size of this record
   *
   * @see Action#getSize()
   */
  public int getSize() {
    int size = 5;
    try {
      size += (url.getBytes("UTF-8").length + target.getBytes("UTF-8").length);
    } catch (UnsupportedEncodingException e) {
      // UTF-8 should be available
    }
    return size;
  }

  /**
   * Returns the target used to display the URL.
   *
   * @return target string
   */
  public String getTarget() {
    return target;
  }

  /**
   * Returns the URL to be loaded.
   *
   * @return URL string
   */
  public String getURL() {
    return url;
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"GetURL"</code>, the <code>url</code> and the
   *         <code>target</code>
   */
  public String toString() {
    return "GetURL url: '" + url + "' target: '" + target + "'";
  }

  protected void writeData(
    OutputBitStream dataStream, OutputBitStream mainStream)
    throws IOException {
    dataStream.writeString(url);
    dataStream.writeString(target);
  }
}

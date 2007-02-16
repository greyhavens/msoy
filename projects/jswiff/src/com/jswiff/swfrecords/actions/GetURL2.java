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
 * Gets contents from an URL or exchanges data with a server.
 * 
 * <p>
 * Performed stack operations:<br>
 * <code>pop target</code> (see <code>GetURL</code>)<br>
 * <code>pop url</code><br>
 * </p>
 * 
 * <p>
 * ActionScript equivalent: <code>getURL(), loadMovie(), loadMovieNum(),
 * loadVariables()</code>
 * </p>
 *
 * @see GetURL
 * @since SWF 4
 */
public final class GetURL2 extends Action {
  /** Indicates that the clip's variables should not be encoded and submitted. */
  public static final byte METHOD_NONE = 0;
  /** The clip's variables are encoded and sent with HTTP GET */
  public static final byte METHOD_GET  = 1;
  /** The clip's variables are encoded and sent with HTTP POST */
  public static final byte METHOD_POST = 2;
  private byte sendVarsMethod;
  private boolean loadTarget;
  private boolean loadVariables;

  /**
   * Creates a new GetURL2 action.
   *
   * @param sendVarsMethod HTTP request method (<code>METHOD_NONE,
   *        METHOD_GET</code> or <code>METHOD_POST</code>)
   * @param loadTarget <code>false</code> if target is a browser frame,
   *        <code>true</code> if it is a path to a clip (in slash syntax -
   *        /parentClip/childClip - or dot syntax - parentClip.childClip)
   * @param loadVariables if <code>true</code>, the server is expected to
   *        respond with an url encoded set of variables
   */
  public GetURL2(
    byte sendVarsMethod, boolean loadTarget, boolean loadVariables) {
    code                  = ActionConstants.GET_URL_2;
    this.sendVarsMethod   = sendVarsMethod;
    this.loadTarget       = loadTarget;
    this.loadVariables    = loadVariables;
  }

  /*
   * Reads a GetURL2 action from a bit stream.
   */
  GetURL2(InputBitStream stream) throws IOException {
    code             = ActionConstants.GET_URL_2;
    loadVariables   = stream.readBooleanBit();
    loadTarget      = stream.readBooleanBit();
    stream.readUnsignedBits(4); // 4 reserved bits
    sendVarsMethod   = (byte) stream.readUnsignedBits(2);
  }

  /**
   * Returns <code>false</code> if target is a browser frame, <code>true</code>
   * if it is a path to a movie clip (in slash or dot syntax)
   *
   * @return <code>true</code> if target is path to a clip
   */
  public boolean isLoadTarget() {
    return loadTarget;
  }

  /**
   * Returns <code>true</code> if the server is supposed to respond with an url
   * encoded set of variables
   *
   * @return <code>true</code> if server sends variables, otherwise false
   */
  public boolean isLoadVariables() {
    return loadVariables;
  }

  /**
   * Returns the HTTP request method (one of the values <code>METHOD_NONE,
   * METHOD_GET</code> or <code>METHOD_POST</code>)
   *
   * @return request method
   */
  public byte getSendVarsMethod() {
    return sendVarsMethod;
  }

  /**
   * Returns the size of this action record in bytes.
   *
   * @return size of this record
   *
   * @see Action#getSize()
   */
  public int getSize() {
    return 4;
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"GetURL2"</code>, the request method and the
   *         <code>loadTarget</code> and <code>loadVariables</code> flags.
   */
  public String toString() {
    String result = "GetURL2 sendVarsMethod: ";
    switch (sendVarsMethod) {
      case METHOD_GET:
        result += "GET";
        break;
      case METHOD_POST:
        result += "POST";
        break;
      default:
        result += "none";
    }
    result += (" loadTarget: " + loadTarget + " loadVariables: " +
    loadVariables);
    return result;
  }

  protected void writeData(
    OutputBitStream dataStream, OutputBitStream mainStream)
    throws IOException {
    dataStream.writeBooleanBit(loadVariables);
    dataStream.writeBooleanBit(loadTarget);
    dataStream.writeUnsignedBits(0, 4); // 4 reserved bits
    dataStream.writeUnsignedBits(sendVarsMethod, 2);
  }
}

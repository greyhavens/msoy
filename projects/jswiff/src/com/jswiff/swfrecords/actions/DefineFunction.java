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
 * This action defines a function.
 * </p>
 * 
 * <p>
 * Note: DefineFunction is rarely used as of SWF 7 and later; it has been
 * superseded by DefineFunction2.
 * </p>
 * 
 * <p>
 * Performed stack operations:
 * 
 * <ul>
 * <li>
 * standard function declarations do not touch the stack
 * </li>
 * <li>
 * when a function name is not specified, it is assumed that a function literal
 * (anonymous function) is declared. In this case, the declared function is
 * pushed to the stack so it can either be assigned or invoked:<br>
 * </li>
 * </ul>
 * </p>
 * 
 * <p>
 * Note: Use <code>Return</code> to declare the function's result. Otherwise
 * the function has no result, and <code>undefined</code> is pushed to stack
 * upon invocation.
 * </p>
 * 
 * <p>
 * ActionScript equivalents:
 * 
 * <ul>
 * <li>
 * standard function declaration, e.g.<br>
 * <code>myFunction(x) {<br>
 * return (x + 3);<br>}</code><br>
 * </li>
 * <li>
 * anonymous function declaration, e.g.<br>
 * <code>function (x) { x + 3 };</code><br>
 * </li>
 * <li>
 * anonymous function invocation, e.g.<br>
 * <code>function (x) { x + 3 } (1);</code><br>
 * </li>
 * <li>
 * method declaration
 * </li>
 * </ul>
 * </p>
 *
 * @see DefineFunction2
 * @since SWF 5
 */
public final class DefineFunction extends Action {
  private String name;
  private String[] parameters;
  private ActionBlock body;

  /**
   * Creates a new DefineFunction action. Use the empty string ("") as function
   * name for anonymous functions.
   *
   * @param functionName name of the function
   * @param parameters array of parameter names
   */
  public DefineFunction(String functionName, String[] parameters) {
    code              = ActionConstants.DEFINE_FUNCTION;
    this.name         = functionName;
    this.parameters   = parameters;
    body              = new ActionBlock();
  }

  /*
   * Creates a new DefineFunction action. Data is read from a bit stream.
   */
  DefineFunction(InputBitStream stream, InputBitStream mainStream)
    throws IOException {
    code   = ActionConstants.DEFINE_FUNCTION;
    name   = stream.readString();
    int numParams = stream.readUI16();
    if (numParams >= 0) {
      parameters = new String[numParams];
      for (int i = 0; i < numParams; i++) {
        parameters[i] = stream.readString();
      }
    }
    int codeSize               = stream.readUI16();

    // now read further actions from the main stream
    // read action block
    byte[] blockBuffer         = mainStream.readBytes(codeSize);
    InputBitStream blockStream = new InputBitStream(blockBuffer);
    blockStream.setANSI(stream.isANSI());
    blockStream.setShiftJIS(stream.isShiftJIS());
    body                       = new ActionBlock(blockStream);
  }

  /**
   * Returns an <code>ActionBlock</code> containing the function's body. Use
   * <code>addAction()</code> to add actions to the body.
   *
   * @return function body as action block
   */
  public ActionBlock getBody() {
    return body;
  }

  /**
   * Returns the name of the function (or the empty string for anonymous
   * functions).
   *
   * @return function name
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the names of the function parameters.
   *
   * @return array of parameter names
   */
  public String[] getParameters() {
    return parameters;
  }

  /**
   * Returns the size of this action record in bytes.
   *
   * @return size of this record
   *
   * @see Action#getSize()
   */
  public int getSize() {
    int size = 8 + body.getSize();
    try {
      // function name (unicode, not null-terminated)
      // (+ parameters.length: for each param a terminating 0)
      int paramLength = (parameters == null) ? 0 : parameters.length; 
      size += (name.getBytes("UTF-8").length + paramLength);
      for (int i = 0; i < paramLength; i++) {
        size += parameters[i].getBytes("UTF-8").length; // unicode, null-terminated
      }
    } catch (UnsupportedEncodingException e) {
      // UTF-8 should be available
    }
    return size;
  }

  /**
   * Adds an action record to the function body.
   *
   * @param action action record
   */
  public void addAction(Action action) {
    body.addAction(action);
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"DefineFunction"</code>, function name
   */
  public String toString() {
    return "DefineFunction " + name;
  }

  protected void writeData(
    OutputBitStream dataStream, OutputBitStream mainStream)
    throws IOException {
    dataStream.writeString(name);
    dataStream.writeUI16((parameters == null) ? 0 : parameters.length);
    if (parameters != null) {
      for (int i = 0; i < parameters.length; i++) {
        dataStream.writeString(parameters[i]);
      }
    }
    dataStream.writeUI16(body.getSize()); // codeSize
    body.write(mainStream, false);
  }
}

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
 * <p>
 * Defines a <code>with</code> action block which lets you specify an object in
 * order to access it's members without having  to repeatedly write the
 * object's name or it's path.
 * </p>
 * 
 * <p>
 * Performed stack operations:<br><code>pop object</code>
 * </p>
 * 
 * <p>
 * ActionScript equivalent: <code>with</code> statement
 * </p>
 *
 * @since SWF 5
 */
public final class With extends Action {
  private ActionBlock actionBlock;

  /**
   * Creates a new With action.
   */
  public With() {
    code          = ActionConstants.WITH;
    actionBlock   = new ActionBlock();
  }

  /*
   * Creates a new With instance.
   */
  With(InputBitStream stream, InputBitStream mainStream)
    throws IOException {
    code = ActionConstants.WITH;
    int blockSize              = stream.readUI16();
    byte[] blockBuffer         = mainStream.readBytes(blockSize);
    InputBitStream blockStream = new InputBitStream(blockBuffer);
    blockStream.setANSI(stream.isANSI());
    blockStream.setShiftJIS(stream.isShiftJIS());
    actionBlock                = new ActionBlock(blockStream);
  }

  /**
   * Returns the size of this action record in bytes.
   *
   * @return size of this record
   *
   * @see Action#getSize()
   */
  public int getSize() {
    return actionBlock.getSize() + 5;
  }

  /**
   * Returns an <code>ActionBlock</code> containing the "with" block (can be
   * used to add actions to the block).
   *
   * @return "with" action block
   */
  public ActionBlock getWithBlock() {
    return actionBlock;
  }

  /**
   * Returns a short description of this action, along with the size in bytes
   * of the contained action block and the number of contained actions.
   *
   * @return <code>"With"</code>, action block size (in bytes), number of
   *         actions
   */
  public String toString() {
    return "With";
  }

  protected void writeData(
    OutputBitStream dataStream, OutputBitStream mainStream)
    throws IOException {
    dataStream.writeUI16(actionBlock.getSize());
    actionBlock.write(mainStream, false);
  }
}

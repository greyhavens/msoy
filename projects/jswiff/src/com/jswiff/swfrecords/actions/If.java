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
 * Evaluates a condition to determine the next action in an SWF file. If the
 * condition is true, the execution continues at the action with the specified
 * label.
 * </p>
 * 
 * <p>
 * Performed stack operations:<br>
 * <code>pop cond</code> (the condition - must evaluate to 0 or 1, or as of SWF
 * 5 to <code>false</code> or <code>true</code>)
 * </p>
 * 
 * <p>
 * ActionScript equivalent: <code>if</code> statement
 * </p>
 *
 * @see ActionBlock
 * @since SWF 4
 */
public final class If extends Branch {
  private short branchOffset;
  private String branchLabel;

  /**
   * Creates a new If action.<br>
   * The <code>branchLabel</code> parameter specifies the target of the jump
   * in case the condition is fulfilled. This label must be identical to the
   * one assigned to the action record the execution is supposed to continue
   * at. Assign <code>ActionBlock.LABEL_END</code> in order to jump to the end
   * of the action block.
   *
   * @param branchLabel label of the action the execution is supposed to
   *        continue at
   */
  public If(String branchLabel) {
    code               = ActionConstants.IF;
    this.branchLabel   = branchLabel;
  }

  /*
   * Reads an If action from a bit stream.
   */
  If(InputBitStream stream) throws IOException {
    code           = ActionConstants.IF;
    branchOffset   = stream.readSI16();
  }

  /**
   * Returns the label of the action the execution is supposed to continue at,
   * if the condition is fulfilled.
   *
   * @return branch label
   */
  public String getBranchLabel() {
    return branchLabel;
  }

  /**
   * Returns the branch offset.
   *
   * @return branch offset
   */
  public short getBranchOffset() {
    return branchOffset;
  }

  /**
   * Returns the size of this action record in bytes.
   *
   * @return size of this record
   *
   * @see Action#getSize()
   */
  public int getSize() {
    return 5; // 1 (code) + 2 (data length) + 2 (branch offset)
  }

  /**
   * Returns a short description of this action.
   *
   * @return <code>"If", branchLabel</code>
   */
  public String toString() {
    return "If branchLabel: " + branchLabel;
  }

  protected void writeData(
    OutputBitStream dataStream, OutputBitStream mainStream)
    throws IOException {
    dataStream.writeSI16(branchOffset);
  }

  void setBranchLabel(String branchLabel) {
    this.branchLabel = branchLabel;
  }

  void setBranchOffset(short branchOffset) {
    this.branchOffset = branchOffset;
  }
}

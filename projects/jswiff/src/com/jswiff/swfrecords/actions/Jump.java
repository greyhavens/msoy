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
 * Creates an unconditional branch. The execution continues at the action with
 * the specified label.
 * </p>
 * 
 * <p>
 * Performed stack operations: none
 * </p>
 * 
 * <p>
 * ActionScript equivalent: none (used internally)
 * </p>
 *
 * @see ActionBlock
 * @since SWF 4
 */
public final class Jump extends Branch {
  private short branchOffset;
  private String branchLabel;

  /**
   * Creates a new Jump action.<br>
   * The <code>branchLabel</code> parameter specifies the target of the
   * branch. This label must be identical to the one assigned to the action
   * record the execution is supposed to continue at. Assign
   * <code>ActionBlock.LABEL_END</code> in order to jump to the end of the
   * action block.
   *
   * @param branchLabel label of the action the execution is supposed to
   *        continue at
   */
  public Jump(String branchLabel) {
    code               = ActionConstants.JUMP;
    this.branchLabel   = branchLabel;
  }

  Jump(short branchOffset) {
    code                = ActionConstants.JUMP;
    this.branchOffset   = branchOffset;
  }

  Jump(InputBitStream stream) throws IOException {
    code           = ActionConstants.JUMP;
    branchOffset   = stream.readSI16();
  }

  /**
   * Returns the label of the action the execution is supposed to continue at.
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
   * @return <code>"Jump", branchLabel</code>
   */
  public String toString() {
    return "Jump branchLabel: " + branchLabel;
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

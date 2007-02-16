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

import java.util.List;


/**
 * <p>
 * This action defines handlers for exceptional conditions (exceptions). After
 * defining the mandatory try block, you can define a catch block and/or a
 * finally block.
 * </p>
 * 
 * <p>
 * Performed stack operations: none
 * </p>
 * 
 * <p>
 * ActionScript equivalent: <code>try..catch..finally</code> keywords
 * </p>
 *
 * @since SWF 7
 */
public final class Try extends Action {
  private boolean catchInRegister;
  private String catchVariable;
  private short catchRegister;
  private ActionBlock tryBlock;
  private ActionBlock catchBlock;
  private ActionBlock finallyBlock;

  /**
   * Creates a new Try action. You can specify a name of a variable the caught
   * object is put into.
   *
   * @param catchVar catch variable name
   */
  public Try(String catchVar) {
    this();
    this.catchVariable = catchVar;
  }

  /**
   * Creates a new Try action. You can specify the register number the caught
   * object is put into.
   *
   * @param catchRegister catch register number
   */
  public Try(short catchRegister) {
    this();
    this.catchRegister   = catchRegister;
    catchInRegister      = true;
  }

  Try(InputBitStream stream, InputBitStream mainStream)
    throws IOException {
    code = ActionConstants.TRY;
    short flags = stream.readUI8(); // 5 reserved bits - ignore
    catchInRegister = ((flags & 4) != 0);
    boolean hasFinallyBlock = ((flags & 2) != 0);
    boolean hasCatchBlock   = ((flags & 1) != 0);
    int trySize             = stream.readUI16();
    int catchSize           = stream.readUI16();
    int finallySize         = stream.readUI16();
    if (catchInRegister) {
      catchRegister = stream.readUI8();
    } else {
      catchVariable = stream.readString();
    }

    // now read further actions from the main stream
    // read try block
    byte[] blockBuffer         = mainStream.readBytes(trySize);
    InputBitStream blockStream = new InputBitStream(blockBuffer);
    blockStream.setANSI(stream.isANSI());
    blockStream.setShiftJIS(stream.isShiftJIS());
    tryBlock                   = new ActionBlock(blockStream);
    removeTryJump();
    // read catch block
    if (hasCatchBlock) {
      blockBuffer   = mainStream.readBytes(catchSize);
      blockStream   = new InputBitStream(blockBuffer);
      blockStream.setANSI(stream.isANSI());
      blockStream.setShiftJIS(stream.isShiftJIS());
      catchBlock    = new ActionBlock(blockStream);
    } else {
      catchBlock = new ActionBlock();
    }

    // read finally block
    if (hasFinallyBlock) {
      blockBuffer    = mainStream.readBytes(finallySize);
      blockStream    = new InputBitStream(blockBuffer);
      blockStream.setANSI(stream.isANSI());
      blockStream.setShiftJIS(stream.isShiftJIS());
      finallyBlock   = new ActionBlock(blockStream);
    } else {
      finallyBlock = new ActionBlock();
    }
  }

  private Try() {
    code           = ActionConstants.TRY;
    tryBlock       = new ActionBlock();
    catchBlock     = new ActionBlock();
    finallyBlock   = new ActionBlock();
  }

  /**
   * Returns the catch action block. Use <code>addToCatchBlock()</code> method
   * to add new action records to the catch action block.
   *
   * @return catch action block
   */
  public ActionBlock getCatchBlock() {
    return catchBlock;
  }

  /**
   * Returns the number of the register the exception is catched into.
   *
   * @return catch variable name
   */
  public short getCatchRegister() {
    return catchRegister;
  }

  /**
   * Returns the name of the variable the exception is catched into.
   *
   * @return catch variable name
   */
  public String getCatchVariable() {
    return catchVariable;
  }

  /**
   * Returns the finally action block.  Use <code>addToFinallyBlock()</code>
   * method to add new action records to the finally action block.
   *
   * @return finally action block
   */
  public ActionBlock getFinallyBlock() {
    return finallyBlock;
  }

  /**
   * Returns the size of this action record in bytes.
   *
   * @return size of this record
   *
   * @see Action#getSize()
   */
  public int getSize() {
    int size = 10 + tryBlock.getSize() + catchBlock.getSize() +
      finallyBlock.getSize();
    if (catchInRegister) {
      size++; // one byte
    } else {
      try {
        size += (catchVariable.getBytes("UTF-8").length + 1); // unicode, null-terminated
      } catch (UnsupportedEncodingException e) {
        // UTF-8 should be available
      }
    }
    return size;
  }

  /**
   * <p>
   * Returns the try action block. Use <code>addToTryBlock()</code> method to
   * add new action records to the try action block.
   * </p>
   * 
   * <p>
   * Note: if a catch block is present, the try block contains an implicit
   * <code>Jump</code> at the end pointing at the first action after the catch
   * block, causing it's actions to be skipped in case no exception is thrown.
   * </p>
   *
   * @return try action block
   */
  public ActionBlock getTryBlock() {
    return tryBlock;
  }

  /**
   * Adds a new action record to the catch block.
   *
   * @param action an action record
   */
  public void addToCatch(Action action) {
    catchBlock.addAction(action);
  }

  /**
   * Adds a new action record to the finally block.
   *
   * @param action an action record
   */
  public void addToFinally(Action action) {
    finallyBlock.addAction(action);
  }

  /**
   * Adds a new action record to the try block.
   *
   * @param action an action record
   */
  public void addToTry(Action action) {
    tryBlock.addAction(action);
  }

  /**
   * Checks if the catched exception is put into a register or into a variable.
   *
   * @return <code>true</code> if exception is catched into a register, else
   *         <code>false</code>
   */
  public boolean catchInRegister() {
    return catchInRegister;
  }

  /**
   * Checks if there is a catch block.
   *
   * @return <code>true</code> if there is a catch block, otherwise
   *         <code>false</code>
   */
  public boolean hasCatchBlock() {
    return (catchBlock.getActions().size() > 0);
  }

  /**
   * Returns the finally action block. Can be used to add new actions to the
   * finally block.
   *
   * @return the finally action block
   */
  public boolean hasFinallyBlock() {
    return (finallyBlock.getActions().size() > 0);
  }

  protected void writeData(
    OutputBitStream dataStream, OutputBitStream mainStream)
    throws IOException {
    dataStream.writeUnsignedBits(0, 5); // 5 reserved bits
    dataStream.writeBooleanBit(catchInRegister);
    dataStream.writeBooleanBit(finallyBlock.getActions().size() > 0);
    dataStream.writeBooleanBit(catchBlock.getActions().size() > 0);
    dataStream.writeUI16(tryBlock.getSize());
    dataStream.writeUI16(catchBlock.getSize());
    dataStream.writeUI16(finallyBlock.getSize());
    if (catchInRegister) {
      dataStream.writeUI8(catchRegister);
    } else {
      dataStream.writeString(catchVariable);
    }
    tryBlock.write(mainStream, false);
    if (catchBlock.getActions().size() > 0) {
      // if there is a catch block, execute a jump to the finally block
      new Jump((short) catchBlock.getSize()).write(mainStream);
    }
    catchBlock.write(mainStream, false);
    finallyBlock.write(mainStream, false);
  }

  private void removeTryJump() {
    // Removes the jump to the first action of the finally block.
    // This jump is the last action of the try block when there is a catch block.
    List actions = tryBlock.getActions();
    if (actions.size() > 0) {
      Action lastAction = (Action) actions.get(actions.size() - 1);
      if (lastAction.getCode() == ActionConstants.JUMP) {
        if (((Jump) lastAction).getBranchLabel().equals(ActionBlock.LABEL_OUT)) {
          actions.remove(actions.size() - 1);
        }
      }
    }
  }
}

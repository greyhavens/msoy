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

import java.io.IOException;


/*
 * This class contains methods for reading action records.
 */
final class ActionReader {
  private ActionReader() {
    // prohibits instantiation
  }

  /*
   * Reads an action record from a bit stream.
   */
  static Action readRecord(InputBitStream stream) throws IOException {
    Action action;
    InputBitStream actionStream = null; // contains action record data
    int offset                  = (int) stream.getOffset();
    short actionCode            = stream.readUI8();
    boolean longHeader          = (actionCode >= 0x80);
    int length                  = longHeader ? stream.readUI16() : 0;
    if (length > 0) {
      actionStream = new InputBitStream(stream.readBytes(length));
      actionStream.setANSI(stream.isANSI());
      actionStream.setShiftJIS(stream.isShiftJIS());
    }
    switch (actionCode) {
      case ActionConstants.ADD:
        action = new Add();
        break;
      case ActionConstants.ADD_2:
        action = new Add2();
        break;
      case ActionConstants.AND:
        action = new And();
        break;
      case ActionConstants.ASCII_TO_CHAR:
        action = new AsciiToChar();
        break;
      case ActionConstants.BIT_AND:
        action = new BitAnd();
        break;
      case ActionConstants.BIT_L_SHIFT:
        action = new BitLShift();
        break;
      case ActionConstants.BIT_OR:
        action = new BitOr();
        break;
      case ActionConstants.BIT_R_SHIFT:
        action = new BitRShift();
        break;
      case ActionConstants.BIT_U_R_SHIFT:
        action = new BitURShift();
        break;
      case ActionConstants.BIT_XOR:
        action = new BitXor();
        break;
      case ActionConstants.CALL:
        // MM bug here: high bit of code set although length=0
        action = new Call();
        break;
      case ActionConstants.CALL_FUNCTION:
        action = new CallFunction();
        break;
      case ActionConstants.CALL_METHOD:
        action = new CallMethod();
        break;
      case ActionConstants.CAST_OP:
        action = new CastOp();
        break;
      case ActionConstants.CHAR_TO_ASCII:
        action = new CharToAscii();
        break;
      case ActionConstants.CLONE_SPRITE:
        action = new CloneSprite();
        break;
      case ActionConstants.CONSTANT_POOL:
        action = new ConstantPool(actionStream);
        break;
      case ActionConstants.DECREMENT:
        action = new Decrement();
        break;
      case ActionConstants.DEFINE_FUNCTION:
        action = new DefineFunction(actionStream, stream);
        break;
      case ActionConstants.DEFINE_FUNCTION_2:
        action = new DefineFunction2(actionStream, stream);
        break;
      case ActionConstants.DEFINE_LOCAL:
        action = new DefineLocal();
        break;
      case ActionConstants.DEFINE_LOCAL_2:
        action = new DefineLocal2();
        break;
      case ActionConstants.DELETE:
        action = new Delete();
        break;
      case ActionConstants.DELETE_2:
        action = new Delete2();
        break;
      case ActionConstants.DIVIDE:
        action = new Divide();
        break;
      case ActionConstants.END:
        action = new End();
        break;
      case ActionConstants.END_DRAG:
        action = new EndDrag();
        break;
      case ActionConstants.ENUMERATE:
        action = new Enumerate();
        break;
      case ActionConstants.ENUMERATE_2:
        action = new Enumerate2();
        break;
      case ActionConstants.EQUALS:
        action = new Equals();
        break;
      case ActionConstants.EQUALS_2:
        action = new Equals2();
        break;
      case ActionConstants.EXTENDS:
        action = new Extends();
        break;
      case ActionConstants.GET_MEMBER:
        action = new GetMember();
        break;
      case ActionConstants.GET_PROPERTY:
        action = new GetProperty();
        break;
      case ActionConstants.GET_TIME:
        action = new GetTime();
        break;
      case ActionConstants.GET_URL:
        action = new GetURL(actionStream);
        break;
      case ActionConstants.GET_URL_2:
        action = new GetURL2(actionStream);
        break;
      case ActionConstants.GET_VARIABLE:
        action = new GetVariable();
        break;
      case ActionConstants.GO_TO_FRAME:
        action = new GoToFrame(actionStream);
        break;
      case ActionConstants.GO_TO_FRAME_2:
        action = new GoToFrame2(actionStream);
        break;
      case ActionConstants.GO_TO_LABEL:
        action = new GoToLabel(actionStream);
        break;
      case ActionConstants.GREATER:
        action = new Greater();
        break;
      case ActionConstants.IF:
        action = new If(actionStream);
        break;
      case ActionConstants.IMPLEMENTS_OP:
        action = new ImplementsOp();
        break;
      case ActionConstants.INCREMENT:
        action = new Increment();
        break;
      case ActionConstants.INIT_ARRAY:
        action = new InitArray();
        break;
      case ActionConstants.INIT_OBJECT:
        action = new InitObject();
        break;
      case ActionConstants.INSTANCE_OF:
        action = new InstanceOf();
        break;
      case ActionConstants.JUMP:
        action = new Jump(actionStream);
        break;
      case ActionConstants.LESS:
        action = new Less();
        break;
      case ActionConstants.LESS_2:
        action = new Less2();
        break;
      case ActionConstants.M_B_ASCII_TO_CHAR:
        action = new MBAsciiToChar();
        break;
      case ActionConstants.M_B_CHAR_TO_ASCII:
        action = new MBCharToAscii();
        break;
      case ActionConstants.M_B_STRING_EXTRACT:
        action = new MBStringExtract();
        break;
      case ActionConstants.M_B_STRING_LENGTH:
        action = new MBStringLength();
        break;
      case ActionConstants.MODULO:
        action = new Modulo();
        break;
      case ActionConstants.MULTIPLY:
        action = new Multiply();
        break;
      case ActionConstants.NEW_METHOD:
        action = new NewMethod();
        break;
      case ActionConstants.NEW_OBJECT:
        action = new NewObject();
        break;
      case ActionConstants.NEXT_FRAME:
        action = new NextFrame();
        break;
      case ActionConstants.NOT:
        action = new Not();
        break;
      case ActionConstants.OR:
        action = new Or();
        break;
      case ActionConstants.PLAY:
        action = new Play();
        break;
      case ActionConstants.POP:
        action = new Pop();
        break;
      case ActionConstants.PREVIOUS_FRAME:
        action = new PreviousFrame();
        break;
      case ActionConstants.PUSH:
        action = new Push(actionStream);
        break;
      case ActionConstants.PUSH_DUPLICATE:
        action = new PushDuplicate();
        break;
      case ActionConstants.RANDOM_NUMBER:
        action = new RandomNumber();
        break;
      case ActionConstants.REMOVE_SPRITE:
        action = new RemoveSprite();
        break;
      case ActionConstants.RETURN:
        action = new Return();
        break;
      case ActionConstants.SET_MEMBER:
        action = new SetMember();
        break;
      case ActionConstants.SET_PROPERTY:
        action = new SetProperty();
        break;
      case ActionConstants.SET_TARGET:
        action = new SetTarget(actionStream);
        break;
      case ActionConstants.SET_TARGET_2:
        action = new SetTarget2();
        break;
      case ActionConstants.SET_VARIABLE:
        action = new SetVariable();
        break;
      case ActionConstants.STACK_SWAP:
        action = new StackSwap();
        break;
      case ActionConstants.START_DRAG:
        action = new StartDrag();
        break;
      case ActionConstants.STOP:
        action = new Stop();
        break;
      case ActionConstants.STOP_SOUNDS:
        action = new StopSounds();
        break;
      case ActionConstants.STORE_REGISTER:
        action = new StoreRegister(actionStream);
        break;
      case ActionConstants.STRICT_EQUALS:
        action = new StrictEquals();
        break;
      case ActionConstants.STRING_ADD:
        action = new StringAdd();
        break;
      case ActionConstants.STRING_EQUALS:
        action = new StringEquals();
        break;
      case ActionConstants.STRING_EXTRACT:
        action = new StringExtract();
        break;
      case ActionConstants.STRING_GREATER:
        action = new StringGreater();
        break;
      case ActionConstants.STRING_LENGTH:
        action = new StringLength();
        break;
      case ActionConstants.STRING_LESS:
        action = new StringLess();
        break;
      case ActionConstants.SUBTRACT:
        action = new Subtract();
        break;
      case ActionConstants.TARGET_PATH:
        action = new TargetPath();
        break;
      case ActionConstants.THROW:
        action = new Throw();
        break;
      case ActionConstants.TOGGLE_QUALITY:
        action = new ToggleQuality();
        break;
      case ActionConstants.TO_INTEGER:
        action = new ToInteger();
        break;
      case ActionConstants.TO_NUMBER:
        action = new ToNumber();
        break;
      case ActionConstants.TO_STRING:
        action = new ToString();
        break;
      case ActionConstants.TRACE:
        action = new Trace();
        break;
      case ActionConstants.TRY:
        action = new Try(actionStream, stream);
        break;
      case ActionConstants.TYPE_OF:
        action = new TypeOf();
        break;
      case ActionConstants.WAIT_FOR_FRAME:
        action = new WaitForFrame(actionStream);
        break;
      case ActionConstants.WAIT_FOR_FRAME_2:
        action = new WaitForFrame2(actionStream);
        break;
      case ActionConstants.WITH:
        action = new With(actionStream, stream);
        break;
      default:
        action = new UnknownAction(actionStream, actionCode);
    }

    // fool a decompilation protection which pretends an action record to be
    // longer than it actually is in order to hide subsequent actions
    int delta = action.getSize() - (length + (longHeader ? 3 : 1));
    if (delta < 0) {
      stream.move(delta);
    }
    action.setOffset(offset);
    return action;
  }
}

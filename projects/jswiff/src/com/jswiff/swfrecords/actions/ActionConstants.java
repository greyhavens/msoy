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

/**
 * This class contains constants regarding action records.
 */
public final class ActionConstants {
  /** End action */
  public static final int END                = 0;
  /** Addition */
  public static final int ADD                = 0x0a;
  /** Addition (as of SWF 5) */
  public static final int ADD_2              = 0x47;
  /** Boolean AND */
  public static final int AND                = 0x10;
  /** ASCII to char conversion (deprecated as of SWF 5) */
  public static final int ASCII_TO_CHAR      = 0x33;
  /** Bitwise AND */
  public static final int BIT_AND            = 0x60;
  /** Bitwise left shift */
  public static final int BIT_L_SHIFT        = 0x63;
  /** Bitwise OR */
  public static final int BIT_OR             = 0x61;
  /** Bitwise right shift */
  public static final int BIT_R_SHIFT        = 0x64;
  /** Bitwise unsigned right shift */
  public static final int BIT_U_R_SHIFT      = 0x65;
  /** Bitwise XOR */
  public static final int BIT_XOR            = 0x62;
  /** Execute script attached to a specified frame (deprecated since SWF 5) */
  public static final int CALL               = 0x9e;
  /** Invoke a function */
  public static final int CALL_FUNCTION      = 0x3d;
  /** Invoke a method */
  public static final int CALL_METHOD        = 0x52;
  /** Type cast */
  public static final int CAST_OP            = 0x2b;
  /** Char to ASCII conversion (deprecated as of SWF 5) */
  public static final int CHAR_TO_ASCII      = 0x32;
  /** Duplicate a sprite */
  public static final int CLONE_SPRITE       = 0x24;
  /** Create new constant pool */
  public static final int CONSTANT_POOL      = 0x88;
  /** Decrement by one */
  public static final int DECREMENT          = 0x51;
  /** Define a function */
  public static final int DEFINE_FUNCTION    = 0x9b;
  /** Define a function (as of SWF 7) */
  public static final int DEFINE_FUNCTION_2  = 0x8e;
  /** Define and initialize local variable */
  public static final int DEFINE_LOCAL       = 0x3c;
  /** Define local variable */
  public static final int DEFINE_LOCAL_2     = 0x41;
  /** Delete object property (to free memory) */
  public static final int DELETE             = 0x3a;
  /** Destroy object reference */
  public static final int DELETE_2           = 0x3b;
  /** Divide two numbers */
  public static final int DIVIDE             = 0x0d;
  /** Ends drag operation, if any */
  public static final int END_DRAG           = 0x28;
  /** Push object's property names to stack */
  public static final int ENUMERATE          = 0x46;
  /** Push object's property names to stack (stack based argument passing) */
  public static final int ENUMERATE_2        = 0x55;
  /** Test two numbers for equality */
  public static final int EQUALS             = 0x0e;
  /** Test two items for equality, takeing data types into account */
  public static final int EQUALS_2           = 0x49;
  /** Create inheritance relationship between two classes */
  public static final int EXTENDS            = 0x69;
  /** Retrieve member value from object */
  public static final int GET_MEMBER         = 0x4e;
  /** Return value of movie property */
  public static final int GET_PROPERTY       = 0x22;
  /** Get time since movie started playing */
  public static final int GET_TIME           = 0x34;
  /** Get a specified URL */
  public static final int GET_URL            = 0x83;
  /** Get contents from URL or exchange data with server */
  public static final int GET_URL_2          = 0x9a;
  /** Get variable value */
  public static final int GET_VARIABLE       = 0x1c;
  /** Go to specified frame */
  public static final int GO_TO_FRAME        = 0x81;
  /** Go to specified frame (stack based) */
  public static final int GO_TO_FRAME_2      = 0x9f;
  /** Go to labeled frame */
  public static final int GO_TO_LABEL        = 0x8c;
  /** Test if number is greater than another */
  public static final int GREATER            = 0x67;
  /** Evaluate condition */
  public static final int IF                 = 0x9d;
  /** Specifies interface a class implements */
  public static final int IMPLEMENTS_OP      = 0x2c;
  /** Decrement by one */
  public static final int INCREMENT          = 0x50;
  /** Create array and initialize it with stack values */
  public static final int INIT_ARRAY         = 0x42;
  /** Create object and initialize it with stack values */
  public static final int INIT_OBJECT        = 0x43;
  /** Determine if object is instance of a class */
  public static final int INSTANCE_OF        = 0x54;
  /** Unconditional branch to labeled action */
  public static final int JUMP               = 0x99;
  /** Tests if number is less than another */
  public static final int LESS               = 0x0f;
  /** Tests if number is less than another, taking account of data types */
  public static final int LESS_2             = 0x48;
  /** Convert ASCII to multibyte char (deprecated as of SWF 5) */
  public static final int M_B_ASCII_TO_CHAR  = 0x37;
  /** Convert multibyte char to ascii (deprecated as of SWF 5) */
  public static final int M_B_CHAR_TO_ASCII  = 0x36;
  /** Extract substring from string (deprecated as of SWF 5) */
  public static final int M_B_STRING_EXTRACT = 0x35;
  /** Compute string length (deprecated as of SWF 5) */
  public static final int M_B_STRING_LENGTH  = 0x31;
  /** Calculate remainder of division between two numbers */
  public static final int MODULO             = 0x3f;
  /** Compute product of two numbers */
  public static final int MULTIPLY           = 0x0c;
  /** Create new object (<code>NEW_OBJECT</code> is likely to be used instead) */
  public static final int NEW_METHOD         = 0x53;
  /** Create a new object, invoking a constructor */
  public static final int NEW_OBJECT         = 0x40;
  /** Advance to next frame */
  public static final int NEXT_FRAME         = 0x04;
  /** Boolean NOT */
  public static final int NOT                = 0x12;
  /** Boolean OR */
  public static final int OR                 = 0x11;
  /** Start playing at current frame */
  public static final int PLAY               = 0x06;
  /** Remove top of stack */
  public static final int POP                = 0x17;
  /** Go back to previous frame */
  public static final int PREVIOUS_FRAME     = 0x05;
  /** Push at least one value to stack */
  public static final int PUSH               = 0x96;
  /** Duplicate top of stack */
  public static final int PUSH_DUPLICATE     = 0x4c;
  /** Calculate random number */
  public static final int RANDOM_NUMBER      = 0x30;
  /** Remove clone sprite */
  public static final int REMOVE_SPRITE      = 0x25;
  /** Return to calling function */
  public static final int RETURN             = 0x3e;
  /** Populate object's member with given value */
  public static final int SET_MEMBER         = 0x4f;
  /** Set movie property */
  public static final int SET_PROPERTY       = 0x23;
  /** Change context of subsequent actions (deprecated as of SWF 5) */
  public static final int SET_TARGET         = 0x8b;
  /** Change context of subsequent actions (deprecated as of SWF 5) */
  public static final int SET_TARGET_2       = 0x20;
  /** Set variable value */
  public static final int SET_VARIABLE       = 0x1d;
  /** Swap the two items on top of stack */
  public static final int STACK_SWAP         = 0x4d;
  /** Make target sprite draggable */
  public static final int START_DRAG         = 0x27;
  /** Stop playing at current frame */
  public static final int STOP               = 0x07;
  /** Mute all playing sounds */
  public static final int STOP_SOUNDS        = 0x09;
  /** Store top of stack into register */
  public static final int STORE_REGISTER     = 0x87;
  /** Check for equality taking data types into account */
  public static final int STRICT_EQUALS      = 0x66;
  /** Concatenate strings (deprecated since SWF 5) */
  public static final int STRING_ADD         = 0x21;
  /** Test strings for equality (deprecated since SWF 5) */
  public static final int STRING_EQUALS      = 0x13;
  /** Extract substring from string (deprecated as of SWF 5) */
  public static final int STRING_EXTRACT     = 0x15;
  /** Test whether string is greater than another */
  public static final int STRING_GREATER     = 0x68;
  /** Compute string length (deprecated as of SWF 5) */
  public static final int STRING_LENGTH      = 0x14;
  /** Test whether string is less than another */
  public static final int STRING_LESS        = 0x29;
  /** Compute difference between two numbers */
  public static final int SUBTRACT           = 0x0b;
  /** Return target path of clip */
  public static final int TARGET_PATH        = 0x45;
  /** Throw an exception */
  public static final int THROW              = 0x2a;
  /** Toggle display quality (deprecated as of SWF 5) */
  public static final int TOGGLE_QUALITY     = 0x08;
  /** Convert item to integer */
  public static final int TO_INTEGER         = 0x18;
  /** Convert item to number */
  public static final int TO_NUMBER          = 0x4a;
  /** Convert item to string */
  public static final int TO_STRING          = 0x4b;
  /** Send debugging output in test mode */
  public static final int TRACE              = 0x26;
  /** Define handlers for exceptions */
  public static final int TRY                = 0x8f;
  /** Return item type */
  public static final int TYPE_OF            = 0x44;
  /** Check if specified frame is loaded (deprecated as of SWF 5) */
  public static final int WAIT_FOR_FRAME     = 0x8a;
  /** Check if specified frame is loaded (stack based, deprecated as of SWF 5) */
  public static final int WAIT_FOR_FRAME_2   = 0x8d;
  /** Define <code>with</code> action block */
  public static final int WITH               = 0x94;

  private ActionConstants() {
    // prohibits instantiation
  }
}

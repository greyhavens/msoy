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
import java.io.Serializable;
import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
import java.util.List;


/**
 * <p>
 * Pushes one or more values to the stack. Add <code>Push.StackValue</code>
 * instances using <code>addValue()</code>.
 * </p>
 * 
 * <p>
 * Performed stack operations: addition of one or more values to stack.
 * </p>
 * 
 * <p>
 * ActionScript equivalent: none (used internally, e.g. for parameter passing).
 * </p>
 *
 * @since SWF 4
 */
public final class Push extends Action {
  private List values = new ArrayList();

  /**
   * Creates a new Push action.
   */
  public Push() {
    code = ActionConstants.PUSH;
  }

  /*
   * Reads a Push action from a bit stream.
   */
  Push(InputBitStream stream) throws IOException {
    code = ActionConstants.PUSH;
    while (stream.available() > 0) {
      StackValue value = new StackValue(stream);
      values.add(value);
    }
  }

  /**
   * Returns the size of this action record in bytes.
   *
   * @return size of this record
   *
   * @see Action#getSize()
   */
  public int getSize() {
    int size = 3;
    for (int i = 0; i < values.size(); i++) {
      size += ((StackValue) values.get(i)).getSize();
    }
    return size;
  }

  /**
   * Returns a list of values this action is supposed to push to the stack. Use
   * this list in a read-only manner.
   *
   * @return a list of Push.StackValue instances
   */
  public List getValues() {
    return values;
  }

  /**
   * Adds a value to be pushed to the stack.
   *
   * @param value a <code>StackValue</code> instance
   */
  public void addValue(StackValue value) {
    values.add(value);
  }

  /**
   * Returns a short description of the action.
   *
   * @return <code>"Push"</code>
   */
  public String toString() {
    return "Push";
  }

  protected void writeData(
    OutputBitStream dataStream, OutputBitStream mainStream)
    throws IOException {
    for (int i = 0; i < values.size(); i++) {
      ((StackValue) values.get(i)).write(dataStream);
    }
  }

  /**
   * This class contains a value which can be pushed to the stack.  The default
   * value is <code>undefined</code>, the type is <code>TYPE_UNDEFINED</code>.
   * Use setters to change.
   */
  public static class StackValue implements Serializable {
    /** Indicates that the value to be pushed is a string. */
    public static final short TYPE_STRING      = 0;
    /** Indicates that the value to be pushed is a floating point number. */
    public static final short TYPE_FLOAT       = 1;
    /** Indicates that the value to be pushed is <code>null</code>. */
    public static final short TYPE_NULL        = 2;
    /** Indicates that the value to be pushed is <code>undefined</code>. */
    public static final short TYPE_UNDEFINED   = 3;
    /** Indicates that the value to be pushed is a register number. */
    public static final short TYPE_REGISTER    = 4;
    /** Indicates that the value to be pushed is a boolean. */
    public static final short TYPE_BOOLEAN     = 5;
    /**
     * Indicates that the value to be pushed is double-precision floating point
     * number.
     */
    public static final short TYPE_DOUBLE      = 6;
    /** Indicates that the value to be pushed is an integer. */
    public static final short TYPE_INTEGER     = 7;
    /** Indicates that the value to be pushed is an 8-bit constant pool index. */
    public static final short TYPE_CONSTANT_8  = 8;
    /** Indicates that the value to be pushed is a 16-bit constant pool index. */
    public static final short TYPE_CONSTANT_16 = 9;
    private short type                         = TYPE_UNDEFINED;
    private String string;
    private float floatValue;
    private short registerNumber;
    private boolean booleanValue;
    private double doubleValue;
    private long integerValue;
    private short constant8;
    private int constant16;

    /**
     * Creates a new StackValue instance. Initial type is
     * <code>TYPE_UNDEFINED</code>.
     */
    public StackValue() {
      // nothing to do
    }

    /*
     * Reads a PushEntry instance from a bit stream.
     */
    StackValue(InputBitStream stream) throws IOException {
      type = stream.readUI8();
      switch (type) {
        case TYPE_STRING:
          string = stream.readString();
          break;
        case TYPE_FLOAT:
          floatValue = stream.readFloat();
          break;
        case TYPE_REGISTER:
          registerNumber = stream.readUI8();
          break;
        case TYPE_BOOLEAN:
          booleanValue = (stream.readUI8() != 0);
          break;
        case TYPE_DOUBLE:
          doubleValue = stream.readDouble();
          break;
        case TYPE_INTEGER:
          integerValue = stream.readUI32();
          break;
        case TYPE_CONSTANT_8:
          constant8 = stream.readUI8();
          break;
        case TYPE_CONSTANT_16:
          constant16 = stream.readUI16();
          break;
      }
    }

    /**
     * Sets the push value to a boolean, and the type to TYPE_BOOLEAN.
     *
     * @param value a boolean value
     */
    public void setBoolean(boolean value) {
      this.booleanValue   = value;
      type                = TYPE_BOOLEAN;
    }

    /**
     * Returns the boolean the push value is set to. If the value type is not
     * TYPE_BOOLEAN, an IllegalStateException is thrown.
     *
     * @return push value as boolean
     */
    public boolean getBoolean() {
      return booleanValue;
    }

    /**
     * Sets the push value to a 16-bit constant pool index, and the type to
     * TYPE_BOOLEAN. Use 16-bit indexes when the constant pool contains more
     * than 256 constants.
     *
     * @param value an 8-bit constant pool index
     */
    public void setConstant16(int value) {
      this.constant16   = value;
      type              = TYPE_CONSTANT_16;
    }

    /**
     * Returns the 16-bit constant pool index the push value is set to. If the
     * value type is not TYPE_CONSTANT_16, an IllegalStateException is thrown.
     *
     * @return push value as 16-bit constant pool index
     *
     * @throws IllegalStateException if type is not TYPE_CONSTANT_16
     */
    public int getConstant16() {
      if (type != TYPE_CONSTANT_16) {
        throw new IllegalStateException("Value type is not TYPE_CONSTANT_16!");
      }
      return constant16;
    }

    /**
     * Sets the push value to an 8-bit constant pool index, and the type to
     * TYPE_BOOLEAN. Use 8-bit indexes when the constant pool contains less
     * than 256 constants.
     *
     * @param value an 8-bit constant pool index
     */
    public void setConstant8(short value) {
      this.constant8   = value;
      type             = TYPE_CONSTANT_8;
    }

    /**
     * Returns the 8-bit constant pool index the push value is set to. If the
     * value type is not TYPE_CONSTANT_8, an IllegalStateException is thrown.
     *
     * @return push value as 8-bit constant pool index
     *
     * @throws IllegalStateException if type is not TYPE_CONSTANT_8
     */
    public short getConstant8() {
      if (type != TYPE_CONSTANT_8) {
        throw new IllegalStateException("Value type is not TYPE_CONSTANT_8!");
      }
      return constant8;
    }

    /**
     * Sets the push value to a double-precision number, and the type to
     * TYPE_DOUBLE.
     *
     * @param value a double value
     */
    public void setDouble(double value) {
      this.doubleValue   = value;
      type               = TYPE_DOUBLE;
    }

    /**
     * Returns the double the push value is set to. If the value type is not
     * TYPE_DOUBLE, an IllegalStateException is thrown.
     *
     * @return push value as double
     *
     * @throws IllegalStateException if type is not TYPE_DOUBLE
     */
    public double getDouble() {
      if (type != TYPE_DOUBLE) {
        throw new IllegalStateException("Value type is not TYPE_DOUBLE!");
      }
      return doubleValue;
    }

    /**
     * Sets the push value to a (single-precision) float, and the type to
     * TYPE_FLOAT.
     *
     * @param value a float value
     */
    public void setFloat(float value) {
      this.floatValue   = value;
      type              = TYPE_FLOAT;
    }

    /**
     * Returns the float the push value is set to. If the value type is not
     * TYPE_FLOAT, an IllegalStateException is thrown.
     *
     * @return push value as float
     *
     * @throws IllegalStateException if type is not TYPE_FLOAT
     */
    public float getFloat() {
      if (type != TYPE_FLOAT) {
        throw new IllegalStateException("Value type is not TYPE_FLOAT!");
      }
      return floatValue;
    }

    /**
     * Sets the push value to an integer, and the type to TYPE_INTEGER.
     *
     * @param value an integer value (of type <code>long</code>)
     */
    public void setInteger(long value) {
      this.integerValue   = value;
      type                = TYPE_INTEGER;
    }

    /**
     * Returns the integer the push value is set to. If the value type is not
     * TYPE_INTEGER, an IllegalStateException is thrown.
     *
     * @return push value as integer
     *
     * @throws IllegalStateException if type is not TYPE_INTEGER
     */
    public long getInteger() {
      if (type != TYPE_INTEGER) {
        throw new IllegalStateException("Value type is not TYPE_INTEGER!");
      }
      return integerValue;
    }

    /**
     * Sets the type to <code>TYPE_NULL</code> (i.e. the push value is
     * <code>null</code>).
     */
    public void setNull() {
      type = TYPE_NULL;
    }

    /**
     * Checks if the push value is <code>null</code>.
     *
     * @return true if <code>null</code>, else false.
     */
    public boolean isNull() {
      return (type == TYPE_NULL);
    }

    /**
     * Sets the push value to a register number, and the type to TYPE_REGISTER.
     *
     * @param value a register number
     */
    public void setRegisterNumber(short value) {
      this.registerNumber   = value;
      type                  = TYPE_REGISTER;
    }

    /**
     * Returns the register number the push value is set to. If the value type
     * is not TYPE_REGISTER, an IllegalStateException is thrown.
     *
     * @return push value as register number
     *
     * @throws IllegalStateException if type is not TYPE_REGISTER
     */
    public short getRegisterNumber() {
      if (type != TYPE_REGISTER) {
        throw new IllegalStateException("Value type is not TYPE_REGISTER!");
      }
      return registerNumber;
    }

    /**
     * Sets the push value to a string, and the type to TYPE_STRING
     *
     * @param value a string value
     */
    public void setString(String value) {
      this.string   = value;
      type          = TYPE_STRING;
    }

    /**
     * Returns the string the push value is set to. If the value type is not
     * TYPE_STRING, an IllegalStateException is thrown
     *
     * @return push value as string
     *
     * @throws IllegalStateException if type is not TYPE_STRING
     */
    public String getString() {
      if (type != TYPE_STRING) {
        throw new IllegalStateException("Value type is not TYPE_STRING!");
      }
      return string;
    }

    /**
     * Returns the type of the push value. The type is one of the constants
     * <code>TYPE_BOOLEAN, TYPE_CONSTANT_8, TYPE_CONSTANT_16, TYPE_DOUBLE,
     * TYPE_FLOAT, TYPE_INTEGER, TYPE_NULL, TYPE_REGISTER, TYPE_STRING,
     * TYPE_UNDEFINED</code>.
     *
     * @return type of push value
     */
    public short getType() {
      return type;
    }

    /**
     * Sets the type to <code>TYPE_UNDEFINED</code> (i.e. the push value is
     * <code>undefined</code>).
     */
    public void setUndefined() {
      type = TYPE_UNDEFINED;
    }

    /**
     * Checks if the push value is <code>undefined</code>.
     *
     * @return true if <code>undefined</code>, else false.
     */
    public boolean isUndefined() {
      return (type == TYPE_UNDEFINED);
    }

    /**
     * Returs a short description of the push value (type and value)
     *
     * @return type and value
     */
    public String toString() {
      String result = "";
      switch (type) {
        case TYPE_STRING:
          result += ("string: '" + string + "'");
          break;
        case TYPE_FLOAT:
          result += ("float: " + floatValue);
          break;
        case TYPE_REGISTER:
          result += ("register: " + registerNumber);
          break;
        case TYPE_BOOLEAN:
          result += ("boolean: " + booleanValue);
          break;
        case TYPE_DOUBLE:
          result += ("double: " + doubleValue);
          break;
        case TYPE_INTEGER:
          result += ("integer: " + integerValue);
          break;
        case TYPE_CONSTANT_8:
          result += ("c8[" + constant8 + "]");
          break;
        case TYPE_CONSTANT_16:
          result += ("c16[" + constant16 + "]");
          break;
        case TYPE_UNDEFINED:
          result += "undefined";
          break;
        case TYPE_NULL:
          result += "null";
          break;
      }
      return result;
    }

    int getSize() {
      int size = 1; // type
      switch (type) {
        case TYPE_STRING:
          try {
            size += (string.getBytes("UTF-8").length + 1);
          } catch (UnsupportedEncodingException e) {
            // UTF-8 should be available. If not, we have a big problem anyway
          }
          break;
        case TYPE_FLOAT:
          size += 4;
          break;
        case TYPE_REGISTER:
          size++;
          break;
        case TYPE_BOOLEAN:
          size++;
          break;
        case TYPE_DOUBLE:
          size += 8;
          break;
        case TYPE_INTEGER:
          size += 4;
          break;
        case TYPE_CONSTANT_8:
          size++;
          break;
        case TYPE_CONSTANT_16:
          size += 2;
          break;
      }
      return size;
    }

    void write(OutputBitStream outStream) throws IOException {
      outStream.writeUI8(type);
      switch (type) {
        case TYPE_STRING:
          outStream.writeString(string);
          break;
        case TYPE_FLOAT:
          outStream.writeFloat(floatValue);
          break;
        case TYPE_REGISTER:
          outStream.writeUI8(registerNumber);
          break;
        case TYPE_BOOLEAN:
          outStream.writeUI8((short) (booleanValue ? 1 : 0));
          break;
        case TYPE_DOUBLE:
          outStream.writeDouble(doubleValue);
          break;
        case TYPE_INTEGER:
          outStream.writeUI32(integerValue);
          break;
        case TYPE_CONSTANT_8:
          outStream.writeUI8(constant8);
          break;
        case TYPE_CONSTANT_16:
          outStream.writeUI16(constant16);
          break;
      }
    }
  }
}

/*
 * JSwiff is an open source Java API for Macromedia Flash file generation
 * and manipulation
 *
 * Copyright (C) 2004-2006 Ralf Terdic (contact@jswiff.com)
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

package com.jswiff.io;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;


/**
 * Implements a bit stream used for writing SWF files.
 */
public final class OutputBitStream {
  private OutputStream stream;
  private ByteArrayOutputStream memoryStream;
  private int bitBuffer;
  private int bitCursor; // 0-7, when 7 is reached, bitBuffer is written
  private boolean compressed     = false;
  private long offset;
  private boolean isMemoryStream;
  private boolean ansi;
  private boolean shiftJIS;

  /**
   * Creates a new OutputBitStream instance.
   *
   * @param stream the internal stream used for output data.
   */
  public OutputBitStream(OutputStream stream) {
    this.stream = stream;
  }

  /**
   * Creates a new memory OutputBitStream instance (with a
   * ByteArrayOutputStream as internal stream, its data can be retrieved with
   * the getData() method).
   */
  public OutputBitStream() {
    memoryStream     = new ByteArrayOutputStream();
    stream           = memoryStream;
    isMemoryStream   = true;
  }

  /**
   * Specifies whether ANSI encoding is to be used when encoding strings.
   *
   * @param ansi <code>true</code> for ANSI encoding
   */
  public void setANSI(boolean ansi) {
    this.ansi = ansi;
  }

  /**
   * Checks whether ANSI encoding is to be used when encoding strings.
   *
   * @return <code>true</code> for ANSI encoding
   */
  public boolean isANSI() {
    return ansi;
  }

  /**
   * Returns the stream data.
   *
   * @return byte array containing the output data
   *
   * @throws IllegalStateException if called on a non-memory stream
   */
  public byte[] getData() {
    if (!isMemoryStream) {
      throw new IllegalStateException(
        "Use this method only with memory streams!");
    }
    try {
      stream.close();
    } catch (IOException e) {
      // nothing to do
    }
    return memoryStream.toByteArray();
  }

  /**
   * Returns the number of bits needed for the representation of a given fixed
   * point value. Returns values between 2 and 64 because fixed point values
   * are represented as long numbers.
   *
   * @param value a double value (representing a fixed point number)
   *
   * @return number of bits needed for the representation of the value
   */
  public static int getFPBitsLength(double value) {
    if (value == 0.0) {
      return 1;
    }
    long fpBits = (long) (value * 65536.0);
    return getSignedBitsLength(fpBits);
  }

  /**
   * Returns the stream offset, i.e. the number of bytes (fully) written.
   *
   * @return the stream offset
   */
  public long getOffset() {
    return offset;
  }

  /**
   * Returns the number of bits needed for the representation of a given signed
   * value. Signed values are represented using at least 2 bits, so the method
   * returns at least a value of 2. The maximum returned value is 64.
   *
   * @param value a long value (representing a signed integer)
   *
   * @return number of bits needed for the representation of the value
   */
  public static int getSignedBitsLength(long value) {
    int nBits; // result is at least 2!
    if (value == 0) {
      nBits = 0; // zero is represented as two cleared bits
    } else {
      // floor(ld(abs(value)))+1 bits for the absolute value, +1 bit for the sign
      nBits = (int) (Math.floor(Math.log(Math.abs(value)) / Math.log(2)) + 2);
    }
    return nBits;
  }

  /**
   * Returns the number of bits needed for the representation of a given
   * unsigned value. For negative values, the method returns 0. For 0 or 1 as
   * value, a length of 1 bit is returned. The maximum returned value is 64.
   *
   * @param value a long value (representing an unsigned integer)
   *
   * @return number of bits needed for the representation of the value
   */
  public static int getUnsignedBitsLength(long value) {
    if (value < 1) {
      return 0;
    }
    return (int) (Math.floor(Math.log(value) / Math.log(2)) + 1);
  }

  /**
   * Specifies whether Shift-JIS encoding is to be used when encoding strings.
   *
   * @param shiftJIS <code>true</code> for Shift-JIS encoding
   */
  public void setShiftJIS(boolean shiftJIS) {
    this.shiftJIS = shiftJIS;
  }

  /**
   * Checks whether Shift-JIS encoding is to be used when encoding strings.
   *
   * @return <code>true</code> for Shift-JIS encoding
   */
  public boolean isShiftJIS() {
    return shiftJIS;
  }

  /**
   * When this method is called, the content of the bit buffer is written and
   * the current byte is filled up with zero bits.
   *
   * @throws IOException if an I/O error has occurred
   */
  public void align() throws IOException {
    if (bitCursor > 0) {
      stream.write(bitBuffer);
      offset++;
      bitCursor   = 0;
      bitBuffer   = 0;
    }
  }

  /**
   * Closes the internal stream. After this, data cannot be written anymore.
   *
   * @throws IOException if an I/O error has occurred
   */
  public void close() throws IOException {
    align();
    stream.close();
  }

  /**
   * Enables stream compression (ZLIB, maximum compression level).
   */
  public void enableCompression() {
    if (!compressed) {
      stream       = new BufferedOutputStream(
          new DeflaterOutputStream(
            stream, new Deflater(Deflater.BEST_COMPRESSION)));
      compressed   = true;
    }
  }

  /**
   * Forces buffered data to be written out.
   *
   * @throws IOException if an I/O error has occurred
   */
  public void flush() throws IOException {
    stream.flush();
  }

  /**
   * Writes a boolean byte as one bit. For <code>true</code>, a 1 is written,
   * for <code>false</code>, a 0.
   *
   * @param value a boolean value
   *
   * @throws IOException if an I/O error has occurred
   */
  public void writeBooleanBit(boolean value) throws IOException {
    writeUnsignedBits(value ? 1 : 0, 1);
  }

  /**
   * Writes a byte buffer.
   *
   * @param buffer buffer as byte array
   *
   * @throws IOException if an I/O error has occurred
   */
  public void writeBytes(byte[] buffer) throws IOException {
    align();
    if (buffer == null) {
      return;
    }
    stream.write(buffer);
    offset += buffer.length;
  }

  /**
   * Writes a double value.
   *
   * @param value a double
   *
   * @throws IOException if an I/O error has occurred
   */
  public void writeDouble(double value) throws IOException {
    long longBits = Double.doubleToLongBits(value);
    byte[] buffer = new byte[8];
    buffer[0]     = (byte) (longBits >> 32);
    buffer[1]     = (byte) (longBits >> 40);
    buffer[2]     = (byte) (longBits >> 48);
    buffer[3]     = (byte) (longBits >> 56);
    buffer[4]     = (byte) longBits;
    buffer[5]     = (byte) (longBits >> 8);
    buffer[6]     = (byte) (longBits >> 16);
    buffer[7]     = (byte) (longBits >> 24);
    writeBytes(buffer);
  }

  /**
   * Writes a 16 bit (8.8) fixed point number.
   *
   * @param value fixed point number as double value
   *
   * @throws IOException if an I/O error occured
   */
  public void writeFP16(double value) throws IOException {
    writeSI16((short) (value * 256.0));
  }

  /**
   * Writes a 32 bit (16.16) fixed point number.
   *
   * @param value fixed point number as double value
   *
   * @throws IOException if an I/O error occured
   */
  public void writeFP32(double value) throws IOException {
    writeSI32((int) (value * 65536.0));
  }

  /**
   * Writes a fixed point value, using a given number of bits (e.g. computed by
   * <code>getFPBitsLength()</code>).
   *
   * @param value a fixed point number as double value
   * @param nBits number of bits to be written
   *
   * @throws IOException if an I/O error has occurred
   */
  public void writeFPBits(double value, int nBits) throws IOException {
    long fpBits = (long) (value * 65536.0);
    writeSignedBits(fpBits, nBits);
  }

  /**
   * Writes a float value.
   *
   * @param value a float
   *
   * @throws IOException if an I/O error has occurred
   */
  public void writeFloat(float value) throws IOException {
    writeSI32(Float.floatToIntBits(value));
  }

  /**
   * Writes a float value as a 16 bit floating point number (half precision, or
   * s10e5, i.e. 1 sign bit, 5 exponent bits and 10 mantissa bits).
   *
   * @param value float value
   *
   * @throws IOException if an I/O error occured
   */
  public void writeFloat16(float value) throws IOException {
    int bits32     = Float.floatToIntBits(value);
    int sign       = Math.abs((bits32 & 0x80000000) >> 31);
    int exponent32 = (bits32 & 0x7f800000) >> 23;
    int mantissa32 = bits32 & 0x7fffff;
    int exponent16 = 0;
    if (exponent32 != 0) {
      if (exponent32 == 0xff) {
        exponent16 = 0x1f;
      } else {
        exponent16 = exponent32 - 127 + 15;
      }
    }
    int mantissa16 = 0;
    if (exponent16 < 0) {
      exponent16 = 0; // +-0
    } else if (exponent16 > 0x1f) {
      exponent16 = 0x1f; // +- Infinity
    } else {
      mantissa16 = mantissa32 >> 13;
    }
    int bits16 = sign << 15;
    bits16 |= exponent16 << 10;
    bits16 |= mantissa16;
    writeUI16(bits16);
  }

  /**
   * Writes a signed word value.
   *
   * @param value signed word as short value
   *
   * @throws IOException if an I/O error has occurred
   */
  public void writeSI16(short value) throws IOException {
    align();
    stream.write(value & 0xFF);
    stream.write(value >> 8);
    offset += 2;
  }

  /**
   * Writes a signed double word.
   *
   * @param value signed double word as int value
   *
   * @throws IOException if an I/O error has occurred
   */
  public void writeSI32(int value) throws IOException {
    align();
    stream.write(value & 0xFF);
    stream.write(value >> 8);
    stream.write(value >> 16);
    stream.write(value >> 24);
    offset += 4;
  }

  /**
   * Writes a signed byte.
   *
   * @param value signed byte as byte value
   *
   * @throws IOException if an I/O error has occurred
   */
  public void writeSI8(byte value) throws IOException {
    align();
    stream.write(value);
    offset++;
  }

  /**
   * Writes a signed integer, using a given number of bits (e.g. computed by
   * <code>getSignedBitsLength()</code>).
   *
   * @param value a signed integer as long value
   * @param nBits number of bits to be written
   *
   * @throws IOException if an I/O error has occurred
   */
  public void writeSignedBits(long value, int nBits) throws IOException {
    int bitsNeeded = getSignedBitsLength(value);
    if (nBits < bitsNeeded) {
      throw new IOException(
        "At least " + bitsNeeded + " bits needed for representation of " +
        value);
    }
    writeInteger(value, nBits);
  }

  /**
   * Writes an UTF-8 encoded, null-terminated string.
   *
   * @param string a string
   *
   * @throws IOException if an I/O error has occurred
   */
  public void writeString(String string) throws IOException {
    String encoding;
    if (shiftJIS) {
      encoding = "SJIS";
    } else if (ansi) {
      encoding = "cp1252";
    } else {
      encoding = "UTF-8";
    }
    writeBytes(string.getBytes(encoding));
    stream.write(0);
    offset++;
  }

  /**
   * Writes an unsigned word.
   *
   * @param value an unsigned word as int value
   *
   * @throws IOException if an I/O error has occurred
   */
  public void writeUI16(int value) throws IOException {
    align();
    stream.write(value & 0xFF);
    stream.write(value >> 8);
    offset += 2;
  }

  /**
   * Writes an unsigned double word.
   *
   * @param value unsigned double word as long value
   *
   * @throws IOException if an I/O error has occurred
   */
  public void writeUI32(long value) throws IOException {
    align();
    stream.write((int) (value & 0xFF));
    stream.write((int) (value >> 8));
    stream.write((int) (value >> 16));
    stream.write((int) (value >> 24));
    offset += 4;
  }

  /**
   * Writes an unsigned byte.
   *
   * @param value unsigned byte as byte value
   *
   * @throws IOException if an I/O error has occurred
   */
  public void writeUI8(short value) throws IOException {
    align();
    stream.write(value);
    offset++;
  }

  /**
   * Writes an unsigned integer, using a given number of bits (e.g. computed by
   * <code>getUnsignedBitsLength()</code>).
   *
   * @param value an unsigned integer as long value
   * @param nBits number of bits to be written
   *
   * @throws IOException if an I/O error has occurred
   */
  public void writeUnsignedBits(long value, int nBits)
    throws IOException {
    int bitsNeeded = getUnsignedBitsLength(value);
    if (nBits < bitsNeeded) {
      throw new IOException(
        "At least " + bitsNeeded + " bits needed for representation of " +
        value + ". Used bits: " + nBits);
    }
    writeInteger(value, nBits);
  }

  private void writeInteger(long value, int nBits) throws IOException {
    int bitsLeft = nBits;
    while (bitsLeft > 0) {
      bitCursor++;
      // bit set?
      if (((1L << (bitsLeft - 1)) & value) != 0) {
        bitBuffer |= (1 << (8 - bitCursor));
      }
      if (bitCursor == 8) {
        // write bit buffer
        stream.write(bitBuffer);
        offset++;
        bitCursor   = 0;
        bitBuffer   = 0;
      }
      bitsLeft--;
    }
  }
}

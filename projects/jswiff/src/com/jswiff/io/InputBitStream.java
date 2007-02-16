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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.zip.InflaterInputStream;


/**
 * Implements a bit stream used for reading SWF files.
 */
public final class InputBitStream {
  private InputStream stream;
  private int bitBuffer;
  private int bitCursor      = 8; // 8 if bitBuffer empty, else 0-7
  private boolean compressed = false;
  private long offset;
  private boolean ansi;
  private boolean shiftJIS;

  /**
   * Creates a new bit stream instance from a given input stream.
   *
   * @param stream the internal input stream the data is read from.
   */
  public InputBitStream(InputStream stream) {
    this.stream   = stream;
    offset        = 0;
  }

  /**
   * Creates a new bit stream instance. Data is read from a given byte buffer.
   *
   * @param buffer data buffer the bit stream reads from
   */
  public InputBitStream(byte[] buffer) {
    this(new ByteArrayInputStream(buffer));
  }

  /**
   * Specifies whether ANSI encoding is to be used when decoding strings.
   *
   * @param ansi <code>true</code> for ANSI encoding
   */
  public void setANSI(boolean ansi) {
    this.ansi = ansi;
  }

  /**
   * Checks whether ANSI encoding is to be used when decoding strings.
   *
   * @return <code>true</code> for ANSI encoding
   */
  public boolean isANSI() {
    return ansi;
  }

  /**
   * Returns the stream offset, i.e. the number of bytes (fully) read.
   *
   * @return the stream offset
   */
  public long getOffset() {
    return offset;
  }

  /**
   * Specifies whether Shift-JIS encoding is to be used when decoding strings.
   *
   * @param shiftJIS <code>true</code> for Shift-JIS encoding
   */
  public void setShiftJIS(boolean shiftJIS) {
    this.shiftJIS = shiftJIS;
  }

  /**
   * Checks whether Shift-JIS encoding is to be used when decoding strings.
   *
   * @return <code>true</code> for Shift-JIS encoding
   */
  public boolean isShiftJIS() {
    return shiftJIS;
  }

  /**
   * Byte align, i.e. after invocation, data is read beginning at the following
   * byte boundary. Has no effect if cursor is already at a byte boundary.
   */
  public void align() {
    bitCursor = 8;
  }

  /**
   * Returns the number of bytes that can be read from the internal input
   * stream without blocking. This value is obtained by invoking available()
   * on the internal stream. Don't use this method on compressed streams.
   *
   * @return the number of bytes that can be read without blocking
   *
   * @throws IOException if an I/O error has occurred
   * @throws IllegalStateException if stream is compressed
   */
  public int available() throws IOException {
    if (compressed) {
      throw new IllegalStateException(
        "Don't use available() on compressed streams!");
    }
    return stream.available();
  }

  /**
   * Closes the internal stream. After this, data cannot be read anymore.
   *
   * @throws IOException if an I/O error has occurred
   */
  public void close() throws IOException {
    stream.close();
  }

  /**
   * Enables stream compression (ZLIB).
   */
  public void enableCompression() {
    if (!compressed) {
      stream = new BufferedInputStream(new InflaterInputStream(stream));
    }
    compressed = true;
  }

  /**
   * Move within the stream, relative to the current position.
   *
   * @param delta positive to move forward, negative for backward move
   *
   * @throws IOException if an I/O error has occured
   */
  public void move(long delta) throws IOException {
    offset = offset + delta;
    stream.reset();
    stream.skip(offset);
  }

  /**
   * Reads a bit and interprets it as boolean.
   *
   * @return true for 1, false for 0
   *
   * @throws IOException if an I/O error has occurred
   */
  public boolean readBooleanBit() throws IOException {
    return ((readUnsignedBits(1)) == 1);
  }

  /**
   * Reads a specific number of bytes.
   *
   * @param length number of bytes to be read
   *
   * @return the read data, as byte array
   *
   * @throws IOException if an I/O error has occurred
   */
  public byte[] readBytes(int length) throws IOException {
    byte[] result;
    if (length > 0) {
      result = new byte[length];
      int totalRead = 0;
      while (totalRead < length) {
        int read = stream.read(result, totalRead, length - totalRead);
        if (read < 0) {
          endReached();
          return null;
        }
        totalRead += read;
      }
    } else {
      return new byte[0];
    }
    offset += length;
    align();
    return result;
  }

  /**
   * Reads a double value.
   *
   * @return double value
   *
   * @throws IOException if an I/O error has occurred
   */
  public double readDouble() throws IOException {
    byte[] buffer = readBytes(8);
    long longBits = (((long) buffer[3] << 56) +
      ((long) (buffer[2] & 255) << 48) + ((long) (buffer[1] & 255) << 40) +
      ((long) (buffer[0] & 255) << 32) + ((long) (buffer[7] & 255) << 24) +
      ((buffer[6] & 255) << 16) + ((buffer[5] & 255) << 8) +
      ((buffer[4] & 255) << 0));
    return Double.longBitsToDouble(longBits);
  }

  /**
   * Reads a 16 bit (8.8) fixed point number.
   *
   * @return number as double value
   *
   * @throws IOException if an I/O error has occured
   */
  public double readFP16() throws IOException {
    short value = readSI16();
    return value / 256.0;
  }

  /**
   * Reads a 32 bit (16.16) fixed point number.
   *
   * @return number as double value
   *
   * @throws IOException if an I/O error has occured
   */
  public double readFP32() throws IOException {
    int value = readSI32();
    return value / 65536.0;
  }

  /**
   * Reads a specific number of bits and interprets them as a fixed point
   * (x.16) value.
   *
   * @param nBits number of bits to be read
   *
   * @return fixed point number as a double value
   *
   * @throws IOException if an I/O error has occurred
   */
  public double readFPBits(int nBits) throws IOException {
    long longNumber = readSignedBits(nBits);
    return longNumber / 65536.0;
  }

  /**
   * Reads a floating point value.
   *
   * @return floating point number as a float value
   *
   * @throws IOException if an I/O error has occurred
   */
  public float readFloat() throws IOException {
    return Float.intBitsToFloat(readSI32());
  }

  /**
   * Reads a 16 bit floating point number (half precision, or s10e5, i.e. 1
   * sign bit, 5 exponent bits and 10 mantissa bits).
   *
   * @return float value
   *
   * @throws IOException if an I/O error occured
   */
  public float readFloat16() throws IOException {
    int bits16     = readUI16();
    int sign       = (bits16 & 0x8000) >> 15;
    int exponent16 = (bits16 & 0x7c00) >> 10;
    int mantissa16 = bits16 & 0x3ff;
    int exponent32 = 0;
    if (exponent16 != 0) {
      if (exponent16 == 0x1f) {
        exponent32 = 0xff;
      } else {
        exponent32 = exponent16 - 15 + 127;
      }
    }
    int mantissa32 = mantissa16 << 13;
    int bits32     = sign << 31;
    bits32 |= exponent32 << 23;
    bits32 |= mantissa32;
    return Float.intBitsToFloat(bits32);
  }

  /**
   * Reads a signed word value
   *
   * @return signed word as a short value
   *
   * @throws IOException if an I/O error has occurred
   */
  public short readSI16() throws IOException {
    return (short) readUI16();
  }

  /**
   * Reads a signed double word
   *
   * @return signed double word as an int value
   *
   * @throws IOException if an I/O error has occurred
   */
  public int readSI32() throws IOException {
    return (int) readUI32();
  }

  /**
   * Reads a signed byte value
   *
   * @return signed byte as a byte value
   *
   * @throws IOException if an I/O error has occurred
   */
  public byte readSI8() throws IOException {
    return (byte) readUI8();
  }

  /**
   * Reads a specific number of bits and interprets them as a signed integer
   *
   * @param nBits number of bits to be read
   *
   * @return signed integer as a long value
   *
   * @throws IOException if an I/O error has occurred
   */
  public long readSignedBits(int nBits) throws IOException {
    long result = readUnsignedBits(nBits);
    if ((result & (1L << (nBits - 1))) != 0) {
      result |= (-1L << nBits);
    }
    return result;
  }

  /**
   * Reads an UTF-8 encoded, null-terminated string
   *
   * @return a string
   *
   * @throws IOException if an I/O error has occurred
   */
  public String readString() throws IOException {
    // read the string byte for byte until we get a null-byte
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    fillBitBuffer();
    while (bitBuffer != 0) {
      baos.write(bitBuffer);
      fillBitBuffer();
    }
    byte[] buffer   = baos.toByteArray();
    String encoding;
    if (shiftJIS) {
      encoding = "SJIS";
    } else if (ansi) {
      encoding = "cp1252";
    } else {
      encoding = "UTF-8";
    }
    return new String(buffer, encoding);
  }

  /**
   * Reads an unsigned word value
   *
   * @return unsigned word as an int value
   *
   * @throws IOException if an I/O error has occurred
   */
  public int readUI16() throws IOException {
    fillBitBuffer();
    int result = bitBuffer;
    fillBitBuffer();
    result |= (bitBuffer << 8);
    align();
    return result;
  }

  /**
   * Reads an unsigned double word value
   *
   * @return unsigned double word as a long value
   *
   * @throws IOException if an I/O error has occurred
   */
  public long readUI32() throws IOException {
    fillBitBuffer();
    long result = bitBuffer;
    fillBitBuffer();
    result |= (bitBuffer << 8);
    fillBitBuffer();
    result |= (bitBuffer << 16);
    fillBitBuffer();
    result |= (bitBuffer << 24);
    align();
    return result;
  }

  /**
   * Reads an unsigned byte value
   *
   * @return unsigned byte as a byte value
   *
   * @throws IOException if an I/O error has occurred
   */
  public short readUI8() throws IOException {
    fillBitBuffer();
    short result = (short) bitBuffer;
    align();
    return result;
  }

  /**
   * Read an unsigned integer from the given number of bits.
   *
   * @param nBits number of bits to be read
   *
   * @return unsigned integer as a long value
   *
   * @throws IOException if an I/O error has occurred
   */
  public long readUnsignedBits(int nBits) throws IOException {
    if (nBits == 0) {
      return 0;
    }
    int bitsLeft = nBits;
    long result  = 0;
    while (bitsLeft > 0) {
      if (bitCursor == 8) {
        // buffer is empty
        fillBitBuffer();
      }

      // check if bit is set
      if ((bitBuffer & (1 << (7 - bitCursor))) != 0) {
        // set corresponding result bit
        result |= (1L << (bitsLeft - 1));
      }
      bitCursor++;
      bitsLeft--;
    }
    return result;
  }

  private void endReached() throws IOException {
    throw new IOException("Input data stream ended unexpectedly!");
  }

  private void fillBitBuffer() throws IOException {
    bitBuffer = stream.read();
    offset++;
    if (bitBuffer < 0) {
      endReached();
    }
    bitCursor = 0;
  }
}

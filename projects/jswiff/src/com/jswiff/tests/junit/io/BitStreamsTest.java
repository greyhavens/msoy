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

package com.jswiff.tests.junit.io;

import com.jswiff.io.InputBitStream;
import com.jswiff.io.OutputBitStream;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


/**
 * Test class for <code>InputBitStream</code> and <code>OutputBitStream</code>.
 */
public class BitStreamsTest extends TestCase {
  private OutputBitStream outStream;
  ByteArrayOutputStream baos;
  private InputBitStream inStream;

  /**
   * Constructor.
   *
   * @param name
   */
  public BitStreamsTest(String name) {
    super(name);
  }

  /**
   * Tests getOffset()
   *
   * @throws IOException if an I/O error occured
   */
  public void testGetOffset() throws IOException {
    outStream.writeSI8((byte) -3);
    outStream.writeSI16((short) 4234);
    outStream.writeSI32(-5234232);
    assertEquals(7, outStream.getOffset());
    outStream.writeUI8((short) 42);
    outStream.writeUI16(5242);
    outStream.writeUI32(552342);
    assertEquals(14, outStream.getOffset());
    outStream.writeUnsignedBits(4, OutputBitStream.getUnsignedBitsLength(4));
    outStream.writeSignedBits(8, OutputBitStream.getSignedBitsLength(8));
    outStream.writeBooleanBit(false);
    assertEquals(15, outStream.getOffset());
    outStream.align();
    assertEquals(16, outStream.getOffset());
    byte[] bytes = { 1, 2, 3 };
    outStream.writeBytes(bytes);
    assertEquals(19, outStream.getOffset());
    outStream.writeBooleanBit(true);
    assertEquals(19, outStream.getOffset());
    outStream.close();
    assertEquals(20, outStream.getOffset());
  }

  /**
   * Tests readBooleanBit() and writeBooleanBit()
   *
   * @throws IOException if an I/O error occured
   */
  public void testReadWriteBooleanBit() throws IOException {
    outStream.writeBooleanBit(true);
    outStream.writeBooleanBit(false);
    initInStream();
    assertTrue(inStream.readBooleanBit());
    assertFalse(inStream.readBooleanBit());
  }

  /**
   * Tests readBytes() and writeBytes()
   *
   * @throws IOException if an I/O error occured
   */
  public void testReadWriteBytes() throws IOException {
    byte[] outBuffer = { 1, -24, 44, 32, -11 };
    outStream.writeBytes(outBuffer);
    initInStream();
    byte[] inBuffer = inStream.readBytes(outBuffer.length);
    for (int i = 0; i < inBuffer.length; i++) {
      assertEquals(outBuffer[i], inBuffer[i]);
    }
  }

  /**
   * Tests readDouble() and writeDouble()
   *
   * @throws IOException if an I/O error occured
   */
  public void testReadWriteDouble() throws IOException {
    double doubleValue = 13498234934.23498238534;
    outStream.writeDouble(doubleValue);
    initInStream();
    assertEquals(doubleValue, inStream.readDouble(), 0.0);
  }

  /**
   * Tests readFPBits() and writeFPBits()
   *
   * @throws IOException if an I/O error occured
   */
  public void testReadWriteFPBits() throws IOException {
    //double value = -71532367676.9798;
    double value = 0.1;
    int nBits    = OutputBitStream.getFPBitsLength(value);
    outStream.writeFPBits(value, nBits);
    initInStream();
    // max delta is 1/65536
    assertEquals(value, inStream.readFPBits(nBits), 1.0 / 65536);
  }

  /**
   * Tests readFloat() and writeFloat()
   *
   * @throws IOException if an I/O error occured
   */
  public void testReadWriteFloat() throws IOException {
    float floatValue = -423.43F;
    outStream.writeFloat(floatValue);
    initInStream();
    assertEquals(floatValue, inStream.readFloat(), 0);
  }

  /**
   * Tests readSI16() and writeSI16()
   *
   * @throws IOException if an I/O error occured
   */
  public void testReadWriteSI16() throws IOException {
    short si16Value = -7;
    outStream.writeSI16(si16Value);
    initInStream();
    assertEquals(si16Value, inStream.readSI16());
  }

  /**
   * Tests readSI32() and writeSI32();
   *
   * @throws IOException if an I/O error occured
   */
  public void testReadWriteSI32() throws IOException {
    int si32Value = -423424234;
    outStream.writeSI32(si32Value);
    initInStream();
    assertEquals(si32Value, inStream.readSI32());
  }

  /**
   * Tests readSI8() and writeSI8()
   *
   * @throws IOException if an I/O error occured
   */
  public void testReadWriteSI8() throws IOException {
    byte si8Value = -112;
    outStream.writeSI8(si8Value);
    initInStream();
    assertEquals(si8Value, inStream.readSI8());
  }

  /**
   * Tests readSignedBits() and writeSignedBits()
   *
   * @throws IOException if an I/O error occured
   */
  public void testReadWriteSignedBits() throws IOException {
    int nBitsNeg = OutputBitStream.getSignedBitsLength(-4234);
    outStream.writeSignedBits(-4234, nBitsNeg);
    int nBitsPos = OutputBitStream.getSignedBitsLength(745);
    outStream.writeSignedBits(745, nBitsPos);
    initInStream();
    assertEquals(-4234, inStream.readSignedBits(nBitsNeg));
    assertEquals(745, inStream.readSignedBits(nBitsPos));
  }

  /**
   * Tests readSignedBits() and writeSignedBits()
   *
   * @throws IOException if an I/O error occured
   */
  public void testReadWriteSignedBitsWithLength() throws IOException {
    outStream.writeSignedBits(-15, 6);
    outStream.writeSignedBits(38, 7);
    initInStream();
    assertEquals(-15, inStream.readSignedBits(6));
    assertEquals(38, inStream.readSignedBits(7));
  }

  /**
   * Tests readUI16() and writeUI16()
   *
   * @throws IOException if an I/O error occured
   */
  public void testReadWriteUI16() throws IOException {
    int ui16Value = 4234;
    outStream.writeUI16(ui16Value);
    initInStream();
    assertEquals(ui16Value, inStream.readUI16());
  }

  /**
   * Tests readUI32() and writeUI32()
   *
   * @throws IOException if an I/O error occured
   */
  public void testReadWriteUI32() throws IOException {
    long ui32Value = 423424234;
    outStream.writeUI32(ui32Value);
    initInStream();
    assertEquals(ui32Value, inStream.readUI32());
  }

  /**
   * Tests readUI8() and writeUI8()
   *
   * @throws IOException if an I/O error occured
   */
  public void testReadWriteUI8() throws IOException {
    short ui8Value = 112;
    outStream.writeUI8(ui8Value);
    initInStream();
    assertEquals(ui8Value, inStream.readUI8());
  }

  /**
   * Tests readUnsignedBits() and writeUnsignedBits()
   *
   * @throws IOException if an I/O error occured
   */
  public void testReadWriteUnsignedBits() throws IOException {
    long longValue = 2;
    int nBits      = OutputBitStream.getUnsignedBitsLength(longValue);
    outStream.writeUnsignedBits(longValue, nBits);
    initInStream();
    assertEquals(longValue, inStream.readUnsignedBits(nBits));
  }

  /**
   * Tests readUnsignedBits() and writeUnsignedBits()
   *
   * @throws IOException if an I/O error occured
   */
  public void testReadWriteUnsignedBitsIterated() throws IOException {
    int max     = 100;
    int[] nBits = new int[max];
    for (int i = 1; i < max; i++) {
      nBits[i] = OutputBitStream.getUnsignedBitsLength(i);
      outStream.writeUnsignedBits(i, nBits[i]);
      outStream.writeDouble(1.1);
    }
    initInStream();
    for (int i = 1; i < max; i++) {
      assertEquals(i, inStream.readUnsignedBits(nBits[i]));
      assertEquals(1.1, inStream.readDouble(), 0.0);
    }
  }

  /**
   * Tests readUnsignedBits() and writeUnsignedBits()
   *
   * @throws IOException if an I/O error occured
   */
  public void testReadWriteUnsignedBitsWithLength() throws IOException {
    long longValue = 75;
    outStream.writeUnsignedBits(longValue, 8);
    initInStream();
    assertEquals(longValue, inStream.readUnsignedBits(8));
  }

  /*
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception {
    baos        = new ByteArrayOutputStream();
    outStream   = new OutputBitStream(baos);
    outStream.enableCompression();
  }

  /*
   * @see TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  private void initInStream() throws IOException {
    outStream.close();
    inStream = new InputBitStream(baos.toByteArray());
    inStream.enableCompression();
  }
}

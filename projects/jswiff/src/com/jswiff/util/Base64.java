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

package com.jswiff.util;

import java.io.UnsupportedEncodingException;


/**
 * Class used for Base64 encoding and decoding. Based on Robert Harder's Base64
 * class, available as public domain software.
 */
public class Base64 {
  private final static int MAX_LINE_LENGTH       = 76;
  /* The equals sign (=) as a byte. */
  private final static byte EQUALS_SIGN          = (byte) '=';
  /* The new line character (\n) as a byte. */
  private final static byte NEW_LINE             = (byte) '\n';
  /* Preferred encoding. */
  private final static String PREFERRED_ENCODING = "UTF-8";
  /* The 64 valid Base64 values. */
  private final static byte[] ALPHABET;
  private final static byte[] _NATIVE_ALPHABET   =  /* May be something funny like EBCDIC */{
      (byte) 'A', (byte) 'B', (byte) 'C', (byte) 'D', (byte) 'E', (byte) 'F',
      (byte) 'G', (byte) 'H', (byte) 'I', (byte) 'J', (byte) 'K', (byte) 'L',
      (byte) 'M', (byte) 'N', (byte) 'O', (byte) 'P', (byte) 'Q', (byte) 'R',
      (byte) 'S', (byte) 'T', (byte) 'U', (byte) 'V', (byte) 'W', (byte) 'X',
      (byte) 'Y', (byte) 'Z', (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd',
      (byte) 'e', (byte) 'f', (byte) 'g', (byte) 'h', (byte) 'i', (byte) 'j',
      (byte) 'k', (byte) 'l', (byte) 'm', (byte) 'n', (byte) 'o', (byte) 'p',
      (byte) 'q', (byte) 'r', (byte) 's', (byte) 't', (byte) 'u', (byte) 'v',
      (byte) 'w', (byte) 'x', (byte) 'y', (byte) 'z', (byte) '0', (byte) '1',
      (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7',
      (byte) '8', (byte) '9', (byte) '+', (byte) '/'
    };

  /*
   * Determine which ALPHABET to use.
   */
  static {
    byte[] __bytes;
    try {
      __bytes = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".getBytes(
          PREFERRED_ENCODING);
    } // end try
    catch (java.io.UnsupportedEncodingException use) {
      __bytes = _NATIVE_ALPHABET; // Fall back to native encoding
    } // end catch
    ALPHABET = __bytes;
  } // end static

  /*
   * Translates a Base64 value to either its 6-bit reconstruction value or a
   * negative number indicating some other meaning.
   */
  private final static byte[] DECODABET     = {
      -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal  0 -  8
      -5, -5, // Whitespace: Tab and Linefeed
      -9, -9, // Decimal 11 - 12
      -5, // Whitespace: Carriage Return
      -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 14 - 26
      -9, -9, -9, -9, -9, // Decimal 27 - 31
      -5, // Whitespace: Space
      -9, -9, -9, -9, -9, -9, -9, -9, -9, -9, // Decimal 33 - 42
      62, // Plus sign at decimal 43
      -9, -9, -9, // Decimal 44 - 46
      63, // Slash at decimal 47
      52, 53, 54, 55, 56, 57, 58, 59, 60, 61, // Numbers zero through nine
      -9, -9, -9, // Decimal 58 - 60
      -1, // Equals sign at decimal 61
      -9, -9, -9, // Decimal 62 - 64
      0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, // Letters 'A' through 'N'
      14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, // Letters 'O' through 'Z'
      -9, -9, -9, -9, -9, -9, // Decimal 91 - 96
      26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, // Letters 'a' through 'm'
      39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, // Letters 'n' through 'z'
      -9, -9, -9, -9 // Decimal 123 - 126
    };
  private final static byte WHITE_SPACE_ENC = -5; // Indicates white space in encoding
  private final static byte EQUALS_SIGN_ENC = -1; // Indicates equals sign in encoding

  private Base64() {
  }

  /**
   * Decodes data from Base64 notation into a byte array.
   *
   * @param s the Base64 string to decode
   *
   * @return the decoded data
   */
  public static byte[] decode(String s) {
    byte[] data;
    try {
      data = s.getBytes(PREFERRED_ENCODING);
    } catch (java.io.UnsupportedEncodingException uee) {
      data = s.getBytes();
    }
    data = decode(data);
    return data;
  }

  /**
   * Decodes data from Base64 notation and converts it to a string. UTF-8 is
   * used as encoding.
   *
   * @param s the Base64 string to decode
   *
   * @return the decoded string
   */
  public static String decodeString(String s) {
    byte[] data = decode(s);
    try {
      return new String(data, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // shouldn't happen
      return new String(data);
    }
  }

  /**
   * Decodes data from Base64 notation into an unsigned byte array. As Java
   * doesn't support unsigned bytes as data types, they are stored as short
   * values.
   *
   * @param s the Base64 string to decode
   *
   * @return the decoded data
   */
  public static short[] decodeUnsigned(String s) {
    byte[] bytes = decode(s);
    short[] data = new short[bytes.length];
    for (int i = 0; i < bytes.length; i++) {
      data[i] = (short) (bytes[i] & 0xff);
    }
    return data;
  }

  /**
   * Encodes a byte array into Base64 notation.
   *
   * @param data The data to convert
   *
   * @return Base64 encoded string
   */
  public static String encode(byte[] data) {
    int len        = data.length;
    int off        = 0;
    int len43      = (len * 4) / 3;
    byte[] outBuff = new byte[(len43) + (((len % 3) > 0) ? 4 : 0) +
      (len43 / MAX_LINE_LENGTH)];
    int d          = 0;
    int e          = 0;
    int len2       = len - 2;
    int lineLength = 0;
    for (; d < len2; d += 3, e += 4) {
      encode3to4(data, d + off, 3, outBuff, e);
      lineLength += 4;
      if (lineLength == MAX_LINE_LENGTH) {
        outBuff[e + 4] = NEW_LINE;
        e++;
        lineLength = 0;
      }
    }
    if (d < len) {
      encode3to4(data, d + off, len - d, outBuff, e);
      e += 4;
    }
    try {
      return new String(outBuff, 0, e, PREFERRED_ENCODING);
    } catch (UnsupportedEncodingException uue) {
      return new String(outBuff, 0, e);
    }
  }

  /**
   * Encodes a string to a sequence of bytes using UTF-8 as encoding, then
   * encodes the byte sequence into Base64 notation. This is especially useful
   * for strings containing illegal characters with respect to the XML spec.
   *
   * @param string string to be encoded
   *
   * @return Base64 encoded string
   */
  public static String encodeString(String string) {
    try {
      byte[] data = string.getBytes("UTF-8");
      return encode(data);
    } catch (UnsupportedEncodingException e) {
      // shouldn't happen
      return string;
    }
  }

  /**
   * Encodes an array of unsigned bytes into Base64 notation. As Java doesn't
   * support unsigned bytes, these are stored as short values.
   *
   * @param data array of unsigned bytes as short array
   *
   * @return Base64 encoded string
   */
  public static String encodeUnsigned(short[] data) {
    byte[] bytes = new byte[data.length];
    for (int i = 0; i < data.length; i++) {
      bytes[i] = (byte) data[i];
    }
    return encode(bytes);
  }

  private static byte[] decode(byte[] data) {
    int off         = 0;
    int len         = data.length;
    int len34       = (len * 3) / 4;
    byte[] outBuff  = new byte[len34]; // Upper limit on size of output
    int outBuffPosn = 0;
    byte[] b4       = new byte[4];
    int b4Posn      = 0;
    int i           = 0;
    byte sbiCrop    = 0;
    byte sbiDecode  = 0;
    for (i = off; i < (off + len); i++) {
      sbiCrop     = (byte) (data[i] & 0x7f); // Only the low seven bits
      sbiDecode   = DECODABET[sbiCrop];
      if (sbiDecode >= WHITE_SPACE_ENC) // White space, Equals sign or better
       {
        if (sbiDecode >= EQUALS_SIGN_ENC) {
          b4[b4Posn++] = sbiCrop;
          if (b4Posn > 3) {
            outBuffPosn += decode4to3(b4, 0, outBuff, outBuffPosn);
            b4Posn = 0;
            // If that was the equals sign, break out of 'for' loop
            if (sbiCrop == EQUALS_SIGN) {
              break;
            }
          }
        }
      } else {
        throw new IllegalArgumentException(
          "Bad Base64 input character at " + i + ": " + data[i] + "(decimal)");
      }
    } // each input character
    byte[] result = new byte[outBuffPosn];
    System.arraycopy(outBuff, 0, result, 0, outBuffPosn);
    return result;
  }

  private static int decode4to3(
    byte[] source, int srcOffset, byte[] destination, int destOffset) {
    if (source[srcOffset + 2] == EQUALS_SIGN) {
      int outBuff = ((DECODABET[source[srcOffset]] & 0xFF) << 18) |
        ((DECODABET[source[srcOffset + 1]] & 0xFF) << 12);
      destination[destOffset] = (byte) (outBuff >>> 16);
      return 1;
    } else if (source[srcOffset + 3] == EQUALS_SIGN) {
      int outBuff = ((DECODABET[source[srcOffset]] & 0xFF) << 18) |
        ((DECODABET[source[srcOffset + 1]] & 0xFF) << 12) |
        ((DECODABET[source[srcOffset + 2]] & 0xFF) << 6);
      destination[destOffset]       = (byte) (outBuff >>> 16);
      destination[destOffset + 1]   = (byte) (outBuff >>> 8);
      return 2;
    } else {
      try {
        int outBuff = ((DECODABET[source[srcOffset]] & 0xFF) << 18) |
          ((DECODABET[source[srcOffset + 1]] & 0xFF) << 12) |
          ((DECODABET[source[srcOffset + 2]] & 0xFF) << 6) |
          (DECODABET[source[srcOffset + 3]] & 0xFF);
        destination[destOffset]       = (byte) (outBuff >> 16);
        destination[destOffset + 1]   = (byte) (outBuff >> 8);
        destination[destOffset + 2]   = (byte) (outBuff);
        return 3;
      } catch (Exception e) {
        return -1;
      }
    }
  }

  private static byte[] encode3to4(
    byte[] source, int srcOffset, int numSigBytes, byte[] destination,
    int destOffset) {
    int inBuff = ((numSigBytes > 0) ? ((source[srcOffset] << 24) >>> 8) : 0) |
      ((numSigBytes > 1) ? ((source[srcOffset + 1] << 24) >>> 16) : 0) |
      ((numSigBytes > 2) ? ((source[srcOffset + 2] << 24) >>> 24) : 0);
    switch (numSigBytes) {
      case 3:
        destination[destOffset] = ALPHABET[(inBuff >>> 18)];
        destination[destOffset + 1] = ALPHABET[(inBuff >>> 12) & 0x3f];
        destination[destOffset + 2] = ALPHABET[(inBuff >>> 6) & 0x3f];
        destination[destOffset + 3] = ALPHABET[(inBuff) & 0x3f];
        return destination;
      case 2:
        destination[destOffset] = ALPHABET[(inBuff >>> 18)];
        destination[destOffset + 1] = ALPHABET[(inBuff >>> 12) & 0x3f];
        destination[destOffset + 2] = ALPHABET[(inBuff >>> 6) & 0x3f];
        destination[destOffset + 3] = EQUALS_SIGN;
        return destination;
      case 1:
        destination[destOffset] = ALPHABET[(inBuff >>> 18)];
        destination[destOffset + 1] = ALPHABET[(inBuff >>> 12) & 0x3f];
        destination[destOffset + 2] = EQUALS_SIGN;
        destination[destOffset + 3] = EQUALS_SIGN;
        return destination;
      default:
        return destination;
    }
  }
}

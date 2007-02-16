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

import java.text.DecimalFormat;

import java.util.Locale;


/**
 * Contains some methods for string manipulation.
 */
public class StringUtilities {
  /** Default rounding precision (digits after decimal point) */
  public static final int DEFAULT_ROUND_PRECISION = 16;
  private static DecimalFormat df;

  static {
    Locale.setDefault(Locale.US);
    df = new DecimalFormat();
    df.setGroupingUsed(false);
  }

  /**
   * Checks if a string contains illegal characters with respect to the XML
   * specification.
   *
   * @param text the text to be checked
   *
   * @return true if illegal chars contained, otherwise false
   */
  public static boolean containsIllegalChars(String text) {
    int size        = text.length();
    boolean illegal = false;
    for (int i = 0; i < size; i++) {
      char c = text.charAt(i);
      if ((c < 32) && (c != '\t') && (c != '\n') && (c != '\r')) {
        illegal = true;
        break;
      }
    }
    return illegal;
  }

  /**
   * Converts a double to a string. You can specify the rounding precision,
   * i.e. the number of digits after the decimal point.
   *
   * @param d double to be converted
   * @param precision rounding precision
   *
   * @return a string representing the passed double value
   */
  public static String doubleToString(double d, int precision) {
    synchronized (df) {
      df.setMaximumFractionDigits(precision);
      return df.format(d);
    }
  }

  /**
   * Converts a double to a string. The default rounding precision is used.
   *
   * @param d double to be converted
   *
   * @return a string representing the passed double value
   */
  public static String doubleToString(double d) {
    return doubleToString(d, DEFAULT_ROUND_PRECISION);
  }

  /**
   * Cleans strings of illegal characters with respect to the XML
   * specification.
   *
   * @param text string to be cleaned
   *
   * @return the cleaned string
   */
  public static String purgeString(String text) {
    char[] block        = null;
    StringBuffer buffer = new StringBuffer();
    int i;
    int last            = 0;
    int size            = text.length();
    for (i = 0; i < size; i++) {
      char c = text.charAt(i);
      if ((c < 32) && (c != '\t') && (c != '\n') && (c != '\r')) {
        // remove character
        if (block == null) {
          block = text.toCharArray();
        }
        buffer.append(block, last, i - last);
        last = i + 1;
      }
    }
    if (last == 0) {
      return text;
    }
    if (last < size) {
      if (block == null) {
        block = text.toCharArray();
      }
      buffer.append(block, last, i - last);
    }
    return buffer.toString();
  }
}

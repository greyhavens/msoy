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

package com.jswiff.swfrecords;

import java.io.Serializable;

/**
 * This class identifies a spoken language that applies to text. Used by Flash
 * Player for line breaking of dynamic text and for choosing fallback fonts.
 */
public final class LangCode implements Serializable {
  /** Undefined language */
  public static final byte UNDEFINED           = 0;
  /** Latin-1 (western) languages: English, French, German etc. */
  public static final byte LATIN               = 1;
  /** Japanese */
  public static final byte JAPANESE            = 2;
  /** Korean */
  public static final byte KOREAN              = 3;
  /** Simplified Chinese */
  public static final byte SIMPLIFIED_CHINESE  = 4;
  /** Traditional Chinese */
  public static final byte TRADITIONAL_CHINESE = 5;
  private byte languageCode;

  /**
   * Creates a new LangCode instance. Specify one of the supplied constants
   * (<code>UNDEFINED</code>, <code>LATIN</code>, <code>JAPANESE</code>,
   * <code>KOREAN</code>, <code>SIMPLIFIED_CHINESE</code> or
   * <code>TRADITIONAL_CHINESE</code>).
   *
   * @param languageCode language code.
   */
  public LangCode(byte languageCode) {
    this.languageCode = languageCode;
  }

  /**
   * Returns the language code (one of the constants <code>UNDEFINED</code>,
   * <code>LATIN</code>, <code>JAPANESE</code>, <code>KOREAN</code>,
   * <code>SIMPLIFIED_CHINESE</code> or <code>TRADITIONAL_CHINESE</code>)
   *
   * @return language code
   */
  public byte getLanguageCode() {
    return languageCode;
  }

  /**
   * Returns a string representation of the language.
   *
   * @return string representation
   */
  public String toString() {
    switch (languageCode) {
      case UNDEFINED:
        return "Undefined";
      case LATIN:
        return "Latin";
      case JAPANESE:
        return "Japanese";
      case KOREAN:
        return "Korean";
      case SIMPLIFIED_CHINESE:
        return "Simplified Chinese";
      case TRADITIONAL_CHINESE:
        return "Traditional Chinese";
      default:
        return "Unknown (" + languageCode + ")";
    }
  }
}

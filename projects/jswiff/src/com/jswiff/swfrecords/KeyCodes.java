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

/**
 * This class contains key codes. Used in <code>ButtonCondAction</code> and
 * <code>ClipActionRecord</code>.
 *
 * @see ButtonCondAction
 * @see ClipActionRecord
 */
public final class KeyCodes {
  /** Left arrow key */
  public static final byte KEY_LEFT      = 1;
  /** Right arrow key */
  public static final byte KEY_RIGHT     = 2;
  /** Home key */
  public static final byte KEY_HOME      = 3;
  /** End key */
  public static final byte KEY_END       = 4;
  /** Insert key */
  public static final byte KEY_INSERT    = 5;
  /** Delete key */
  public static final byte KEY_DELETE    = 6;
  /** Backspace key */
  public static final byte KEY_BACKSPACE = 8;
  /** Enter key */
  public static final byte KEY_ENTER     = 13;
  /** Up arrow key */
  public static final byte KEY_UP        = 14;
  /** Down arrow key */
  public static final byte KEY_DOWN      = 15;
  /** Page up key */
  public static final byte KEY_PAGE_UP   = 16;
  /** Page down key */
  public static final byte KEY_PAGE_DOWN = 17;
  /** Tab key */
  public static final byte KEY_TAB       = 18;
  /** Escape key */
  public static final byte KEY_ESCAPE    = 19;
  /** Space key */
  public static final byte KEY_SPACE     = 32;

  private KeyCodes() {
    // prohibit instantiation
  }
}

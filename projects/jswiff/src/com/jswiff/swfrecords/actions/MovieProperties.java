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
 * This class contains constants used as movie property indexes by
 * <code>GetProperty</code> and <code>SetProperty</code>.
 */
public final class MovieProperties {
  /** x coordinate */
  public static final int X              = 0;
  /** y coordinate */
  public static final int Y              = 1;
  /** Horizontal scale in percent */
  public static final int X_SCALE        = 2;
  /** Vertical scale in percent */
  public static final int Y_SCALE        = 3;
  /** Frame number in which the playhead is located */
  public static final int CURRENT_FRAME  = 4;
  /** The total number of frames */
  public static final int TOTAL_FRAMES   = 5;
  /** Transparency value */
  public static final int ALPHA          = 6;
  /** Indicates whether visible or not */
  public static final int VISIBLE        = 7;
  /** Width in pixels */
  public static final int WIDTH          = 8;
  /** Height in pixels */
  public static final int HEIGHT         = 9;
  /** Rotation in degrees */
  public static final int ROTATION       = 10;
  /** Target path */
  public static final int TARGET         = 11;
  /** Number of frames loaded from a streaming movie */
  public static final int FRAMES_LOADED  = 12;
  /** Instance name */
  public static final int NAME           = 13;
  /** Absolute path in slash syntax notation */
  public static final int DROP_TARGET    = 14;
  /** URL of the SWF file */
  public static final int URL            = 15;
  /** Level of anti-aliasing - superseded by <code>QUALITY</code> as of SWF 5 */
  public static final int HIGH_QUALITY   = 16;
  /**
   * Specifies whether a yellow rectangle appears around the movie when having
   * keyboard focus
   */
  public static final int FOCUS_RECT     = 17;
  /** Seconds of streaming sound to prebuffer */
  public static final int SOUND_BUF_TIME = 18;
  /** Stores a string that dictates the rendering quality of the Flash Player */
  public static final int QUALITY        = 19;
  /** x coordinate of the mouse position */
  public static final int X_MOUSE        = 20;
  /** y coordinate of the mouse position */
  public static final int Y_MOUSE        = 21;

  private MovieProperties() {
    // no need to instantiate
  }
}

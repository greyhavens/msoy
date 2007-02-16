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

import com.jswiff.io.InputBitStream;
import com.jswiff.io.OutputBitStream;

import java.io.IOException;
import java.io.Serializable;


/**
 * This class implements a container for event flags. Used in
 * <code>ClipActions</code> and <code>ClipActionRecord</code> to indicate
 * which events to react upon.
 *
 * @see ClipActions
 * @see ClipActionRecord
 */
public final class ClipEventFlags implements Serializable {
  private boolean keyUp;
  private boolean keyDown;
  private boolean mouseUp;
  private boolean mouseDown;
  private boolean mouseMove;
  private boolean unload;
  private boolean enterFrame;
  private boolean load;
  private boolean dragOver;
  private boolean rollOut;
  private boolean rollOver;
  private boolean releaseOutside;
  private boolean release;
  private boolean press;
  private boolean initialize;
  private boolean data;
  private boolean construct;
  private boolean keyPress;
  private boolean dragOut;

  /**
   * Creates a new ClipEventFlags instance.
   */
  public ClipEventFlags() {
    // do nothing
  }

  /**
   * Creates a new ClipEventFlags instance, reading data from a bit stream.
   *
   * @param stream source bit stream
   * @param swfVersion SWF version
   *
   * @throws IOException if an I/O error has occured
   */
  public ClipEventFlags(InputBitStream stream, short swfVersion)
    throws IOException {
    keyUp            = stream.readBooleanBit();
    keyDown          = stream.readBooleanBit();
    mouseUp          = stream.readBooleanBit();
    mouseDown        = stream.readBooleanBit();
    mouseMove        = stream.readBooleanBit();
    unload           = stream.readBooleanBit();
    enterFrame       = stream.readBooleanBit();
    load             = stream.readBooleanBit();
    dragOver         = stream.readBooleanBit();
    rollOut          = stream.readBooleanBit();
    rollOver         = stream.readBooleanBit();
    releaseOutside   = stream.readBooleanBit();
    release          = stream.readBooleanBit();
    press            = stream.readBooleanBit();
    initialize       = stream.readBooleanBit();
    data             = stream.readBooleanBit();
    if (swfVersion >= 6) {
      // 5 reserved bits
      stream.readUnsignedBits(5);
      construct   = stream.readBooleanBit();
      keyPress    = stream.readBooleanBit();
      dragOut     = stream.readBooleanBit();
      // 8 reserved bits
      stream.readUnsignedBits(8);
    }
  }

  /**
   * Sets the construct flag.
   */
  public void setConstruct() {
    construct = true;
  }

  /**
   * Checks if construct flag is set.
   *
   * @return <code>true</code> if set, otherwise <code>false</code>
   */
  public boolean isConstruct() {
    return construct;
  }

  /**
   * Sets the data flag.
   */
  public void setData() {
    data = true;
  }

  /**
   * Checks if data flag is set.
   *
   * @return <code>true</code> if set, otherwise <code>false</code>
   */
  public boolean isData() {
    return data;
  }

  /**
   * Sets the dragOut flag.
   */
  public void setDragOut() {
    dragOut = true;
  }

  /**
   * Checks if dragOut flag is set.
   *
   * @return <code>true</code> if set, otherwise <code>false</code>
   */
  public boolean isDragOut() {
    return dragOut;
  }

  /**
   * Sets the dragOver flag.
   */
  public void setDragOver() {
    dragOver = true;
  }

  /**
   * Checks if dragOver flag is set.
   *
   * @return <code>true</code> if set, otherwise <code>false</code>
   */
  public boolean isDragOver() {
    return dragOver;
  }

  /**
   * Sets the enterFrame flag.
   */
  public void setEnterFrame() {
    enterFrame = true;
  }

  /**
   * Checks if enterFrame flag is set.
   *
   * @return <code>true</code> if set, otherwise <code>false</code>
   */
  public boolean isEnterFrame() {
    return enterFrame;
  }

  /**
   * Sets the initialize flag.
   */
  public void setInitialize() {
    initialize = true;
  }

  /**
   * Checks if initialize flag is set.
   *
   * @return <code>true</code> if set, otherwise <code>false</code>
   */
  public boolean isInitialize() {
    return initialize;
  }

  /**
   * Sets the keyDown flag.
   */
  public void setKeyDown() {
    keyDown = true;
  }

  /**
   * Checks if keyDown flag is set.
   *
   * @return <code>true</code> if set, otherwise <code>false</code>
   */
  public boolean isKeyDown() {
    return keyDown;
  }

  /**
   * Sets the keyPress flag.
   */
  public void setKeyPress() {
    keyPress = true;
  }

  /**
   * Checks if keyPress flag is set.
   *
   * @return <code>true</code> if set, otherwise <code>false</code>
   */
  public boolean isKeyPress() {
    return keyPress;
  }

  /**
   * Sets the keyUp flag.
   */
  public void setKeyUp() {
    keyUp = true;
  }

  /**
   * Checks if keyUp flag is set.
   *
   * @return <code>true</code> if set, otherwise <code>false</code>
   */
  public boolean isKeyUp() {
    return keyUp;
  }

  /**
   * Sets the load flag.
   */
  public void setLoad() {
    load = true;
  }

  /**
   * Checks if load flag is set.
   *
   * @return <code>true</code> if set, otherwise <code>false</code>
   */
  public boolean isLoad() {
    return load;
  }

  /**
   * Sets the mouseDown flag.
   */
  public void setMouseDown() {
    mouseDown = true;
  }

  /**
   * Checks if mouseDown flag is set.
   *
   * @return <code>true</code> if set, otherwise <code>false</code>
   */
  public boolean isMouseDown() {
    return mouseDown;
  }

  /**
   * Sets the mouseMove flag.
   */
  public void setMouseMove() {
    mouseMove = true;
  }

  /**
   * Checks if mouseMove flag is set.
   *
   * @return <code>true</code> if set, otherwise <code>false</code>
   */
  public boolean isMouseMove() {
    return mouseMove;
  }

  /**
   * Sets the mouseUp flag.
   */
  public void setMouseUp() {
    mouseUp = true;
  }

  /**
   * Checks if mouseup flag is set.
   *
   * @return <code>true</code> if set, otherwise <code>false</code>
   */
  public boolean isMouseUp() {
    return mouseUp;
  }

  /**
   * Sets the press flag.
   */
  public void setPress() {
    press = true;
  }

  /**
   * Checks if press flag is set.
   *
   * @return <code>true</code> if set, otherwise <code>false</code>
   */
  public boolean isPress() {
    return press;
  }

  /**
   * Sets the release flag.
   */
  public void setRelease() {
    release = true;
  }

  /**
   * Checks if release flag is set.
   *
   * @return <code>true</code> if set, otherwise <code>false</code>
   */
  public boolean isRelease() {
    return release;
  }

  /**
   * Sets the releaseOutside flag.
   */
  public void setReleaseOutside() {
    releaseOutside = true;
  }

  /**
   * Checks if releaseOutside flag is set.
   *
   * @return <code>true</code> if set, otherwise <code>false</code>
   */
  public boolean isReleaseOutside() {
    return releaseOutside;
  }

  /**
   * Sets the rollOut flag.
   */
  public void setRollOut() {
    rollOut = true;
  }

  /**
   * Checks if rollOut flag is set.
   *
   * @return <code>true</code> if set, otherwise <code>false</code>
   */
  public boolean isRollOut() {
    return rollOut;
  }

  /**
   * Sets the rollOver flag.
   */
  public void setRollOver() {
    rollOver = true;
  }

  /**
   * Checks if rollOver flag is set.
   *
   * @return <code>true</code> if set, otherwise <code>false</code>
   */
  public boolean isRollOver() {
    return rollOver;
  }

  /**
   * Sets the unload flag.
   */
  public void setUnload() {
    unload = true;
  }

  /**
   * Checks if unload flag is set.
   *
   * @return <code>true</code> if set, otherwise <code>false</code>
   */
  public boolean isUnload() {
    return unload;
  }

  /**
   * Writes this instance to a bit stream.
   *
   * @param stream target bit stream
   * @param swfVersion SWF version
   *
   * @throws IOException if an I/O error has occured
   */
  public void write(OutputBitStream stream, short swfVersion)
    throws IOException {
    stream.writeBooleanBit(keyUp);
    stream.writeBooleanBit(keyDown);
    stream.writeBooleanBit(mouseUp);
    stream.writeBooleanBit(mouseDown);
    stream.writeBooleanBit(mouseMove);
    stream.writeBooleanBit(unload);
    stream.writeBooleanBit(enterFrame);
    stream.writeBooleanBit(load);
    stream.writeBooleanBit(dragOver);
    stream.writeBooleanBit(rollOut);
    stream.writeBooleanBit(rollOver);
    stream.writeBooleanBit(releaseOutside);
    stream.writeBooleanBit(release);
    stream.writeBooleanBit(press);
    stream.writeBooleanBit(initialize);
    stream.writeBooleanBit(data);
    if (swfVersion >= 6) {
      stream.writeUnsignedBits(0, 5); // 5 reserved bits
      stream.writeBooleanBit(construct);
      stream.writeBooleanBit(keyPress);
      stream.writeBooleanBit(dragOut);
      stream.writeUnsignedBits(0, 8); // 8 reserved bits
    }
  }
}

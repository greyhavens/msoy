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


/**
 * <p>
 * Instructs Flash Player to go to the specified frame.
 * </p>
 * 
 * <p>
 * Performed stack operations:<br>
 * <code>pop frame</code> (frame number or label)<br>
 * </p>
 * 
 * <p>
 * ActionScript equivalent: <code>gotoAndPlay(), gotoAndStop()</code>
 * </p>
 *
 * @since SWF 4
 */
public final class GoToFrame2 extends Action {
  private boolean play;
  private int sceneBias;

  /**
   * Creates a new GoToFrame2 action. If the <code>play</code> flag is set, the
   * movie starts playing at the specified frame, otherwise it stops at that
   * frame. The <code>sceneBias</code> parameter is a non-negative offset to
   * be added to the specified frame.
   *
   * @param play play flag
   * @param sceneBias offset added to target frame
   */
  public GoToFrame2(boolean play, int sceneBias) {
    code             = ActionConstants.GO_TO_FRAME_2;
    this.play        = play;
    this.sceneBias   = sceneBias;
  }

  GoToFrame2(InputBitStream stream) throws IOException {
    code = ActionConstants.GO_TO_FRAME_2;
    short bits            = stream.readUI8();

    // 6 reserved bits
    boolean sceneBiasFlag = ((bits & 2) != 0);
    play                  = ((bits & 1) != 0);
    if (sceneBiasFlag) {
      sceneBias = stream.readUI16();
    }
  }

  /**
   * Returns the scene bias, i.e. the non-negative offset to be added to the
   * target frame.
   *
   * @return offset added to target frame
   */
  public int getSceneBias() {
    return sceneBias;
  }

  /**
   * Returns the size of this action record in bytes.
   *
   * @return size of this record
   *
   * @see Action#getSize()
   */
  public int getSize() {
    int size = 4;
    if (sceneBias > 0) {
      size += 2;
    }
    return size;
  }

  /**
   * Returns the state of the <code>play</code> flag. If <code>true</code>, the
   * movie starts playing at the specified frame. Otherwise, the movie stops
   * at that frame.
   *
   * @return state of <code>play</code> flag
   */
  public boolean play() {
    return play;
  }

  /**
   * Returns a short description of this action, the state of the
   * <code>play</code> flag and an eventual <code>sceneBias</code>.
   *
   * @return <code>"GoToFrame2", play, sceneBias</code>
   */
  public String toString() {
    String result = "GoToFrame2  play: " + play;
    if (sceneBias > 0) {
      result += (" sceneBias: " + sceneBias);
    }
    return result;
  }

  protected void writeData(
    OutputBitStream dataStream, OutputBitStream mainStream)
    throws IOException {
    dataStream.writeUnsignedBits(0, 6);
    boolean sceneBiasFlag = (sceneBias > 0);
    dataStream.writeBooleanBit(sceneBiasFlag);
    dataStream.writeBooleanBit(play);
    if (sceneBiasFlag) {
      dataStream.writeUI16(sceneBias);
    }
  }
}

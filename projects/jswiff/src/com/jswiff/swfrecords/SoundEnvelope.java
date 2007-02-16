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
 * This class is used for time-based volume control within
 * <code>SoundInfo</code>. Defines the volume level for the left and right
 * channel, starting at a certain sound sample called the 'envelope point'.
 *
 * @see SoundInfo
 */
public final class SoundEnvelope implements Serializable {
  private long pos44;
  private int leftLevel;
  private int rightLevel;

  /**
   * Creates a new SoundEnvelope instance. Specify the position of the envelope
   * point within the sound as a number of 44 kHz samples (multiply
   * accordingly when using a lower sampling rate). Then supply the volume
   * level for the left and right channel. For mono sounds, use identical
   * values.
   *
   * @param pos44 envelope point in number of 44 kHz samples
   * @param leftLevel left volume level (between 0 and 32768)
   * @param rightLevel right volume level (between 0 and 32768)
   */
  public SoundEnvelope(long pos44, int leftLevel, int rightLevel) {
    this.pos44        = pos44;
    this.leftLevel    = leftLevel;
    this.rightLevel   = rightLevel;
  }

  /**
   * Creates a new SoundEnvelope instance, reading data from a bit stream.
   *
   * @param stream source bit stream
   *
   * @throws IOException if an I/O error occured
   */
  public SoundEnvelope(InputBitStream stream) throws IOException {
    pos44        = stream.readUI32();
    leftLevel    = stream.readUI16();
    rightLevel   = stream.readUI16();
  }

  /**
   * Returns the volume level of the left channel.
   *
   * @return left channel volume (between 0 and 32768)
   */
  public int getLeftLevel() {
    return leftLevel;
  }

  /**
   * Position of the envelope point as a number of 44 kHz samples (multiplied
   * accordingly when using a lower sampling rate).
   *
   * @return envelope point in number of 44 kHz samples
   */
  public long getPos44() {
    return pos44;
  }

  /**
   * Returns the volume level of the right channel.
   *
   * @return right channel volume (between 0 and 32768)
   */
  public int getRightLevel() {
    return rightLevel;
  }

  /**
   * Writes the instance to a bit stream.
   *
   * @param stream target bit stream
   *
   * @throws IOException if an I/O error occured
   */
  public void write(OutputBitStream stream) throws IOException {
    stream.writeUI32(pos44);
    stream.writeUI16(leftLevel);
    stream.writeUI16(rightLevel);
  }
}

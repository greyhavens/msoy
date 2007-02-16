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

package com.jswiff.swfrecords;

import com.jswiff.io.InputBitStream;
import com.jswiff.io.OutputBitStream;

import java.io.IOException;


/**
 * Use only with <code>DefineMorphShape2</code>.
 */
public final class FocalMorphGradient extends MorphGradient {
  private double startFocalPointRatio;
  private double endFocalPointRatio;

  /**
   * Creates a new FocalMorphGradient instance.
   *
   * @param gradientRecords TODO: Comments
   * @param startFocalPointRatio TODO: Comments
   * @param endFocalPointRatio TODO: Comments
   */
  public FocalMorphGradient(
    MorphGradRecord[] gradientRecords, double startFocalPointRatio,
    double endFocalPointRatio) {
    super(gradientRecords);
    this.startFocalPointRatio   = startFocalPointRatio;
    this.endFocalPointRatio     = endFocalPointRatio;
  }

  FocalMorphGradient(InputBitStream stream) throws IOException {
    super(stream);
    startFocalPointRatio   = stream.readFP16();
    endFocalPointRatio     = stream.readFP16();
  }

  /**
   * TODO: Comments
   *
   * @param endFocalPointRatio TODO: Comments
   */
  public void setEndFocalPointRatio(double endFocalPointRatio) {
    this.endFocalPointRatio = endFocalPointRatio;
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public double getEndFocalPointRatio() {
    return endFocalPointRatio;
  }

  /**
   * TODO: Comments
   *
   * @param startFocalPointRatio TODO: Comments
   */
  public void setStartFocalPointRatio(double startFocalPointRatio) {
    this.startFocalPointRatio = startFocalPointRatio;
  }

  /**
   * TODO: Comments
   *
   * @return TODO: Comments
   */
  public double getStartFocalPointRatio() {
    return startFocalPointRatio;
  }

  void write(OutputBitStream stream) throws IOException {
    super.write(stream);
    stream.writeFP16(startFocalPointRatio);
    stream.writeFP16(endFocalPointRatio);
  }
}

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

import java.io.IOException;
import java.io.Serializable;


/**
 * Instances of this class represent a header of an SWF file.
 */
public final class SWFHeader implements Serializable {
  private boolean compressed;
  private short version;
  private long fileLength;
  private Rect frameSize;
  private short frameRate;
  private int frameCount;

  /**
   * Creates a new SWFHeader instance.
   */
  public SWFHeader() {
    // empty
  }

  /**
   * Reads an SWF header from a bit stream.
   *
   * @param stream a bit stream
   *
   * @throws IOException if an I/O error has occurred
   */
  public SWFHeader(InputBitStream stream) throws IOException {
    read(stream);
  }

  /**
   * Sets the compression flag of the SWF
   *
   * @param compressed <code>true</code> activates the compression
   */
  public void setCompressed(boolean compressed) {
    this.compressed = compressed;
  }

  /**
   * Checks if the SWF file is compressed.
   *
   * @return <code>true</code> if compression is on, otherwise
   *         <code>false</code>
   */
  public boolean isCompressed() {
    return compressed;
  }

  /**
   * Sets the file length. This value is computed and set when writing the SWF
   * with <code>SWFWriter</code>.
   *
   * @param fileLength file length in bytes
   */
  public void setFileLength(long fileLength) {
    this.fileLength = fileLength;
  }

  /**
   * Returns the length of the SWF file.
   *
   * @return SWF file length
   */
  public long getFileLength() {
    return fileLength;
  }

  /**
   * Sets the number of frames in the SWF movie. This value is computed and set
   * when the SWF is written with <code>SWFWriter</code>.
   *
   * @param frameCount frame count
   */
  public void setFrameCount(int frameCount) {
    this.frameCount = frameCount;
  }

  /**
   * Returns the number of frames in the SWF movie.
   *
   * @return frame count
   */
  public int getFrameCount() {
    return frameCount;
  }

  /**
   * Sets the frame rate of the SWF movie.
   *
   * @param frameRate the frame rate in fps
   */
  public void setFrameRate(short frameRate) {
    this.frameRate = frameRate;
  }

  /**
   * Returns the frame rate of the SWF movie.
   *
   * @return the frame rate in fps
   */
  public short getFrameRate() {
    return frameRate;
  }

  /**
   * Sets the frame size of the SWF.
   *
   * @param frameSize the frame size (as <code>Rect</code> instance)
   */
  public void setFrameSize(Rect frameSize) {
    this.frameSize = frameSize;
  }

  /**
   * Returns the frame size of the SWF movie.
   *
   * @return the frame size (as <code>Rect</code> instance)
   */
  public Rect getFrameSize() {
    return frameSize;
  }

  /**
   * Sets the SWF version of the file.
   *
   * @param version SWF version
   */
  public void setVersion(short version) {
    this.version = version;
  }

  /**
   * Returns the SWF version of the file.
   *
   * @return SWF version
   */
  public short getVersion() {
    return version;
  }

  private void read(InputBitStream stream) throws IOException {
    // read signature
    short compressionByte = stream.readUI8();

    // header starts with CWS (0x43 0x57 0x53) for compressed
    // or FWS (0x46 0x57 0x53) for uncompressed files
    if (
      ((compressionByte != 0x43) && (compressionByte != 0x46)) ||
          (stream.readUI8() != 0x57) || (stream.readUI8() != 0x53)) {
      throw new IOException("Invalid SWF file signature!");
    }
    if (compressionByte == 0x43) {
      compressed = true;
    }
    version      = (byte) stream.readUI8();
    fileLength   = stream.readUI32();
    if (compressed) {
      stream.enableCompression();
    }
    frameSize = new Rect(stream);
    stream.readUI8(); // ignore one byte
    frameRate    = stream.readUI8();
    frameCount   = stream.readUI16();
  }
}

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

package com.jswiff.swfrecords.tags;

import com.jswiff.io.InputBitStream;
import com.jswiff.io.OutputBitStream;

import java.io.IOException;


/**
 * This tag is used as of SWF 8 to define SWF properties like access mode and
 * the presence of metadata. Do NOT add this tag to your
 * <code>SWFDocument</code>, use its <code>setAccessMode</code> and
 * <code>setMetadata</code> methods instead!
 *
 * @see com.jswiff.SWFDocument#setAccessMode(byte)
 * @see com.jswiff.SWFDocument#setMetadata(String)
 * @since SWF 8
 */
public class FileAttributes extends Tag {
  private boolean allowNetworkAccess;
  private boolean hasMetadata;

  /**
   * Creates a new FileAttributes instance.
   */
  public FileAttributes() {
    code = TagConstants.FILE_ATTRIBUTES;
  }

  /**
   * Specifies whether the SWF is granted network or local access.
   *
   * @param allowNetworkAccess true for network access, false for local access
   */
  public void setAllowNetworkAccess(boolean allowNetworkAccess) {
    this.allowNetworkAccess = allowNetworkAccess;
  }

  /**
   * Checks whether the SWF is granted network or local access.
   *
   * @return true for network access, false for local access
   */
  public boolean isAllowNetworkAccess() {
    return allowNetworkAccess;
  }

  /**
   * Specifies whether the SWF contains metadata (in a Metadata tag).
   *
   * @param hasMetadata true if Metadata tag contained
   */
  public void setHasMetadata(boolean hasMetadata) {
    this.hasMetadata = hasMetadata;
  }

  /**
   * Checks whether the SWF contains metadata (in a Metadata tag).
   *
   * @return true if Metadata tag contained
   */
  public boolean hasMetadata() {
    return hasMetadata;
  }

  protected void writeData(OutputBitStream outStream) throws IOException {
    int flags = 0;
    if (allowNetworkAccess) {
      flags |= 0x01;
    }
    if (hasMetadata) {
      flags |= 0x10;
    }
    outStream.writeSI32(flags);
  }

  void setData(byte[] data) throws IOException {
    InputBitStream inStream = new InputBitStream(data);
    int flags               = inStream.readSI32();
    allowNetworkAccess      = ((flags & 0x01) != 0);
    hasMetadata             = ((flags & 0x10) != 0);
  }
}

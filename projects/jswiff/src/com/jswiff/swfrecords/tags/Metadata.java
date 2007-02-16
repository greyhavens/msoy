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
 * Contains XML metadata in Dublin Core RDF format. Do NOT add this tag to your
 * <code>SWFDocument</code>, use its <code>setMetadata</code> method instead!
 */
public class Metadata extends Tag {
  private String dataString;

  /**
   * Creates a new Metadata instance.
   *
   * @param dataString metadata as Dublin Core RDF
   */
  public Metadata(String dataString) {
    this.dataString   = dataString;
    code              = TagConstants.METADATA;
  }

  Metadata() {
    // nothing to do
  }

  /**
   * Sets the metadata of the document. Use Dublin Core RDF.
   *
   * @param dataString metadata as Dublin Core RDF
   */
  public void setDataString(String dataString) {
    this.dataString = dataString;
  }

  /**
   * Returns the metadata of the document (if set). Dublin Core RDF is used.
   *
   * @return metadata as Dublin Core RDF
   */
  public String getDataString() {
    return dataString;
  }

  protected void writeData(OutputBitStream outStream) throws IOException {
    outStream.writeString(dataString);
  }

  void setData(byte[] data) throws IOException {
    InputBitStream inStream = new InputBitStream(data);
    dataString = inStream.readString();
  }
}

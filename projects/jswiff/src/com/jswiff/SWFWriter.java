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

package com.jswiff;

import com.jswiff.io.OutputBitStream;
import com.jswiff.swfrecords.tags.FileAttributes;
import com.jswiff.swfrecords.tags.Metadata;
import com.jswiff.swfrecords.tags.SetBackgroundColor;
import com.jswiff.swfrecords.tags.Tag;
import com.jswiff.swfrecords.tags.TagConstants;
import com.jswiff.swfrecords.tags.TagWriter;

import java.io.IOException;
import java.io.OutputStream;

import java.util.Iterator;


/**
 * This class writes an SWF document (an <code>SWFDocument</code> instance) to
 * an output stream. Usage:
 * <pre>
 * <code>
 * SWFWriter writer = new SWFWriter(doc, outputStream);
 * writer.write();
 * </code>
 * </pre>
 */
public class SWFWriter {
  private OutputBitStream bitStream;
  private SWFDocument document;
  private boolean japanese;

  /**
   * Creates a new SWF writer which writes the specified SWF document to the
   * stream supplied here.
   *
   * @param document the SWF document to be written
   * @param stream the output stream the SWF file is written to
   */
  public SWFWriter(SWFDocument document, OutputStream stream) {
    bitStream       = new OutputBitStream(stream);
    this.document   = document;
  }

  /**
   * Specifies whether strings should be encoded using Japanese encoding
   * (Shift-JIS). This is relevant only for SWF 5 or earlier, where strings
   * are encoded using either ANSI or Shift-JIS. In Flash Player, the decoding
   * choice is made depending on the locale, as this information is not stored
   * in the SWF. Later SWF versions use Unicode (UTF-8) and ignore this
   * option.
   *
   * @param japanese <code>true</code> if Shift-JIS encoding is to be used
   */
  public void setJapanese(boolean japanese) {
    this.japanese = japanese;
  }

  /**
   * Writes the SWF to the stream passed to the constructor. The stream is then
   * closed.
   *
   * @throws IOException if an I/O error occured
   */
  public void write() throws IOException {
    try {
      byte[] docPropertiesTagsBuffer = getDocPropertiesTagsBuffer();
      byte[] tagsBuffer              = TagWriter.writeTags(
          document.getTags(), document.getVersion(), japanese);
      byte[] headerEndData           = getHeaderEndData();
      long fileLength                = 8 + headerEndData.length +
        tagsBuffer.length + docPropertiesTagsBuffer.length;
      writeHeaderStart();
      bitStream.writeUI32(fileLength);
      if (document.isCompressed()) {
        bitStream.enableCompression();
      }
      bitStream.writeBytes(headerEndData);
      // header written, now write document property tags (background, file attrs, metadata)
      bitStream.writeBytes(docPropertiesTagsBuffer);
      // write all remaining tags
      bitStream.writeBytes(tagsBuffer);
    } finally {
      try {
        bitStream.close();
      } catch (Exception e) {
        // empty on purpose - don't need to propagate errors which occur while closing
      }
    }
  }

  private byte[] getDocPropertiesTagsBuffer() throws IOException {
    OutputBitStream tagStream = new OutputBitStream();
    if (document.getVersion() >= 8) {
      FileAttributes fileAttributes = new FileAttributes();
      fileAttributes.setAllowNetworkAccess(
        document.getAccessMode() == SWFDocument.ACCESS_MODE_NETWORK);
      TagWriter.writeTag(tagStream, fileAttributes, document.getVersion());
      String metadata = document.getMetadata();
      if (metadata != null) {
        fileAttributes.setHasMetadata(true);
        TagWriter.writeTag(
          tagStream, new Metadata(metadata), document.getVersion());
      }
    }
    TagWriter.writeTag(
      tagStream, new SetBackgroundColor(document.getBackgroundColor()),
      document.getVersion());
    return tagStream.getData();
  }

  private int getFrameCount() {
    int count = 0;
    for (Iterator i = document.getTags().iterator(); i.hasNext();) {
      if (((Tag) i.next()).getCode() == TagConstants.SHOW_FRAME) {
        count++;
      }
    }
    return count;
  }

  private byte[] getHeaderEndData() throws IOException {
    OutputBitStream headerStream = new OutputBitStream();

    // frame size
    document.getFrameSize().write(headerStream);
    // frame rate
    headerStream.writeUI8((short) 0); // this byte is ignored
    headerStream.writeUI8(document.getFrameRate());
    // frame count
    headerStream.writeUI16(getFrameCount());
    byte[] headerData = headerStream.getData();
    return headerData;
  }

  private void writeHeaderStart() throws IOException {
    // writes CWS/FWS and version - that's 4 bytes
    // C (0x43) for compressed or F (0x46) for uncompressed files
    bitStream.writeUI8((short) (document.isCompressed() ? 0x43 : 0x46));
    // WS (0x57 0x53)
    bitStream.writeBytes(new byte[] { 0x57, 0x53 });
    // version
    bitStream.writeUI8(document.getVersion());
  }
}

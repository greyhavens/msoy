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

package com.jswiff.xml;

import com.jswiff.SWFDocument;
import com.jswiff.swfrecords.Color;
import com.jswiff.swfrecords.Rect;
import com.jswiff.swfrecords.tags.Tag;
import com.jswiff.util.Base64;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import java.util.Iterator;
import java.util.List;


/**
 * This class converts an SWF document to XML.
 */
public class XMLWriter {
  private Document xmlDocument;
  private Element rootElement;
  private SWFDocument swfDocument;

  /**
   * Creates a new XMLWriter instance and generates an in-memory XML document
   * from the SWF document used as argument.
   *
   * @param swfDocument the SWF doc to be converted to XML
   */
  public XMLWriter(SWFDocument swfDocument) {
    this.swfDocument   = swfDocument;
    xmlDocument        = DocumentHelper.createDocument();
    rootElement        = xmlDocument.addElement("swfdocument");
    writeHeader();
    writeTags();
  }

  /**
   * Writes the XML document generated from the SWF to a stream. If the
   * <code>format</code> flag is set, the output XML is formatted to make it
   * more readable.
   *
   * @param stream the target stream
   * @param format specifies whether to format the output XML
   *
   * @throws IOException if an I/O error occured
   */
  public void write(OutputStream stream, boolean format)
    throws IOException {
    if (format) {
      OutputFormat formatter = OutputFormat.createPrettyPrint();
      formatter.setNewLineAfterDeclaration(false);
      formatter.setTrimText(false);
      org.dom4j.io.XMLWriter writer = new org.dom4j.io.XMLWriter(
          stream, formatter);
      writer.write(xmlDocument);
    } else {
      org.dom4j.io.XMLWriter writer = new org.dom4j.io.XMLWriter(stream);
      writer.write(xmlDocument);
    }
  }

  /**
   * Writes the XML document generated from the SWF to a writer. If the
   * <code>format</code> flag is set, the output XML is formatted to make it
   * more readable.
   *
   * @param writer the target writer
   * @param format specifies whether to format the output XML
   *
   * @throws IOException if an I/O error occured
   */
  public void write(Writer writer, boolean format) throws IOException {
    if (format) {
      OutputFormat formatter = OutputFormat.createPrettyPrint();
      formatter.setNewLineAfterDeclaration(false);
      formatter.setTrimText(false);
      org.dom4j.io.XMLWriter xmlWriter = new org.dom4j.io.XMLWriter(
          writer, formatter);
      xmlWriter.write(xmlDocument);
    } else {
      org.dom4j.io.XMLWriter xmlWriter = new org.dom4j.io.XMLWriter(writer);
      xmlWriter.write(xmlDocument);
    }
  }

  private void writeHeader() {
    Element headerElement = rootElement.addElement("header");
    headerElement.addAttribute(
      "swfversion", Integer.toString(swfDocument.getVersion()));
    if (swfDocument.isCompressed()) {
      headerElement.addAttribute("compressed", "true");
    }
    Element frames = headerElement.addElement("frames");
    frames.addAttribute("count", Integer.toString(swfDocument.getFrameCount()));
    frames.addAttribute("rate", Integer.toString(swfDocument.getFrameRate()));
    Rect size = swfDocument.getFrameSize();
    RecordXMLWriter.writeRect(frames, "size", size);
    Color bgColor = swfDocument.getBackgroundColor();
    if (bgColor != null) {
      RecordXMLWriter.writeColor(headerElement, "bgcolor", bgColor);
    }
    if (swfDocument.getVersion() >= 8) {
      switch (swfDocument.getAccessMode()) {
        case SWFDocument.ACCESS_MODE_LOCAL:
          headerElement.addAttribute("access", "local");
          break;
        case SWFDocument.ACCESS_MODE_NETWORK:
          headerElement.addAttribute("access", "network");
          break;
      }
      String metadata = swfDocument.getMetadata();
      if (metadata != null) {
        headerElement.addElement("metadata").addText(
          Base64.encodeString(metadata));
      }
    }
  }

  private void writeTags() {
    List tags = swfDocument.getTags();
    for (Iterator it = tags.iterator(); it.hasNext();) {
      Tag tag = (Tag) it.next();
      TagXMLWriter.writeTag(rootElement, tag);
    }
  }
}

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

package com.jswiff.tests.simple;

import com.jswiff.SWFDocument;
import com.jswiff.SWFReader;
import com.jswiff.SWFWriter;
import com.jswiff.listeners.SWFDocumentReader;
import com.jswiff.xml.XMLReader;
import com.jswiff.xml.XMLWriter;

import org.dom4j.DocumentException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * Parses an SWF file, transforms it to XML, parses it back to a SWF document
 * and writes it to a file. Useful for testing SWF and XML read/write.
 */
public class SWFXMLCopy {
  /**
   * Main method.
   *
   * @param args arguments: source and destination file
   *
   * @throws IOException if an I/O error occured
   * @throws DocumentException if XML is malformed
   */
  public static void main(String[] args) throws IOException, DocumentException {
    SWFReader reader            = new SWFReader(new FileInputStream(args[0]));
    SWFDocumentReader docReader = new SWFDocumentReader();
    reader.addListener(docReader);
    reader.read();
    SWFDocument doc            = docReader.getDocument();
    XMLWriter xmlWriter        = new XMLWriter(doc);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    xmlWriter.write(baos, false);
    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    XMLReader xmlReader       = new XMLReader(bais);
    doc                       = xmlReader.getDocument();
    SWFWriter writer          = new SWFWriter(
        doc, new FileOutputStream(args[1]));
    writer.write();
  }
}

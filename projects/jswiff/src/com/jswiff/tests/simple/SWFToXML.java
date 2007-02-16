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

package com.jswiff.tests.simple;

import com.jswiff.xml.Transformer;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * Transforms an SWF to XML and outputs it to the console.
 */
public class SWFToXML {
  /**
   * Main method.
   *
   * @param args pass one SWF file path
   *
   * @throws IOException if an I/O error occured
   */
  public static void main(String[] args) throws IOException {
    if (args.length < 2) {
      System.out.println("Please pass a source SWF and a target XML file path!");
      return;
    }
    Transformer.toXML(new FileInputStream(args[0]), new FileOutputStream(args[1]), true);
  }
}

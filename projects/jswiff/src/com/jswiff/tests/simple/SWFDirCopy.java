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

import com.jswiff.SWFDocument;
import com.jswiff.SWFReader;
import com.jswiff.SWFWriter;
import com.jswiff.listeners.SWFDocumentReader;
import com.jswiff.swfrecords.tags.Tag;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Copies all SWF files from a directory to another. Each file is first parsed
 * to an SWFDocument, which is then written to another file. Can also be used
 * for batch processing - this class decompresses all copied files.
 */
public class SWFDirCopy {
	/**
	 * Main method.
	 *
	 * @param args arguments: source and destination dir
	 *
	 * @throws IOException if an I/O error occured
	 */
	public static void main(String[] args) throws IOException {
		File sourceDir	    = new File(args[0]);
		File[] sourceFiles  = sourceDir.listFiles();
		File destinationDir = new File(args[1]);
		for (int i = 0; i < sourceFiles.length; i++) {
			File sourceFile		 = sourceFiles[i];
			File destinationFile = new File(
					destinationDir, sourceFile.getName());
			System.out.print("Duplicating file " + sourceFile + "... ");
			copy(sourceFile, destinationFile);
			copyEachTag(sourceFile, destinationFile);
			System.out.println("done.");
		}
	}

	private static void copy(File source, File destination)
		throws IOException {
		SWFReader reader		    = new SWFReader(
				new FileInputStream(source));
		SWFDocumentReader docReader = new SWFDocumentReader();
		reader.addListener(docReader);
		reader.read();
		SWFDocument doc = docReader.getDocument();
		doc.setCompressed(false);
		SWFWriter writer = new SWFWriter(
				doc, new FileOutputStream(destination));
		writer.write();
	}

	private static void copyEachTag(File source, File destination)
		throws IOException {
		SWFReader reader		    = new SWFReader(
				new FileInputStream(source));
		SWFDocumentReader docReader = new SWFDocumentReader();
		reader.addListener(docReader);
		reader.read();
		SWFDocument doc = docReader.getDocument();
		List tags	    = doc.getTags();
		List tagCopies  = new ArrayList();
		for (Iterator it = tags.iterator(); it.hasNext();) {
			Tag tag = (Tag) it.next();
			tagCopies.add(tag.copy());
		}
		tags.clear();
		doc.addTags(tagCopies);
		doc.setCompressed(false);
		SWFWriter writer = new SWFWriter(
				doc, new FileOutputStream(destination));
		writer.write();
	}
}

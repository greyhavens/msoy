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
package com.jswiff.swfrecords.tags;

import com.jswiff.io.OutputBitStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


/**
 * Base class for SWF tags.
 */
public abstract class Tag implements Serializable {
	protected boolean forceLongHeader; // override this with =true to write long headers
	protected short code;
	protected int length;
	private byte[] outData;
	private short swfVersion = 7;
  private boolean shiftJIS;

	/**
	 * Returns the tag code.
	 *
	 * @return tag code
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Creates a deep copy of this tag. Useful if you want to clone a part of a
	 * SWF document.
	 *
	 * @return a copy of the tag
	 */
	public Tag copy() {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos     = new ObjectOutputStream(baos);
			oos.writeObject(this);
			ByteArrayInputStream bais = new ByteArrayInputStream(
					baos.toByteArray());
			ObjectInputStream ois     = new ObjectInputStream(bais);
			return (Tag) ois.readObject();
		} catch (Exception e) {
			// actually, this should never happen (everything serializable??)
      // this will eventually be removed
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	/**
	 * This method writes the tag data to a bit stream. Descendants of the
	 * <code>Tag</code> class must implement this class.
	 *
	 * @param outStream target bit stream
	 *
	 * @throws IOException if an I/O error has occured
	 */
	abstract protected void writeData(OutputBitStream outStream)
		throws IOException;

	void setCode(short code) {
		this.code = code;
	}

	abstract void setData(byte[] data) throws IOException;

	void setSWFVersion(short swfVersion) {
		this.swfVersion = swfVersion;
	}

	short getSWFVersion() {
		return swfVersion;
	}
  
  void setJapanese(boolean shiftJIS) {
    this.shiftJIS = shiftJIS; 
  }
  
  boolean isJapanese() {
    return shiftJIS;
  }

	void write(OutputBitStream stream) throws IOException {
		initData(stream);
		stream.writeBytes(getHeaderData());
		stream.writeBytes(outData);
	}

	private byte[] getHeaderData() throws IOException {
		OutputBitStream headerStream = new OutputBitStream();
		int typeAndLength			 = code << 6;
		length						 = outData.length;
    //  for the length to be correct, initData() must be called before this
		if (forceLongHeader || (length >= 0x3F)) {
			// long header
			typeAndLength |= 0x3F;
			headerStream.writeUI16(typeAndLength);
			headerStream.writeUI32(length);
		} else {
			// short header
			typeAndLength |= length;
			headerStream.writeUI16(typeAndLength);
		}
		return headerStream.getData();
	}

	private void initData(OutputBitStream parentStream) throws IOException {
		OutputBitStream outStream = new OutputBitStream();
    outStream.setANSI(parentStream.isANSI());
    outStream.setShiftJIS(parentStream.isShiftJIS());
		writeData(outStream);
		outData = outStream.getData();
	}
}

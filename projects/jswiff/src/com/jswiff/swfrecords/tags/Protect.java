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

import java.io.IOException;


/**
 * <p>
 * This tag is used to advise SWF editors to restrict edition capabilities for
 * the current SWF ducument. The behavior depends on the editor's
 * implementation.
 * </p>
 * 
 * <p>
 * Upon encountering a <code>Protect</code> tag, the Macromedia Flash authoring
 * environment checks if a password is contained in this tag. If the password
 * is missing, the file import is disallowed. Otherwise, file import is
 * allowed if the correct password is specified.
 * </p>
 * 
 * <p>
 * <b>Warning:</b> This tag is advisory only. Since editors might choose to
 * ignore it, this is not an appropriate way of protecting sensitive data.
 * </p>
 * 
 * <p>
 * <b>Warning:</b> the main purpose of this tag is to mark the file as being
 * copyrighted in a way or another. Importing the file regardless of this tag
 * may be considered by the file's author as reverse engineering and copyright
 * violation.
 * </p>
 *
 * @since SWF 2
 */
public final class Protect extends Tag {
	private String password;

	/**
	 * Creates a new Protect tag. Supply a password encrypted with the MD5
	 * algorithm.
	 *
	 * @param password MD5-encrypted password
	 */
	public Protect(String password) {
		code			  = TagConstants.PROTECT;
		this.password     = password;
	}

	Protect() {
		// empty
	}

	/**
	 * Sets the MD5 encrypted password the user needs to supply in order to be
	 * able to import this SWF file.
	 *
	 * @param password MD5-encrypted password
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Returns the MD5 encrypted password the user needs to supply in order to
	 * be able to import this SWF file.
	 *
	 * @return MD5-encrypted password
	 */
	public String getPassword() {
		return password;
	}

	protected void writeData(OutputBitStream outStream)
		throws IOException {
		if (password != null) {
			outStream.writeUI16(0);
			outStream.writeString(password);
		}
	}

	void setData(byte[] data) throws IOException {
		if ((data.length > 3)) {
			// reserved UINT16=0, then passwd as MD5
			password = new String(data, 2, data.length - 3, "UTF-8");
		}
	}
}

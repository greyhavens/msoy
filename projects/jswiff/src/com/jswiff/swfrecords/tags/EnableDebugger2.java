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
 * This tag enables debugging within the Macromedia Flash authoring tool. A
 * password encrypted with the MD5 algorithm has to be supplied.
 * </p>
 * 
 * <p>
 * Note: the format of the debugging information required in the ActionScript
 * debugger has changed with version 6. In SWF 6 or later, this tag is used
 * instead of <code>EnableDebugger</code>.
 * </p>
 *
 * @since SWF 6
 */
public final class EnableDebugger2 extends Tag {
	private String password;

	/**
	 * Creates a new EnableDebugger instance. Supply a password encrypted with
	 * the MD5 algorithm.
	 *
	 * @param password MD5 encrypted password
	 */
	public EnableDebugger2(String password) {
		code			  = TagConstants.ENABLE_DEBUGGER_2;
		this.password     = password;
	}

	EnableDebugger2() {
		// empty
	}

	/**
	 * Sets the (MD5-encrypted) password.
   *
   * @param password encrypted password
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Returns the MD5-encrypted password.
	 *
	 * @return encrypted password
	 */
	public String getPassword() {
		return password;
	}

	protected void writeData(OutputBitStream outStream)
		throws IOException {
		outStream.writeUI16(0);
		if (password != null) {
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

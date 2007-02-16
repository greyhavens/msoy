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

import com.jswiff.io.InputBitStream;
import com.jswiff.io.OutputBitStream;

import java.io.IOException;


/**
 * The RemoveObject tag removes the instance of a particular character at the
 * specified depth from the display list.
 *
 * @since SWF 1
 */
public final class RemoveObject extends Tag {
	private int characterId;
	private int depth;

	/**
	 * Creates a new RemoveObject tag. Specify character ID and depth of the
	 * instance to be removed.
	 *
	 * @param characterId character ID of instance to remove
	 * @param depth depth of instance to remove
	 */
	public RemoveObject(int characterId, int depth) {
		code				 = TagConstants.REMOVE_OBJECT;
		this.characterId     = characterId;
		this.depth			 = depth;
	}

	RemoveObject() {
		// empty
	}

	/**
	 * Sets the character ID of the instance which is supposed to be removed.
	 *
	 * @param characterId character ID of instance to be removed
	 */
	public void setCharacterId(int characterId) {
		this.characterId = characterId;
	}

	/**
	 * Returns the character ID of the instance which is supposed to be
	 * removed.
	 *
	 * @return character ID of instance to be removed
	 */
	public int getCharacterId() {
		return characterId;
	}

	/**
	 * Sets the depth of the character instance which is supposed to be
	 * removed.
	 *
	 * @param depth depth of instance to be removed
	 */
	public void setDepth(int depth) {
		this.depth = depth;
	}

	/**
	 * Returns the depth of the character instance which is supposed to be
	 * removed.
	 *
	 * @return depth of instance to be removed
	 */
	public int getDepth() {
		return depth;
	}

	protected void writeData(OutputBitStream outStream)
		throws IOException {
		outStream.writeUI16(characterId);
		outStream.writeUI16(depth);
	}

	void setData(byte[] data) throws IOException {
		InputBitStream inStream = new InputBitStream(data);
		characterId     = inStream.readUI16();
		depth		    = inStream.readUI16();
	}
}

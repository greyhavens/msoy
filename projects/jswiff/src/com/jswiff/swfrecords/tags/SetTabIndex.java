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
 * This tag is used to set the tab order index of character instances. This
 * index determines the order in which character instances receive input focus
 * when repeatedly pressing the TAB key (aka 'tab order'). It also affects the
 * access order (aka 'reading order') when using screen readers.
 *
 * @since SWF 7
 */
public final class SetTabIndex extends Tag {
	private int depth;
	private int tabIndex;

	/**
	 * Creates a new SetTabIndex tag. Provide the depth of the character
	 * instance and its tab order index.
	 *
	 * @param depth depth the character instance is placed at
	 * @param tabIndex tab order index (up to 65535)
	 */
	public SetTabIndex(int depth, int tabIndex) {
		code			  = TagConstants.SET_TAB_INDEX;
		this.depth		  = depth;
		this.tabIndex     = tabIndex;
	}

	SetTabIndex() {
		// empty
	}

	/**
	 * Sets the depth the character instance is placed at.
	 *
	 * @param depth placement depth
	 */
	public void setDepth(int depth) {
		this.depth = depth;
	}

	/**
	 * Returns the depth the character instance is placed at.
	 *
	 * @return placement depth
	 */
	public int getDepth() {
		return depth;
	}

	/**
	 * Sets the tab order index of the character instance.
	 *
	 * @param tabIndex tab order index
	 */
	public void setTabIndex(int tabIndex) {
		this.tabIndex = tabIndex;
	}

	/**
	 * Returns the tab order index of the character instance.
	 *
	 * @return tab order index
	 */
	public int getTabIndex() {
		return tabIndex;
	}

	protected void writeData(OutputBitStream outStream)
		throws IOException {
		outStream.writeUI16(depth);
		outStream.writeUI16(tabIndex);
	}

	void setData(byte[] data) throws IOException {
		InputBitStream inStream = new InputBitStream(data);
		depth		 = inStream.readUI16();
		tabIndex     = inStream.readUI16();
	}
}

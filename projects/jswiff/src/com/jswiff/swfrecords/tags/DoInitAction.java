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
import com.jswiff.swfrecords.actions.Action;
import com.jswiff.swfrecords.actions.ActionBlock;

import java.io.IOException;


/**
 * <p>
 * This tag contains a series of initialization actions for a particular
 * sprite. These actions are executed only once, before the first instatiation
 * of the sprite. Typically used for class definitions.
 * </p>
 * 
 * <p>
 * This tag is used to implement the <code>#initclip</code> ActionScript
 * compiler directive.
 * </p>
 *
 * @since SWF 6
 */
public final class DoInitAction extends Tag {
	private int spriteId;
	private ActionBlock initActions;

	/**
	 * Creates a new DoInitAction tag. Supply the character ID of the sprite
	 * the initialization actions apply to. After creation, use
	 * <code>addAction()</code> to add actions to the contained action block.
	 *
	 * @param spriteId character ID of sprite to be initialized
	 */
	public DoInitAction(int spriteId) {
		code			  = TagConstants.DO_INIT_ACTION;
		this.spriteId     = spriteId;
		initActions		  = new ActionBlock();
	}

	DoInitAction() {
		// empty
	}

	/**
	 * Returns the action block containing the initialization action records.
	 * Use <code>addAction()</code> to add an action record to this block.
	 *
	 * @return initialization action block
	 */
	public ActionBlock getInitActions() {
		return initActions;
	}

	/**
	 * Sets the character ID of the sprite the initialization actions apply to.
	 *
	 * @param spriteId sprite's character ID
	 */
	public void setSpriteId(int spriteId) {
		this.spriteId = spriteId;
	}

	/**
	 * Returns the character ID of the sprite the initialization actions apply
	 * to.
	 *
	 * @return sprite's character ID
	 */
	public int getSpriteId() {
		return spriteId;
	}

	/**
	 * Adds an initialization action record.
	 *
	 * @param action init action record
	 */
	public void addAction(Action action) {
		initActions.addAction(action);
	}

	protected void writeData(OutputBitStream outStream)
		throws IOException {
		outStream.writeUI16(spriteId);
		initActions.write(outStream, true);
	}

	void setData(byte[] data) throws IOException {
		InputBitStream inStream = new InputBitStream(data);
    if (getSWFVersion() < 6) {
      if (isJapanese()) {
        inStream.setShiftJIS(true);
      } else {
        inStream.setANSI(true);
      }
    }
		spriteId	    = inStream.readUI16();
		initActions     = new ActionBlock(inStream);
	}
}

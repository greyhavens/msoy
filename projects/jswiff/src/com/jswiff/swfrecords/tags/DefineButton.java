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
import com.jswiff.swfrecords.ButtonRecord;
import com.jswiff.swfrecords.actions.ActionBlock;

import java.io.IOException;

import java.util.Vector;


/**
 * <p>
 * This tag defines a button character. It contains an array of at least one
 * <code>ButtonRecord</code> instance in order to define the button's
 * appearance depending on it's state. See <code>ButtonRecord</code> for
 * details on button states.
 * </p>
 * 
 * <p>
 * DefineButton also includes an action block which contains actions performed
 * when when the button is clicked and released.
 * </p>
 *
 * @see ButtonRecord
 * @since SWF 1
 */
public final class DefineButton extends DefinitionTag {
	private ButtonRecord[] characters;
	private ActionBlock actionBlock;

	/**
	 * Creates a new DefineButton tag.
	 *
	 * @param characterId the button's character ID
	 * @param characters array of button records
	 *
	 * @throws IllegalArgumentException if button record array is
	 * 		   <code>null</code> or empty
	 */
	public DefineButton(int characterId, ButtonRecord[] characters) {
		code = TagConstants.DEFINE_BUTTON;
		if ((characters == null) || (characters.length == 0)) {
			throw new IllegalArgumentException(
				"At least one button record is needed!");
		}
		this.characterId     = characterId;
		this.characters		 = characters;
	}

	DefineButton() {
		// empty
	}

	/**
	 * Returns the button's action block. Can be used to add new actions.
	 *
	 * @return the action block assigned to the button
	 */
	public ActionBlock getActions() {
		if (actionBlock == null) {
			actionBlock = new ActionBlock(); // lazy init
		}
		return actionBlock;
	}

	/**
	 * Sets an array of at least one <code>ButtonRecord</code> instance
	 * defining the appearance of the button depending on it's state.
	 *
	 * @param characters button records
	 */
	public void setCharacters(ButtonRecord[] characters) {
		this.characters = characters;
	}

	/**
	 * Returns an array of at least one <code>ButtonRecord</code> instance
	 * defining the appearance of the button depending on it's state.
	 *
	 * @return button records
	 */
	public ButtonRecord[] getCharacters() {
		return characters;
	}

	protected void writeData(OutputBitStream outStream)
		throws IOException {
		outStream.writeUI16(characterId);
		for (int i = 0; i < characters.length; i++) {
			characters[i].write(outStream, false);
		}
		outStream.writeUI8((short) 0); // CharacterEndFlag
		getActions().write(outStream, true);
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
		characterId = inStream.readUI16();
		Vector buttonRecords = new Vector();
		do {
			// check next byte without using stream (to be able to read it again if != 0)
			if (data[(int) inStream.getOffset()] == 0) {
				inStream.readUI8(); // ignore CharacterEndFlag
				break;
			}
			buttonRecords.add(new ButtonRecord(inStream, false));
		} while (true);
		characters = new ButtonRecord[buttonRecords.size()];
		buttonRecords.copyInto(characters);
		actionBlock = new ActionBlock(inStream);
	}
}

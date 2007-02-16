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

package com.jswiff.swfrecords.tags;

import com.jswiff.io.InputBitStream;
import com.jswiff.io.OutputBitStream;
import com.jswiff.swfrecords.ButtonCondAction;
import com.jswiff.swfrecords.ButtonRecord;

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
 * DefineButton2 allows actions to be triggered by any state transition. See
 * <code>ButtonCondAction</code> for details on button state transitions.
 * </p>
 *
 * @see ButtonRecord
 * @see ButtonCondAction
 * @since SWF 3
 */
public final class DefineButton2 extends DefinitionTag {
  private boolean trackAsMenu;
  private ButtonRecord[] characters;
  private ButtonCondAction[] actions;

  /**
   * Creates a new DefineButton2 tag.
   *
   * @param characterId the button's character ID
   * @param characters array of button records
   * @param trackAsMenu if <code>true</code>, button can be influenced by
   *        events started on other buttons
   */
  public DefineButton2(
    int characterId, ButtonRecord[] characters, boolean trackAsMenu) {
    code               = TagConstants.DEFINE_BUTTON_2;
    this.characterId   = characterId;
    this.characters    = characters;
    this.trackAsMenu   = trackAsMenu;
  }

  DefineButton2() {
    // empty
  }

  /**
   * Sets an array of <code>ButtonCondAction</code> instaces which define the
   * button's behavior.
   *
   * @param actions <code>ButtonCondAction</code> array
   */
  public void setActions(ButtonCondAction[] actions) {
    this.actions = actions;
  }

  /**
   * Returns an array of <code>ButtonCondAction</code> instaces which define
   * the button's behavior.
   *
   * @return <code>ButtonCondAction</code> array
   */
  public ButtonCondAction[] getActions() {
    return actions;
  }

  /**
   * Sets an array of at least one <code>ButtonRecord</code> instance defining
   * the appearance of the button depending on it's state.
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

  /**
   * Specifies whether the button is tracked as menu or as conventional button
   * (i.e. if the button's events are affected by events started on other
   * buttons or not).
   *
   * @param trackAsMenu <code>true</code> if tracked as menu button, otherwise
   *        false
   */
  public void setTrackAsMenu(boolean trackAsMenu) {
    this.trackAsMenu = trackAsMenu;
  }

  /**
   * Checks if the button is tracked as menu or as conventional button (i.e. if
   * the button's events are affected by events started on other buttons or
   * not).
   *
   * @return <code>true</code> if tracked as menu button, otherwise false
   */
  public boolean isTrackAsMenu() {
    return trackAsMenu;
  }

  protected void writeData(OutputBitStream outStream) throws IOException {
    forceLongHeader = true;
    outStream.writeUI16(characterId);
    outStream.writeUnsignedBits(0, 7); // 7 reserved bits
    outStream.writeBooleanBit(trackAsMenu);
    if ((actions == null) || (actions.length == 0)) {
      outStream.writeUI16(0); // ActionOffset = 0
      for (int i = 0; i < characters.length; i++) {
        characters[i].write(outStream, true);
      }
      outStream.writeUI8((short) 0); // CharacterEndFlag
    } else {
      OutputBitStream charStream = new OutputBitStream();
      for (int i = 0; i < characters.length; i++) {
        characters[i].write(charStream, true);
      }
      charStream.writeUI8((short) 0); // CharacterEndFlag
      byte[] charStreamBuffer = charStream.getData();
      outStream.writeUI16(charStreamBuffer.length + 2); // ActionOffset
      outStream.writeBytes(charStreamBuffer); // characters and CharacterEndFlag
      // write actions
      for (int i = 0; i < actions.length; i++) {
        // first write to bit stream to compute condActionSize
        OutputBitStream bitStream = new OutputBitStream();
        bitStream.setANSI(outStream.isANSI());
        bitStream.setShiftJIS(outStream.isShiftJIS());
        actions[i].write(bitStream);
        byte[] bitStreamData = bitStream.getData();
        if (i < (actions.length - 1)) {
          outStream.writeUI16(bitStreamData.length + 2); // condActionSize
        } else {
          // last entry - write 0 as offset
          outStream.writeUI16(0); // last action, condActionSize = 0
        }
        outStream.writeBytes(bitStreamData); // write action
      }

      // outStream.writeUI8((short) 0); // ActionEndFlag
    }
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
    characterId   = inStream.readUI16();
    trackAsMenu   = ((inStream.readUI8() & 1) != 0); // ignore upper 7 bits
    int actionOffset = inStream.readUI16();

    // read ButtonRecord array into characters
    Vector buttonRecords = new Vector();
    long startOffset = inStream.getOffset();
    do {
      // some stupid programs sometimes don't write any button records
      long remainingBytes = data.length - inStream.getOffset();
      if ((actionOffset == 0) && (remainingBytes == 1)) {
        // no actions, CharacterEndFlag follows
        break;
      } else if ((inStream.getOffset() - startOffset) == (actionOffset - 3)) {
        // actionOffset reached, CharacterEndFlag and actions follow
        break;
      } else if (remainingBytes < 6) {
        // other errors, e.g. actionOffset != 0 and stream ends etc.
        break;
      }
      buttonRecords.add(new ButtonRecord(inStream, true));
    } while (true);
    inStream.readUI8(); // ignore CharacterEndFlag
    characters = new ButtonRecord[buttonRecords.size()];
    buttonRecords.copyInto(characters);
    if (actionOffset == 0) {
      return;
    }

    // read ButtonCondAction array into actions
    Vector buttonCondActions = new Vector();
    int condActionSize       = -1;
    do {
      condActionSize = inStream.readUI16();
      buttonCondActions.add(new ButtonCondAction(inStream));
    } while (condActionSize != 0);
    actions = new ButtonCondAction[buttonCondActions.size()];
    buttonCondActions.copyInto(actions);
  }
}

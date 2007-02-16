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
 * This tag instructs Flash Player to execute a series of actions when
 * encountering the next <code>ShowFrame</code> tag, after all drawing for the
 * current frame has completed.
 *
 * @since SWF 3
 */
public final class DoAction extends Tag {
  private ActionBlock actions;

  /**
   * Creates a new DoAction instance.
   */
  public DoAction() {
    code = TagConstants.DO_ACTION;
  }

  /**
   * Returns the action block containing actions this tag is supposed to
   * perform. Use <code>addAction()</code> to add action records to this
   * block.
   *
   * @return contained actions
   */
  public ActionBlock getActions() {
    if (actions == null) {
      actions = new ActionBlock();
    }
    return actions;
  }

  /**
   * Adds an action record.
   *
   * @param action action record
   */
  public void addAction(Action action) {
    getActions().addAction(action);
  }

  protected void writeData(OutputBitStream outStream) throws IOException {
    forceLongHeader = true;
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
    actions = new ActionBlock(inStream);
  }
}

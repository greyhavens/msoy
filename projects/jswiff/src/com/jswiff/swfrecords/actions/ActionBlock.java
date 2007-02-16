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

package com.jswiff.swfrecords.actions;

import com.jswiff.io.InputBitStream;
import com.jswiff.io.OutputBitStream;

import java.io.IOException;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * <p>
 * This class implements a container for action records. It is used in actions
 * which contain other actions (e.g. <code>DefineFunction</code> or
 * <code>With</code>).
 * </p>
 * 
 * <p>
 * Build nested action blocks bottom-up, i.e. the inner blocks first. For
 * example if you have a <code>With</code>action inside a
 * <code>DefineFunction2</code> action block, first add actions to the action
 * block of <code>With</code>, then add <code>With</code> to the action block
 * of <code>DefineFunction2</code>. Finally, add <code>DefineFunction2</code>
 * to the top level action block.
 * </p>
 */
public final class ActionBlock implements Serializable {
  /** Label name pointing to the end of the current action block. */
  public static String LABEL_END = "__end";
  /**
   * Label name pointing outside the block (usually an error). Use this only
   * for error checking!
   */
  public static String LABEL_OUT = "__out";
  private static int instCounter = 0; // instance counter used for labels
  private List actions           = new ArrayList();
  private Map labelMap           = new HashMap();
  private Map inverseLabelMap    = new HashMap();

  /**
   * Creates a new Block action.
   */
  public ActionBlock() {
    // nothing to do
  }

  /**
   * Reads an action block from a bit stream.
   *
   * @param stream the source bit stream
   *
   * @throws IOException if an I/O error has occured
   */
  public ActionBlock(InputBitStream stream) throws IOException {
    int startOffset      = (int) stream.getOffset();
    boolean hasEndAction = false;
    while (stream.available() > 0) {
      Action record = ActionReader.readRecord(stream);
      if (record.code != ActionConstants.END) {
        actions.add(record);
      } else {
        hasEndAction = true;
        break;
      }
    }
    if (actions.size() == 0) {
      return;
    }

    // end offset (relative to start offset, end action ignored)
    int relativeEndOffset = (int) stream.getOffset() - startOffset -
      (hasEndAction ? 1 : 0);

    // correct offsets, setting to relative to first action (not to start of stream)
    // also, populate the label map with integers containing the corresponding offsets
    int labelCounter      = 0;
    Map actionMap         = new HashMap(); // contains  offset->action  mapping
    for (int i = 0; i < actions.size(); i++) {
      Action action = (Action) actions.get(i);
      int newOffset = action.getOffset() - startOffset;
      action.setOffset(newOffset);
      actionMap.put(new Integer(newOffset), action);
      // collect labels from Jump and If actions
      if (
        (action.getCode() == ActionConstants.IF) ||
            (action.getCode() == ActionConstants.JUMP)) {
        Branch branchAction = (Branch) action;

        // temporarily put the offset into the label map
        // later on, the offset will be replaced with the corresponding action instance
        int branchOffset    = getBranchOffset(branchAction);
        String branchLabel;
        if (branchOffset < relativeEndOffset) {
          Integer branchOffsetObj = new Integer(branchOffset);

          // check if branch target isn't already assigned a label
          String oldLabel         = (String) inverseLabelMap.get(
              branchOffsetObj);
          if (oldLabel == null) {
            branchLabel = "L_" + instCounter + "_" + labelCounter++;
            labelMap.put(branchLabel, branchOffsetObj);
            inverseLabelMap.put(branchOffsetObj, branchLabel);
          } else {
            branchLabel = oldLabel;
          }
        } else if (branchOffset == relativeEndOffset) {
          branchLabel = LABEL_END;
        } else {
          branchLabel = LABEL_OUT;
        }
        branchAction.setBranchLabel(branchLabel);
      }
    }

    // now replace offsets from label map with corresponding actions
    Set keys = labelMap.keySet();
    for (Iterator i = keys.iterator(); i.hasNext();) {
      String label        = (String) i.next();
      Object branchOffset = labelMap.get(label);
      Action action       = (Action) actionMap.get(branchOffset);
      if (action != null) {
        // action == null when label == LABEL_OUT
        action.setLabel(label);
        labelMap.put(label, action);
      }
    }
    instCounter++;
  }

  /**
   * Resets the instance counter. This counter is used to create action labels
   * when parsing an SWF file.
   */
  public static void resetInstanceCounter() {
    instCounter = 0;
  }

  /**
   * Returns a list of the contained action records. Warning: use this list in
   * a read-only manner!
   *
   * @return contained actions in a list
   */
  public List getActions() {
    return actions;
  }

  /**
   * Returns the size of the action block in bytes, i.e. the sum of the size of
   * the contained action records.
   *
   * @return size of block in bytes
   */
  public int getSize() {
    int size = 0;
    for (Iterator i = actions.iterator(); i.hasNext();) {
      size += ((Action) i.next()).getSize();
    }
    return size;
  }

  /**
   * Adds an action record to this action block.
   *
   * @param action an action record
   */
  public void addAction(Action action) {
    // add action to list
    actions.add(action);
  }

  /**
   * Removes the specified action record from the action block.
   *
   * @param action action record to be removed
   *
   * @return <code>true</code> if action block contained the specified action
   *         record
   */
  public boolean removeAction(Action action) {
    return actions.remove(action);
  }

  /**
   * Removes the action record at the specified position within the block.
   *
   * @param index index of the action record to be removed
   *
   * @return the action record previously contained at specified position
   */
  public Action removeAction(int index) {
    return (Action) actions.remove(index);
  }

  /**
   * Writes the action block to a bit stream.
   *
   * @param stream the target bit stream
   * @param writeEndAction if <code>true</code>, an END action is written at
   *        the end of the block
   *
   * @throws IOException if an I/O error has occured
   */
  public void write(OutputBitStream stream, boolean writeEndAction)
    throws IOException {
    // two passes
    // first pass: correct offsets and populate labelMap
    int currentOffset = 0;
    for (Iterator i = actions.iterator(); i.hasNext();) {
      Action action = (Action) i.next();
      action.setOffset(currentOffset);
      currentOffset += action.getSize();
      // if action has label, add (label->action) mapping to labelMap
      String label = action.getLabel();
      if (label != null) {
        labelMap.put(label, action);
      }
    }

    // second pass: replace branch labels with branch offsets and write actions
    for (Iterator i = actions.iterator(); i.hasNext();) {
      Action action = (Action) i.next();
      switch (action.getCode()) {
        case ActionConstants.JUMP:
        case ActionConstants.IF:
          replaceBranchLabelWithRelOffset((Branch) action); // replace branch label with offset relative to subsequent action
          break;
      }
      action.write(stream);
    }

    // now write END action if needed
    if (writeEndAction) {
      stream.writeUI8((short) 0); // ActionEndFlag
    }
  }

  /*
   * Returns the action corresponding to a specific label. Labels are used for
   * jumps within this action block.
   */
  private Action getAction(String label) {
    Object action = labelMap.get(label);
    if (action instanceof Action) {
      return (Action) action;
    }
    throw new IllegalArgumentException(
      "Label '" + label + "' points at non-existent action!");
  }

  /*
   * Returns the absolute branch offset of an action.
   */
  private int getBranchOffset(Branch action) {
    int branchOffset = 0;
    branchOffset = action.getBranchOffset();
    // convert from relative to absolute offset 
    branchOffset += (action.getOffset() + action.getSize());
    return branchOffset;
  }

  /*
   * Returns the absolute offset corresponding to a given label.
   */
  private int getOffset(String label) {
    if (label.equals(LABEL_END)) {
      return getSize();
    }
    Action action = getAction(label);
    if (action == null) {
      throw new IllegalArgumentException("Label " + label + " not defined!");
    }
    return action.getOffset();
  }

  private void replaceBranchLabelWithRelOffset(Branch action) {
    // replace branch label with offset relative to subsequent action
    // get absolute offset corresponding to branch label
    short branchOffset = (short) getOffset(action.getBranchLabel());

    // compute offset relative to subsequent action
    branchOffset -= (action.getOffset() + action.getSize());
    // set branch offset
    action.setBranchOffset(branchOffset);
  }
}

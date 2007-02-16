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

package com.jswiff.investigator;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.UIManager;
import javax.swing.WindowConstants;


/*
 * Implements a progress dialog to tell the user what's happening
 * (inspired by James Gosling's ProgressMonitor class).
 */
final class ProgressDialog {
  private JDialog myDialog;
  private JOptionPane pane;
  private JProgressBar myBar;
  private JLabel noteLabel;
  private JLabel messageLabel;
  private Component myParentComponent;
  private String note;
  private String myTitle;
  private String message;
  Object[] selectionOptions;
  private int minValue;
  private int maxValue;
  private int lastDisp;
  private int reportDelta;
  boolean hasCancelButton;

  ProgressDialog(
    Component parentComponent, String title, String message, String note,
    int minValue, int maxValue, boolean hasCancelButton) {
    this.minValue            = minValue;
    this.maxValue            = maxValue;
    this.myParentComponent   = parentComponent;
    if (hasCancelButton) {
      selectionOptions      = new Object[1];
      selectionOptions[0]   = UIManager.getString(
          "OptionPane.cancelButtonText");
    } else {
      selectionOptions = new Object[0];
    }
    reportDelta = (maxValue - minValue) / 100;
    if (reportDelta < 1) {
      reportDelta = 1;
    }
    this.myTitle           = title;
    this.message           = message;
    this.note              = note;
    this.hasCancelButton   = hasCancelButton;
  }

  boolean isCanceled() {
    if (pane == null) {
      return false;
    }
    Object paneValue = pane.getValue();
    return ((paneValue != null) && (selectionOptions.length == 1) &&
    (paneValue.equals(selectionOptions[0])));
  }

  void setMessage(String message) {
    this.message = message;
    if (messageLabel != null) {
      messageLabel.setText(message);
    }
  }

  void setNote(String note) {
    this.note = note;
    if (noteLabel != null) {
      noteLabel.setText(note);
    }
  }

  void setProgressValue(int newValue) {
    if (myBar == null) {
      displayWindow();
    }
    if (newValue >= maxValue) {
      close();
    } else if (newValue >= (lastDisp + reportDelta)) {
      lastDisp = newValue;
      myBar.setValue(newValue);
    }
  }

  void close() {
    if (myDialog != null) {
      myDialog.setVisible(false);
      myDialog.dispose();
      myDialog   = null;
      pane       = null;
      myBar      = null;
    }
  }

  private void displayWindow() {
    myBar = new JProgressBar();
    myBar.setMinimum(minValue);
    myBar.setMaximum(maxValue);
    if (note != null) {
      noteLabel      = new JLabel(note);
      messageLabel   = new JLabel(message);
    }
    pane       = new ProgressOptionPane(
        new Object[] { messageLabel, noteLabel, myBar });
    myDialog   = pane.createDialog(myParentComponent, myTitle);
    myDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    myDialog.show();
  }

  private class ProgressOptionPane extends JOptionPane {
    ProgressOptionPane(Object messageList) {
      super(
        messageList, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null,
        ProgressDialog.this.selectionOptions, null);
    }

    public JDialog createDialog(Component parentComponent, String title) {
      Frame frame          = JOptionPane.getFrameForComponent(parentComponent);
      final JDialog dialog = new JDialog(frame, title, false);
      dialog.setResizable(false);
      Container contentPane = dialog.getContentPane();
      contentPane.add(this, BorderLayout.CENTER);
      dialog.pack();
      dialog.setSize(
        (int) (dialog.getSize().width * 1.5), dialog.getSize().height);
      dialog.setLocationRelativeTo(parentComponent);
      dialog.addWindowListener(
        new WindowAdapter() {
          boolean gotFocus = false;

          public void windowClosing(WindowEvent we) {
            if (hasCancelButton) {
              setValue(selectionOptions[0]);
            }
          }

          public void windowActivated(WindowEvent we) {
            if (!gotFocus) {
              selectInitialValue();
              gotFocus = true;
            }
          }
        });
      addPropertyChangeListener(
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent event) {
            if (
              dialog.isVisible() &&
                  (event.getSource() == ProgressOptionPane.this) &&
                  (event.getPropertyName().equals(VALUE_PROPERTY) ||
                  event.getPropertyName().equals(INPUT_VALUE_PROPERTY))) {
              dialog.setVisible(false);
              dialog.dispose();
            }
          }
        });
      return dialog;
    }
  }
}

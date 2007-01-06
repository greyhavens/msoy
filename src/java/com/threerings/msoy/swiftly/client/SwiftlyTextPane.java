package com.threerings.msoy.swiftly.client;

import java.awt.Container;
import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

public class SwiftlyTextPane extends JTextPane
{
    public SwiftlyTextPane (SwiftlyEditor editor)
    {
        _editor = editor;
        keyBindings();
    }

    protected void keyBindings ()
    {
        // TODO getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW); vs. WHEN_FOCUSED
        InputMap inputMap = getInputMap();
        ActionMap actionMap = getActionMap();

        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_W, Event.CTRL_MASK);
        Action action = new CloseTabAction();
        inputMap.put(key, action);
        actionMap.put(action, action);
    }

    protected class UndoHandler implements UndoableEditListener
    {
        public void undoableEditHappened (UndoableEditEvent e)
        {
            _undo.addEdit(e.getEdit());
            undoAction.update();
            redoAction.update();
        }
    }

    protected class UndoAction extends AbstractAction
    {
        public UndoAction () {
            super("Undo");
            setEnabled(false);
        }

        public void actionPerformed (ActionEvent e)
        {
            try {
                _undo.undo();
            } catch (CannotUndoException ex) {
                // TODO this should probably display in the statusbar
                System.out.println("Unable to undo: " + ex);
            }
            update();
            redoAction.update();
        }

        protected void update ()
        {
            if(_undo.canUndo()) {
                setEnabled(true);
                putValue(Action.NAME, _undo.getUndoPresentationName());
            }
            else {
                setEnabled(false);
                putValue(Action.NAME, "Undo");
            }
        }
    }

    protected class RedoAction extends AbstractAction
    {
        public RedoAction ()
        {
            super("Redo");
            setEnabled(false);
        }

        public void actionPerformed (ActionEvent e)
        {
            try {
                _undo.redo();
            } catch (CannotRedoException ex) {
                // TODO this should probably display in the statusbar
                System.out.println("Unable to redo: " + ex);
            }
            update();
            undoAction.update();
        }

        protected void update ()
        {
            if(_undo.canRedo()) {
                setEnabled(true);
                putValue(Action.NAME, _undo.getRedoPresentationName());
            }
            else {
                setEnabled(false);
                putValue(Action.NAME, "Redo");
            }
        }
    }

    protected class CloseTabAction extends AbstractAction
    {
        public void actionPerformed (ActionEvent e) {
            _editor.closeEditorTab(getPage().toString());
        }
    }
    
    protected UndoManager _undo = new UndoManager();
    protected UndoableEditListener undoHandler = new UndoHandler();
    protected UndoAction undoAction = new UndoAction();
    protected RedoAction redoAction = new RedoAction();
    protected SwiftlyEditor _editor;
}


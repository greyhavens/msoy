package com.threerings.msoy.swiftly.client;

import java.awt.Container;
import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

public class SwiftlyTextPane extends JTextPane
{
    public SwiftlyTextPane (SwiftlyEditor editor, SwiftlyDocument document)
    {
        _editor = editor;
        _document = document;
        keyBindings();
    }

    protected void keyBindings ()
    {
        // ctrl-w closes the tab
        addKeyAction(new CloseTabAction(), KeyStroke.getKeyStroke(KeyEvent.VK_W, Event.CTRL_MASK));

        // ctrl-z undos the action
        addKeyAction(new UndoAction(), KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK));
    }

    protected void addKeyAction (Action action, KeyStroke key)
    {
        // TODO getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW); vs. WHEN_FOCUSED
        getInputMap().put(key, action);
        getActionMap().put(action, action);
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
            _editor.closeEditorTab(_document);
        }
    }
    
    protected UndoManager _undo = new UndoManager();
    protected UndoableEditListener undoHandler = new UndoHandler();
    protected UndoAction undoAction = new UndoAction();
    protected RedoAction redoAction = new RedoAction();
    protected SwiftlyEditor _editor;
    protected SwiftlyDocument _document;
}


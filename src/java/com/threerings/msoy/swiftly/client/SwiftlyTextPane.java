package com.threerings.msoy.swiftly.client;

import java.awt.Color;
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
import javax.swing.text.StyledDocument;
import javax.swing.text.BadLocationException;

public class SwiftlyTextPane extends JTextPane
{
    public SwiftlyTextPane (SwiftlyEditor editor, SwiftlyDocument document)
    {
        _editor = editor;
        _document = document;
        keyBindings();

        // load the document text and undo listener
        StyledDocument styledDoc = getStyledDocument();
        try {
            styledDoc.insertString(0, document.getText(), null);
        } catch (BadLocationException e) {
        }
        styledDoc.addUndoableEditListener(new UndoHandler());

        // setup some default colors
        // TODO make setable by the user?
        setForeground(Color.white);
        setBackground(Color.black);
        setCaretColor(Color.white);
    }

    protected void keyBindings ()
    {
        // ctrl-w closes the tab
        addKeyAction(new CloseTabAction(), KeyStroke.getKeyStroke(KeyEvent.VK_W, Event.CTRL_MASK));

        // ctrl-z undos the action
        addKeyAction(_undoAction, KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK));

        // ctrl-y redoes the action
        addKeyAction(_redoAction, KeyStroke.getKeyStroke(KeyEvent.VK_Y, Event.CTRL_MASK));
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
            _undoAction.update();
            _redoAction.update();
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
                _editor.getApplet().setStatus("Unable to undo.");
            }
            update();
            _redoAction.update();
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
                _editor.getApplet().setStatus("Unable to redo.");
            }
            update();
            _undoAction.update();
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
    protected UndoableEditListener _undoHandler = new UndoHandler();
    protected UndoAction _undoAction = new UndoAction();
    protected RedoAction _redoAction = new RedoAction();
    protected SwiftlyEditor _editor;
    protected SwiftlyDocument _document;
}


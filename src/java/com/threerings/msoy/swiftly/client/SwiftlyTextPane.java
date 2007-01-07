package com.threerings.msoy.swiftly.client;

import java.awt.Color;
import java.awt.Container;
import java.awt.Event;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;

import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.StyledDocument;
import javax.swing.text.BadLocationException;

public class SwiftlyTextPane extends JTextPane
{
    public SwiftlyTextPane (SwiftlyEditor editor, SwiftlyDocument document)
    {
        _editor = editor;
        _document = document;
        createActionTable();
        addKeyBindings();
        addPopupMenu();

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

    protected void addKeyBindings ()
    {
        // ctrl-w closes the tab
        addKeyAction(new CloseTabAction(), KeyStroke.getKeyStroke(KeyEvent.VK_W, Event.CTRL_MASK));

        // ctrl-z undos the action
        addKeyAction(_undoAction, KeyStroke.getKeyStroke(KeyEvent.VK_Z, Event.CTRL_MASK));

        // ctrl-y redoes the action
        addKeyAction(_redoAction, KeyStroke.getKeyStroke(KeyEvent.VK_Y, Event.CTRL_MASK));
    }

    protected void addPopupMenu ()
    {
        _popup = new JPopupMenu();

        // TODO is there a cross platform way to show what the keybindings are for these actions?
        // Cut
        JMenuItem menuItem = new JMenuItem("Cut");
        menuItem.addActionListener(getActionByName(DefaultEditorKit.cutAction));
        _popup.add(menuItem);
        // Copy
        menuItem = new JMenuItem("Copy");
        menuItem.addActionListener(getActionByName(DefaultEditorKit.copyAction));
        _popup.add(menuItem);
        // Paste
        menuItem = new JMenuItem("Paste");
        menuItem.addActionListener(getActionByName(DefaultEditorKit.pasteAction));
        _popup.add(menuItem);
        // Seperator
        _popup.addSeparator();
        // Select All
        menuItem = new JMenuItem("Select All");
        menuItem.addActionListener(getActionByName(DefaultEditorKit.selectAllAction));
        _popup.add(menuItem);
        // Seperator
        _popup.addSeparator();
        // Undo
        menuItem = new JMenuItem("Undo");
        menuItem.addActionListener(_undoAction);
        _popup.add(menuItem);
        // Redo
        menuItem = new JMenuItem("Redo");
        menuItem.addActionListener(_redoAction);
        _popup.add(menuItem);

        MouseListener popupListener = new PopupListener();
        // add popupListener to the textpane
        addMouseListener(popupListener);
    }

    protected void addKeyAction (Action action, KeyStroke key)
    {
        // TODO getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW); vs. WHEN_FOCUSED
        getInputMap().put(key, action);
        getActionMap().put(action, action);
    }

    // Create the action lookup table
    protected void createActionTable() {
        for (Action action : getStyledEditorKit().getActions()) {
            _actions.put((String)action.getValue(Action.NAME), action);
        }
    }

    // Lookup an action by name
    protected Action getActionByName(String name) {
        return (Action)(_actions.get(name));
    }

    protected class PopupListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                _popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
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
    
    protected JPopupMenu _popup;
    protected UndoManager _undo = new UndoManager();
    protected UndoableEditListener _undoHandler = new UndoHandler();
    protected UndoAction _undoAction = new UndoAction();
    protected RedoAction _redoAction = new RedoAction();
    protected SwiftlyEditor _editor;
    protected SwiftlyDocument _document;
    protected HashMap<String,Action> _actions = new HashMap<String,Action>();
}


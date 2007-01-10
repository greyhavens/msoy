package com.threerings.msoy.swiftly.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Event;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;

import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;

public class SwiftlyTextPane extends JTextPane
{
    public SwiftlyTextPane (SwiftlyEditor editor, SwiftlyDocument document)
    {
        _editor = editor;
        _document = document;

        _kit = new ActionScriptEditorKit();
        setEditorKit(_kit);
        ActionScriptStyledDocument styledDoc = new ActionScriptStyledDocument();
        setDocument(styledDoc);

        // setContentType("text/actionscript");

        // load the document text
        try {
            // styledDoc.insertString(0, document.getText(), null);
            _kit.read(document.getReader(), styledDoc, 0);
        } catch (IOException io) {
            return;
        } catch (BadLocationException be) {
            return;
        }

        addKeyBindings();
        addPopupMenu();

        // add listeners
        styledDoc.addUndoableEditListener(new UndoHandler());
        styledDoc.addDocumentListener(new SwiftlyDocumentListener());

        // setup some default colors
        // TODO make setable by the user?
        setForeground(Color.black);
        setBackground(Color.white);
    }

    public SwiftlyDocument getSwiftlyDocument ()
    {
        return _document;
    }

    // Save the document if needed. Return true if save happened/worked
    public boolean saveDocument ()
    {
        if (_documentChanged) {
            // TODO save the document into the internets
            _documentChanged = false;
            return true;
        }
        return false;
    }

    public boolean hasUnsavedChanges ()
    {
        return _documentChanged;
    }

    public AbstractAction getUndoAction ()
    {
        return _undoAction;
    }

    public AbstractAction getRedoAction ()
    {
        return _redoAction;
    }

    public Action createCutAction ()
    {
        return new AbstractAction("Cut") {
            public void actionPerformed (ActionEvent e) {
                cut();
            }
        };
    }

    public Action createCopyAction ()
    {
        return new AbstractAction("Copy") {
            public void actionPerformed (ActionEvent e) {
                copy();
            }
        };
    }

    public Action createPasteAction ()
    {
        return new AbstractAction("Paste") {
            public void actionPerformed (ActionEvent e) {
                paste();
            }
        };
    }

    // TODO select all when called from the toolbar isn't showing the selected text, despite
    // having actually selected it. Fix this.
    public Action createSelectAllAction ()
    {
        return new AbstractAction("Select All") {
            public void actionPerformed (ActionEvent e) {
                selectAll();
            }
        };
    }

    protected void addKeyBindings ()
    {
        // ctrl-n opens a new tab
        addKeyAction(_editor.createNewTabAction(),
                     KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.CTRL_MASK));

        // ctrl-s saves the current document
        addKeyAction(_editor.createSaveCurrentTabAction(),
                     KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK));

        // ctrl-w closes the tab
        addKeyAction(_editor.createCloseCurrentTabAction(),
                     KeyStroke.getKeyStroke(KeyEvent.VK_W, Event.CTRL_MASK));

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
        _popup.add(createMenuItem("Cut", createCutAction()));
        // Copy
        _popup.add(createMenuItem("Copy", createCopyAction()));
        // Paste
        _popup.add(createMenuItem("Paste", createPasteAction()));
        // Separator
        _popup.addSeparator();
        // Select All
        _popup.add(createMenuItem("Select All", createSelectAllAction()));
        // Separator
        _popup.addSeparator();
        // Undo
        _popup.add(_undoAction);
        // Redo
        _popup.add(_redoAction);

        MouseListener popupListener = new PopupListener();
        // add popupListener to the textpane
        addMouseListener(popupListener);
    }

    protected JMenuItem createMenuItem (String text, Action action)
    {
        JMenuItem item = new JMenuItem(action);
        item.setText(text);
        return item;
    }

    protected void addKeyAction (Action action, KeyStroke key)
    {
        // key bindings work even if the textpane doesn't have focus
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, action);
        getActionMap().put(action, action);
    }

    protected class PopupListener extends MouseAdapter {
        @Override // from MouseAdapter
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        @Override // from MouseAdapter
        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        protected void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                _popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    protected class UndoHandler implements UndoableEditListener
    {
        // from interface UndoableEditListener
        public void undoableEditHappened (UndoableEditEvent e)
        {
            if (e.getEdit().getPresentationName().equals("style change")) {
                return;
            }
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

        // from AbstractAction
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
                // We have undone to the previous save
                if (_documentChanged) {
                    _documentChanged = false;
                    _editor.setTabTitleChanged(false);
                }
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

        // from AbstractAction
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

    class SwiftlyDocumentListener implements DocumentListener {
        // from interface DocumentListener
        public void insertUpdate(DocumentEvent e) {
            if (!_documentChanged) {
                _editor.setTabTitleChanged(true);
                _documentChanged = true;
            }
        }

        // from interface DocumentListener
        public void removeUpdate(DocumentEvent e) {
            // nada
        }

        // from interface DocumentListener
        public void changedUpdate(DocumentEvent e) {
            // nada
        }
    }

    protected ActionScriptEditorKit _kit;
    protected JPopupMenu _popup;
    protected UndoManager _undo = new UndoManager();
    protected UndoableEditListener _undoHandler = new UndoHandler();
    protected SwiftlyEditor _editor;
    protected SwiftlyDocument _document;
    protected UndoAction _undoAction = new UndoAction();
    protected RedoAction _redoAction = new RedoAction();
    protected boolean _documentChanged;
}

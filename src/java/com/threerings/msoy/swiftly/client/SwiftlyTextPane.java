package com.threerings.msoy.swiftly.client;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import java.io.IOException;
import java.io.StringReader;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

import javax.swing.text.BadLocationException;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import com.threerings.msoy.swiftly.data.DocumentElement;
import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.msoy.swiftly.util.SwiftlyContext;

import sdoc.SyntaxDocument;
import sdoc.SyntaxEditorKit;
import sdoc.SyntaxSupport;

public class SwiftlyTextPane extends JEditorPane
{
    public SwiftlyTextPane (SwiftlyContext ctx, SwiftlyEditor editor, DocumentElement document)
    {
        _ctx = ctx;
        _editor = editor;
        _document = document;

        _kit = new SyntaxEditorKit();
        setEditorKit(_kit);

        // setContentType("text/actionscript");

        // setup the actions
        _undoAction = new UndoAction();
        _redoAction = new RedoAction();
        _saveAction = new SaveAction();

        addKeyBindings();
        addPopupMenu();

        // setup some default colors
        // TODO make setable by the user?
        setForeground(Color.black);
        setBackground(Color.white);

        // TODO pick the lexer based on the mime type
        SyntaxSupport support = SyntaxSupport.getInstance();
        support.addSupport(SyntaxSupport.JAVA_LEXER, this);
        getDocument().putProperty(SyntaxDocument.tabSizeAttribute, new Integer(2));

        setDocumentElement(document);
    }

    public void setDocumentElement (DocumentElement document)
    {
        SyntaxDocument styledDoc = (SyntaxDocument) getDocument();
        styledDoc.addUndoableEditListener(new UndoHandler());
        styledDoc.addDocumentListener(new DocumentElementListener());

        try {
            _kit.read(new StringReader(document.getText()), styledDoc, 0);
        } catch (IOException io) {
            // TODO: complain?
        } catch (BadLocationException be) {
            // TODO: complain?
        }

        setDocument(styledDoc);
        setDocumentChanged(false);
    }

    public DocumentElement getDocumentElement ()
    {
        return _document;
    }

    /**
     * Save the document if it contains unsaved changes.
     * @return true if the save happened, false otherwise.
     */
    public boolean saveDocument ()
    {
        if (hasUnsavedChanges()) {
            try {
                _document.setText(getDocument().getText(0, getDocument().getLength()));
                _editor.saveDocumentElement(_document);
                setDocumentChanged(false);
                return true;
            } catch (BadLocationException be) {
                // TODO: warn
            }
        }
        return false;
    }

    /**
     * Sets the value of _documentChanged, as well as handling a number of other bits of business
     * that need to change whenever _documentChanged does. This should always be used instead of 
     * setting _documentChanged directly.
     */
    public void setDocumentChanged (boolean value)
    {
        _documentChanged = value;
        _editor.updateCurrentTabTitle();
        _saveAction.setEnabled(value);
    }

    /**
     * Returns true if the document has unsaved changes, false otherwise.
     */
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

    public Action getSaveAction ()
    {
        return _saveAction;
    }

    public Action createCutAction ()
    {
        return new AbstractAction(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, "m.action.cut")) {
            public void actionPerformed (ActionEvent e) {
                cut();
            }
        };
    }

    public Action createCopyAction ()
    {
        return new AbstractAction(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, "m.action.copy")) {
            public void actionPerformed (ActionEvent e) {
                copy();
            }
        };
    }

    public Action createPasteAction ()
    {
        return new AbstractAction(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, "m.action.paste")) {
            public void actionPerformed (ActionEvent e) {
                paste();
            }
        };
    }

    public Action createSelectAllAction ()
    {
        return new AbstractAction(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, "m.action.select_all")) {
            public void actionPerformed (ActionEvent e) {
                selectAll();
            }
        };
    }

    protected void addKeyBindings ()
    {
        // ctrl-s saves the current document
        addKeyAction(getSaveAction(),
                     KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));

        // ctrl-w closes the tab
        addKeyAction(_editor.createCloseCurrentTabAction(),
                     KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.CTRL_MASK));

        // ctrl-z undoes the action
        addKeyAction(_undoAction, KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));

        // ctrl-y redoes the action
        addKeyAction(_redoAction, KeyStroke.getKeyStroke(KeyEvent.VK_Y, ActionEvent.CTRL_MASK));
    }

    protected void addPopupMenu ()
    {
        _popup = new JPopupMenu();

        // TODO is there a cross platform way to show what the keybindings are for these actions?
        _popup.add(createCutAction());
        _popup.add(createCopyAction());
        _popup.add(createPasteAction());
        _popup.addSeparator();
        _popup.add(createSelectAllAction());
        _popup.addSeparator();
        _popup.add(_undoAction);
        _popup.add(_redoAction);

        MouseListener popupListener = new PopupListener();
        // add popupListener to the textpane
        addMouseListener(popupListener);
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

    protected class SaveAction extends AbstractAction
    {
        public SaveAction ()
        {
            super(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, "m.action.save"));
            setEnabled(false);
        }

        // from interface AbstractAction
        public void actionPerformed (ActionEvent e) {
            if (hasUnsavedChanges()) {
                saveDocument();
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
            super(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, "m.action.undo"));
            setEnabled(false);
        }

        // from AbstractAction
        public void actionPerformed (ActionEvent e)
        {
            try {
                _undo.undo();
            } catch (CannotUndoException ex) {
                // update() will set the action disabled
            }
            update();
            _redoAction.update();
        }

        protected void update ()
        {
            if(_undo.canUndo()) {
                setEnabled(true);
            }
            else {
                setEnabled(false);
                // We have undone to the previous save
                if (hasUnsavedChanges()) {
                    setDocumentChanged(false);
                }
            }
        }
    }

    protected class RedoAction extends AbstractAction
    {
        public RedoAction ()
        {
            super(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, "m.action.redo"));
            setEnabled(false);
        }

        // from AbstractAction
        public void actionPerformed (ActionEvent e)
        {
            try {
                _undo.redo();
            } catch (CannotRedoException ex) {
                // update() will set the action disabled
            }
            update();
            _undoAction.update();
        }

        protected void update ()
        {
            if(_undo.canRedo()) {
                setEnabled(true);
            }
            else {
                setEnabled(false);
            }
        }
    }

    class DocumentElementListener implements DocumentListener {
        // from interface DocumentListener
        public void insertUpdate(DocumentEvent e) {
            if (!hasUnsavedChanges()) {
                setDocumentChanged(true);
            }
        }

        // from interface DocumentListener
        public void removeUpdate(DocumentEvent e) {
            if (!hasUnsavedChanges()) {
                setDocumentChanged(true);
            }
        }

        // from interface DocumentListener
        public void changedUpdate(DocumentEvent e) {
            // nada
        }
    }

    protected SwiftlyContext _ctx;
    protected SwiftlyEditor _editor;
    protected DocumentElement _document;

    protected SyntaxEditorKit _kit;
    protected JPopupMenu _popup;
    protected UndoManager _undo = new UndoManager();
    protected UndoableEditListener _undoHandler = new UndoHandler();
    protected UndoAction _undoAction;
    protected RedoAction _redoAction;
    protected SaveAction _saveAction;
    protected boolean _documentChanged = false;
}

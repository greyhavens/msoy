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

import com.threerings.msoy.swiftly.data.DocumentUpdateListener;
import com.threerings.msoy.swiftly.data.DocumentUpdatedEvent;
import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.msoy.swiftly.data.SwiftlyTextDocument;
import com.threerings.msoy.swiftly.util.SwiftlyContext;

import sdoc.SyntaxDocument;
import sdoc.SyntaxEditorKit;
import sdoc.SyntaxSupport;

public class SwiftlyTextPane extends JEditorPane
    implements DocumentUpdateListener
{
    public static final int PRINT_MARGIN_WIDTH = 100; 

    public SwiftlyTextPane (SwiftlyContext ctx, SwiftlyEditor editor, PathElement pathElement)
    {
        _ctx = ctx;
        _editor = editor;
        _pathElement = pathElement;

        // TODO: this might not be required
        _kit = new SyntaxEditorKit();
        setEditorKit(_kit);

        setContentType(pathElement.getMimeType());

        // setup the actions
        _undoAction = new UndoAction();
        _redoAction = new RedoAction();

        addKeyBindings();
        addPopupMenu();

        // setup some default colors
        // TODO make setable by the user?
        setForeground(Color.black);
        setBackground(Color.white);

        // TODO pick the lexer based on the mime type
        SyntaxSupport support = SyntaxSupport.getInstance();
        support.addSupport(SyntaxSupport.JAVA_LEXER, this);
        support.setUseDefaultUndoManager(false);
        support.setPrintMarginWidth(PRINT_MARGIN_WIDTH);
        getDocument().putProperty(SyntaxDocument.tabSizeAttribute, new Integer(4));
        // TODO: use the SyntaxSupport anti alias font business
        
        // initialize the SyntaxDocument
        _syntaxDoc = (SyntaxDocument) getDocument();
        _syntaxDoc.addUndoableEditListener(new UndoHandler());
        _syntaxDoc.addDocumentListener(new DocumentElementListener());

        // lock the editor to input waiting for the document to load
        setEditable(false);
    }

    public void setDocument (SwiftlyTextDocument document)
    {
        _document = document;
        loadDocumentText();
        // unlock the editor
        setEditable(true);
    }

    // from DocumentUpdateListener
    public void documentUpdated (DocumentUpdatedEvent event) {
        // only apply the document changes if the event is for this textpane's document
        // and we were not the sender
        if (event.getElementId() == _document.elementId && 
            event.getEditorOid() != _ctx.getClient().getClientOid()) {
            loadDocumentText();
        }
    }

    public PathElement getPathElement ()
    {
        return _pathElement;
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

    protected void loadDocumentText ()
    {
        // remember where the cursor is
        int pos = getCaretPosition();

        // this change came from the network so don't send it back out again
        _dontPropagateThisChange = true;

        setText(_document.getText());
        _dontPropagateThisChange = false;

        // set the cursor back to where it was
        // TODO: this is a fairly useless hack that will go away.
        try {
            setCaretPosition(pos);
        } catch (IllegalArgumentException e) {
            // that's fine, the position has gone away with this edit
        }
    }

    protected void addKeyBindings ()
    {
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

    protected void updateDocument ()
    {
        if (_document != null) {
            _editor.updateDocument(_document.elementId, getText());
        }
    }

    protected class PopupListener extends MouseAdapter
    {
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
            if (_undo.canUndo()) {
                setEnabled(true);
            } else {
                setEnabled(false);
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
            if (_undo.canRedo()) {
                setEnabled(true);
            } else {
                setEnabled(false);
            }
        }
    }

    class DocumentElementListener implements DocumentListener
    {
        // from interface DocumentListener
        public void insertUpdate(DocumentEvent e) {
            // send out the update event only if we didn't get this update from the network
            if (!_dontPropagateThisChange) {
                updateDocument();
            }
        }

        // from interface DocumentListener
        public void removeUpdate(DocumentEvent e) {
            // send out the update event only if we didn't get this update from the network
            if (!_dontPropagateThisChange) {
                updateDocument();
            }
        }

        // from interface DocumentListener
        public void changedUpdate(DocumentEvent e) {
            // nada
        }
    }

    protected SwiftlyContext _ctx;
    protected SwiftlyEditor _editor;
    protected PathElement _pathElement;
    protected SwiftlyTextDocument _document;
    protected SyntaxDocument _syntaxDoc;
    protected boolean _dontPropagateThisChange;

    protected SyntaxEditorKit _kit;
    protected JPopupMenu _popup;
    protected UndoManager _undo = new UndoManager();
    protected UndoableEditListener _undoHandler = new UndoHandler();
    protected UndoAction _undoAction;
    protected RedoAction _redoAction;
}

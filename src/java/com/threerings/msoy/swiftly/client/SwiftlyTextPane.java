//
// $Id$

package com.threerings.msoy.swiftly.client;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Element;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import sdoc.SyntaxDocument;
import sdoc.SyntaxEditorKit;
import sdoc.SyntaxSupport;

import com.threerings.msoy.swiftly.data.DocumentUpdateListener;
import com.threerings.msoy.swiftly.data.DocumentUpdatedEvent;
import com.threerings.msoy.swiftly.data.ProjectRoomObject;
import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.msoy.swiftly.data.SwiftlyDocument;
import com.threerings.msoy.swiftly.data.SwiftlyTextDocument;
import com.threerings.msoy.swiftly.util.SwiftlyContext;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;
import com.threerings.util.MessageBundle;

public class SwiftlyTextPane extends JEditorPane
    implements DocumentUpdateListener, SetListener, PositionableComponent, AccessControlListener
{
    public static final int PRINT_MARGIN_WIDTH = 100;

    public SwiftlyTextPane (SwiftlyContext ctx, SwiftlyEditor editor, SwiftlyTextDocument document)
    {
        _ctx = ctx;
        _editor = editor;
        _document = document;
        _msgs = _ctx.getMessageManager().getBundle(SwiftlyCodes.SWIFTLY_MSGS);

        // TODO: this might not be required
        _kit = new SyntaxEditorKit();
        setEditorKit(_kit);

        setContentType(document.getPathElement().getMimeType());

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
        support.highlightCurrent(false);
        getDocument().putProperty(SyntaxDocument.tabSizeAttribute, new Integer(4));
        // TODO: use the SyntaxSupport anti alias font business

        // initialize the SyntaxDocument
        _syntaxDoc = (SyntaxDocument) getDocument();
        _syntaxDoc.addUndoableEditListener(new UndoHandler());
        _syntaxDoc.addDocumentListener(new DocumentElementListener());

        // load the document into the text pane
        loadDocumentText();
    }

    // from AccessControlListener
    public void writeAccessGranted ()
    {
        setEditable(true);
    }

    // from AccessControlListener
    public void readOnlyAccessGranted ()
    {
        setEditable(false);
    }

    @Override // from JComponent
    public void addNotify ()
    {
        super.addNotify();
        _editor.addAccessControlListener(this);
    }

    @Override // from JComponent
    public void removeNotify ()
    {
        super.removeNotify();
        _editor.removeAccessControlListener(this);
    }

    // from DocumentUpdateListener
    public void documentUpdated (DocumentUpdatedEvent event) {
        // only apply the document changes if the event is for this textpane's document
        // and we were not the sender
        if (event.getDocumentId() == _document.documentId &&
            event.getEditorOid() != _ctx.getClient().getClientOid()) {
            loadDocumentText();
        }
    }

    // from interface SetListener
    public void entryAdded (EntryAddedEvent event)
    {
        // nada
    }

    // from interface SetListener
    public void entryUpdated (EntryUpdatedEvent event)
    {
        if (event.getName().equals(ProjectRoomObject.DOCUMENTS)) {
            SwiftlyDocument element = (SwiftlyDocument)event.getEntry();
            // check to see if the updated document is the one being displayed
            if (element.documentId == _document.documentId) {
                SwiftlyTextDocument newDoc = (SwiftlyTextDocument)element;
                SwiftlyTextDocument oldDoc = (SwiftlyTextDocument)event.getOldEntry();

                // update the document reference to point at the new document
                _document = newDoc;

                // only refresh the text if the document text has changed in the new document
                if (!newDoc.getText().equals(oldDoc.getText())) {
                    // display the new text
                    loadDocumentText();
                }
            }
        }
    }

    // from interface SetListener
    public void entryRemoved (EntryRemovedEvent event)
    {
        // nada
    }

    // from PositionableComponent
    public Component getComponent ()
    {
        return this;
    }

    // from PositionableComponent
    public void gotoLocation (int row, int column, boolean highlight)
    {
        // TODO: the component should get focus after moving the caret

        // move the caret to the requested position
        Element root = getDocument().getDefaultRootElement();
        // row = 1, column = 1 is the starting position. anything less will be a problem
        int character = Math.max(column, 1);
        int line = Math.max(row, 1);
        line = Math.min(line, root.getElementCount());
        setCaretPosition(root.getElement(line - 1).getStartOffset() + (character - 1));

        // highlight the new position if requested
        if (highlight) {
            try {
                final Object position = getHighlighter().addHighlight(
                    getCaretPosition(), getCaretPosition() + 1,
                    new DefaultHighlighter.DefaultHighlightPainter(Color.yellow));;
                // show the highlighting for 3 seconds
                Timer timer = new Timer(3000, new ActionListener () {
                    public void actionPerformed (ActionEvent evt)
                    {
                        getHighlighter().removeHighlight(position);
                    }
                });
                timer.setRepeats(false);
                timer.start();
            } catch (BadLocationException e) {
                // nada. can't highlight nothing
            }
        }
    }

    public Action createCutAction ()
    {
        return new AbstractAction(_msgs.get("m.action.cut")) {
            public void actionPerformed (ActionEvent e) {
                cut();
            }
        };
    }

    public Action createCopyAction ()
    {
        return new AbstractAction(_msgs.get("m.action.copy")) {
            public void actionPerformed (ActionEvent e) {
                copy();
            }
        };
    }

    public Action createPasteAction ()
    {
        return new AbstractAction(_msgs.get("m.action.paste")) {
            public void actionPerformed (ActionEvent e) {
                paste();
            }
        };
    }

    public Action createSelectAllAction ()
    {
        return new AbstractAction(_msgs.get("m.action.select_all")) {
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

        // add popupListener to the textpane
        addMouseListener(new PopupListener());
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
            _editor.updateDocument(_document.documentId, getText());
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
            super(_msgs.get("m.action.undo"));
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
            super(_msgs.get("m.action.redo"));
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
        public void insertUpdate(DocumentEvent e)
        {
            // send out the update event only if we didn't get this update from the network
            if (!_dontPropagateThisChange) {
                updateDocument();
            }
        }

        // from interface DocumentListener
        public void removeUpdate(DocumentEvent e)
        {
            // send out the update event only if we didn't get this update from the network
            if (!_dontPropagateThisChange) {
                updateDocument();
            }
        }

        // from interface DocumentListener
        public void changedUpdate(DocumentEvent e)
        {
            // nada
        }
    }

    protected SwiftlyContext _ctx;
    protected SwiftlyEditor _editor;
    protected MessageBundle _msgs;
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

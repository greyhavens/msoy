//
// $Id$

package com.threerings.msoy.swiftly.client.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
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

import com.threerings.msoy.swiftly.client.Translator;
import com.threerings.msoy.swiftly.client.controller.DocumentUpdateDispatcher;
import com.threerings.msoy.swiftly.client.controller.EditorActionProvider;
import com.threerings.msoy.swiftly.data.SwiftlyTextDocument;

/**
 * Implementation of TextEditor.
 */
public class TextEditorView extends JEditorPane
    implements TextEditor
{
    public TextEditorView (EditorActionProvider actions, Translator translator,
                           SwiftlyTextDocument document, DocumentUpdateDispatcher dispatcher)
    {
        _translator = translator;
        _document = document;
        _dispatcher = dispatcher;

        // construct these actions after we have the translator available
        _undoAction = new UndoAction();
        _redoAction = new RedoAction();

        // TODO: this might not be required
        _kit = new SyntaxEditorKit();
        setEditorKit(_kit);

        setContentType(document.getPathElement().getMimeType());

        // setup the actions
        _cutAction = new AbstractAction(_translator.xlate("m.action.cut")) {
            public void actionPerformed (ActionEvent e) {
                cut();
            }
        };
        _copyAction = new AbstractAction(_translator.xlate("m.action.copy")) {
            public void actionPerformed (ActionEvent e) {
                copy();
            }
        };
        _pasteAction = new AbstractAction(_translator.xlate("m.action.paste")) {
            public void actionPerformed (ActionEvent e) {
                paste();
            }
        };
        _selectAllAction = new AbstractAction(_translator.xlate("m.action.select_all")) {
            public void actionPerformed (ActionEvent e) {
                selectAll();
            }
        };

        // add key bindings
        // ctrl-w closes the tab
        addKeyAction(actions.getCloseCurrentTabAction(),
            KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.CTRL_MASK));

        // ctrl-z undoes the action
        addKeyAction(_undoAction, KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));

        // ctrl-y redoes the action
        addKeyAction(_redoAction, KeyStroke.getKeyStroke(KeyEvent.VK_Y, ActionEvent.CTRL_MASK));

        addPopupMenu();

        // setup some default colors
        // TODO make setable by the user?
        setForeground(Color.black);
        setBackground(Color.white);

        // tweak the margins which are borked by SDoc or Substance
        setMargin(new Insets(-2, 0, 0, 0));

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

        // set a good default font size
        setFont(getFont().deriveFont(DEFAULT_FONT_SIZE));

        // load the document into the text pane
        documentTextChanged();
    }

    // from AccessControlComponent
    public void showWriteAccess ()
    {
        setEditable(true);
    }

    // from AccessControlComponent
    public void showReadOnlyAccess ()
    {
        setEditable(false);
    }

    // from PositionableComponent
    public Component getComponent ()
    {
        return this;
    }

    // from PositionableComponent
    public void gotoLocation (PositionLocation location)
    {
        // TODO: the component should get focus after moving the caret

        // move the caret to the requested position
        Element root = getDocument().getDefaultRootElement();
        // row = 1, column = 1 is the starting position. anything less will be a problem
        int character = Math.max(location.column, 1);
        int line = Math.max(location.row, 1);
        line = Math.min(line, root.getElementCount());
        setCaretPosition(root.getElement(line - 1).getStartOffset() + (character - 1));

        // highlight the new position if requested
        if (location.highlight) {
            try {
                final Object position = getHighlighter().addHighlight(
                    getCaretPosition(), getCaretPosition() + 1,
                    new DefaultHighlighter.DefaultHighlightPainter(Color.yellow));
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

    // from DocumentEditor
    public SwiftlyTextDocument getSwiftlyDocument ()
    {
        return _document;
    }

    // from DocumentEditor
    public void loadDocument (SwiftlyTextDocument doc)
    {
        SwiftlyTextDocument currentDoc = _document;
        _document = doc;

        // only refresh the text if the text data has changed in the new document
        if (!doc.contentsEqual(currentDoc)) {
            documentTextChanged();
        }
    }

    // from TextEditor
    public void documentTextChanged ()
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

    private void addPopupMenu ()
    {
        // TODO is there a cross platform way to show what the keybindings are for these actions?
        _popup.add(_cutAction);
        _popup.add(_copyAction);
        _popup.add(_pasteAction);
        _popup.addSeparator();
        _popup.add(_selectAllAction);
        _popup.addSeparator();
        _popup.add(_undoAction);
        _popup.add(_redoAction);

        // add popupListener to the textpane
        addMouseListener(new PopupListener());
    }

    private void addKeyAction (Action action, KeyStroke key)
    {
        // key bindings work even if the textpane doesn't have focus
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, action);
        getActionMap().put(action, action);
    }

    private class PopupListener extends MouseAdapter
    {
        @Override // from MouseAdapter
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        @Override // from MouseAdapter
        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                _popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    private class UndoHandler implements UndoableEditListener
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

    private class UndoAction extends AbstractAction
    {
        public UndoAction () {
            super(_translator.xlate("m.action.undo"));
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

        private void update ()
        {
            if (_undo.canUndo()) {
                setEnabled(true);
            } else {
                setEnabled(false);
            }
        }
    }

    private class RedoAction extends AbstractAction
    {
        public RedoAction ()
        {
            super(_translator.xlate("m.action.redo"));
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

        private void update ()
        {
            if (_undo.canRedo()) {
                setEnabled(true);
            } else {
                setEnabled(false);
            }
        }
    }

    /**
     * Listens for changes to the document view and dispatches those changes to the controller.
     */
    private class DocumentElementListener implements DocumentListener
    {
        // from interface DocumentListener
        public void insertUpdate(DocumentEvent e)
        {
            // send out the update event only if we didn't get this update from the network
            if (!_dontPropagateThisChange) {
                _dispatcher.documentTextChanged(_document, getText());
            }
        }

        // from interface DocumentListener
        public void removeUpdate(DocumentEvent e)
        {
            // send out the update event only if we didn't get this update from the network
            if (!_dontPropagateThisChange) {
                _dispatcher.documentTextChanged(_document, getText());
            }
        }

        // from interface DocumentListener
        public void changedUpdate(DocumentEvent e)
        {
            // nada
        }
    }

    private static final int PRINT_MARGIN_WIDTH = 100;
    private static final float DEFAULT_FONT_SIZE = 14;

    private final Translator _translator;
    private final DocumentUpdateDispatcher _dispatcher;
    private SwiftlyTextDocument _document;
    private final SyntaxDocument _syntaxDoc;
    private boolean _dontPropagateThisChange;

    private final SyntaxEditorKit _kit;
    private final JPopupMenu _popup = new JPopupMenu();
    private final Action _cutAction;
    private final Action _copyAction;
    private final Action _pasteAction;
    private final Action _selectAllAction;
    private final UndoManager _undo = new UndoManager();
    private final UndoAction _undoAction;
    private final RedoAction _redoAction;
}

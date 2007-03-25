//
// $Id$

package com.threerings.msoy.swiftly.client;        

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import sdoc.Gutter;

import com.samskivert.swing.HGroupLayout;
import com.samskivert.swing.VGroupLayout;
import com.samskivert.swing.util.SwingUtil;
import com.samskivert.util.StringUtil;
import com.threerings.util.MessageBundle;

import com.threerings.presents.client.InvocationService.ConfirmListener;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.crowd.client.PlacePanel;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.micasa.client.ChatPanel;
import com.threerings.micasa.client.OccupantList;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;

import com.threerings.msoy.swiftly.data.DocumentUpdatedEvent;
import com.threerings.msoy.swiftly.data.CompilerOutput;
import com.threerings.msoy.swiftly.data.FlexCompilerOutput;
import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.data.ProjectRoomObject;
import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.msoy.swiftly.data.SwiftlyDocument;
import com.threerings.msoy.swiftly.data.SwiftlyTextDocument;
import com.threerings.msoy.swiftly.util.SwiftlyContext;

import static com.threerings.msoy.Log.log;

public class SwiftlyEditor extends PlacePanel
    implements SwiftlyDocumentEditor, AttributeChangeListener, SetListener
{
    public SwiftlyEditor (ProjectRoomController ctrl, SwiftlyContext ctx)
    {
        super(ctrl);
        _ctx = ctx;
        _ctrl = ctrl;
        _msgs = _ctx.getMessageManager().getBundle(SwiftlyCodes.SWIFTLY_MSGS);

        setLayout(new VGroupLayout(
                      VGroupLayout.STRETCH, VGroupLayout.STRETCH, 5, VGroupLayout.TOP));
        // let's not jam ourselves up against the edges of the window
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // add our toolbar
        add(_toolbar = new EditorToolBar(ctrl, _ctx, this), VGroupLayout.FIXED);

        // set up the top pane: project panel and editor
        _editorTabs = new TabbedEditor(_ctx, this);
        _editorTabs.setMinimumSize(new Dimension(400, 400));

        _projectPanel = new ProjectPanel(_ctx, this);
        _projectPanel.setMinimumSize(new Dimension(0, 0));

        JSplitPane topPane =
            new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, _projectPanel, _editorTabs);
        topPane.setOneTouchExpandable(true);
        topPane.setDividerLocation(200);

        // set up the bottom pane: chat and console
        JPanel chatPanel = new JPanel(
            new HGroupLayout(HGroupLayout.STRETCH, HGroupLayout.STRETCH, 5, HGroupLayout.LEFT));
        chatPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        OccupantList ol;
        chatPanel.add(ol = new OccupantList(_ctx), HGroupLayout.FIXED);
        ol.setPreferredSize(new Dimension(100, 0));
        chatPanel.add(new ChatPanel(_ctx, false));

        _console = new Console(_ctx, this);
        _console.setMinimumSize(new Dimension(0, 0));

        JSplitPane bottomPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chatPanel, _console);
        bottomPane.setOneTouchExpandable(true);
        bottomPane.setDividerLocation(400);

        _contentPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPane, bottomPane);
        _contentPane.setOneTouchExpandable(true);
        add(_contentPane);

        initFileTypes();
        consoleMessage(_msgs.get("m.welcome"));
    }

    @Override // from Component
    public void doLayout ()
    {
        super.doLayout();

        // set up our divider location when we are first laid out
        if (getHeight() != 0 && _contentPane.getLastDividerLocation() == 0) {
            _contentPane.setDividerLocation(getHeight()-200);
        }
    }

    public void openPathElement (final PathElement pathElement)
    {
        boolean alreadyOpen = _editorTabs.selectTab(pathElement); 
        // If this is a new tab, add a documentupdate listener and ask the backend to load
        // the document contents or pull the already opened document from the dset
        if (!alreadyOpen) {
            // If the document is already in the dset, load that.
            SwiftlyDocument doc = getDocumentFromPath(pathElement);
            if (doc != null) {
                doc.loadInEditor(this);
                return;
            }

            // Otherwise load the document from the backend.
            _roomObj.service.loadDocument(_ctx.getClient(), pathElement, new ConfirmListener() {
                // from interface ConfirmListener
                public void requestProcessed () {
                    SwiftlyDocument doc = getDocumentFromPath(pathElement);
                    doc.loadInEditor(SwiftlyEditor.this);
                }
                // from interface ConfirmListener
                public void requestFailed (String reason) {
                    showErrorDialog(_msgs.xlate(reason));
                }
            });
        }
    }

    public void editTextDocument (SwiftlyTextDocument document)
    {
        PathElement pathElement = document.getPathElement();
        SwiftlyTextPane textPane = new SwiftlyTextPane(_ctx, this, pathElement);
        TabbedEditorScroller scroller = new TabbedEditorScroller(textPane, pathElement);
        // add line numbers
        scroller.setRowHeaderView(new Gutter(textPane, scroller));

        // add the tab
        _editorTabs.addEditorTab(scroller, pathElement);

        // set the document into the text pane
        textPane.setDocument(document);

        // TODO: text pane will not be a listener
        _roomObj.addListener(textPane);
    }

    public void updateTabTitleAt (PathElement pathElement)
    {
        _editorTabs.updateTabTitleAt(pathElement);
    }

    public void setTabDocument (SwiftlyTextDocument doc)
    {
        _editorTabs.setTabDocument(doc);
    }

    public void closeCurrentTab ()
    {
        // TODO: SwiftlyTextPane will no longer be a listener
        // _roomObj.removeListener(_editorTabs.getSelectedComponent());
        _editorTabs.closeCurrentTab();
    }

    /**
     * See {@link Console} for documentation.
     */
    public void consoleMessage (String message)
    {
        _console.consoleMessage(message);
    }

    public AbstractAction createCloseCurrentTabAction ()
    {
        return _editorTabs.createCloseCurrentTabAction();
    }

    public Action getPreviewAction ()
    {
        if (_previewAction == null) {
            _previewAction = new AbstractAction(_msgs.get("m.action.preview")) {
                // from AbstractAction
                public void actionPerformed (ActionEvent e) {
                    showPreview();
                }
            };
            _previewAction.setEnabled(false);
        }
        return _previewAction;
    }

    public EditorToolBar getToolbar()
    {
        return _toolbar;
    }

    public ProjectPanel getProjectPanel ()
    {
        return _projectPanel;
    }

    /**
     * Sends a message to the server reporting that the given document element should have its
     * text replaced with the supplied string.
     */
    public void updateDocument (int elementId, String text)
    {
        _roomObj.service.updateDocument(_ctx.getClient(), elementId, text);
    }

    /**
     * Shows a modal, internal frame dialog prompting the user to name a {@link PathElement}
     * @param pathElementType the type of {@link PathElement} to name
     * @return the name of the path element. null if the user clicked cancel
     */
     // TODO: this is only being used to name directories. Consider simplifying
    public String showSelectPathElementNameDialog (PathElement.Type pathElementType)
    {
        String prompt;
        prompt = _msgs.get("m.dialog.select_name." + pathElementType);
        return JOptionPane.showInternalInputDialog(this, prompt);
    }

    /**
     * Shows a modal, external frame dialog prompting the user to name a {@link PathElement.FILE}
     * and select the mime type for this file.
     * @param parentElement the PathElement that will be the parent of the returned PathElement
     * @return the new path element. null if the user clicked cancel
     */
    public PathElement showCreateFileDialog (PathElement parentElement)
    {
        CreateFileDialog dialog = new CreateFileDialog();
        // return null if the user hit cancelled or did not set a file name
        if (dialog.wasCancelled() || StringUtil.isBlank(dialog.getName())) {
            return null;
        }
        return PathElement.createFile(dialog.getName(), parentElement, dialog.getMimeType());
    }

    /**
     * Shows a modal, internal frame dialog reporting an error to the user.
     */
    public void showErrorDialog (String message)
    {
        JOptionPane.showInternalMessageDialog(
            this, message, _msgs.get("m.dialog.error.title"), JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Shows a modal, internal frame dialog asking for user confirmation.
     * Returns true if the user clicked Yes, false if they clicked No.
     */
    public boolean showConfirmDialog (String message)
    {
        int response = JOptionPane.showInternalConfirmDialog(
            this, message, _msgs.get("m.dialog.confirm.title"), JOptionPane.YES_NO_OPTION);
        return response == JOptionPane.YES_OPTION;
    }

    /**
     * Display a line in the console when the build starts.
     */
    public void buildStarted () 
    {
        consoleMessage(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, "m.build_started"));
    }

    @Override // from PlacePanel
    public void willEnterPlace (PlaceObject plobj)
    {
        _roomObj = (ProjectRoomObject)plobj;
        _roomObj.addListener(this);

        // Raise all any documents from the dead, re-binding transient
        // instance variables.
        for (SwiftlyDocument document : _roomObj.documents) {
            document.lazarus(_roomObj.pathElements);
        }

        // let our project panel know about all the roomy goodness
        _projectPanel.setProject(_roomObj);
    }

    @Override // from PlacePanel
    public void didLeavePlace (PlaceObject plobj)
    {
        if (_roomObj != null) {
            _roomObj.removeListener(this);
            _roomObj = null;
        }

        // TODO: shutdown the project panel?
    }

    // from interface AttributeChangeListener
    public void attributeChanged (AttributeChangedEvent event)
    {
        if (event.getName().equals(ProjectRoomObject.CONSOLE)) {
            consoleMessage(_msgs.xlate(_roomObj.console));

        } else if (event.getName().equals(ProjectRoomObject.RESULT)) {
            displayBuildResult();
            _previewAction.setEnabled(_roomObj.result.getBuildResultURL() != null);

        } else if (event.getName().equals(ProjectRoomObject.BUILDING)) {
            _ctrl.buildAction.setEnabled(!_roomObj.building);
        }
    }

    // from interface SetListener
    public void entryAdded (EntryAddedEvent event)
    {
        if (event.getName().equals(ProjectRoomObject.DOCUMENTS)) {
            final SwiftlyDocument element = (SwiftlyDocument)event.getEntry();
            // Re-bind transient instance variables
            element.lazarus(_roomObj.pathElements);
        }
    }

    // from interface SetListener
    public void entryUpdated (EntryUpdatedEvent event)
    {
        // TODO do we actually want to do anything here?
        if (event.getName().equals(ProjectRoomObject.DOCUMENTS)) {
            final SwiftlyDocument element = (SwiftlyDocument)event.getEntry();
            // Re-bind transient instance variables
            element.lazarus(_roomObj.pathElements);
        }
    }

    // from interface SetListener
    public void entryRemoved (EntryRemovedEvent event)
    {
        // TODO: if that document is still open FREAK OUT. Server is going to refcount so shouldn't
        // ever happen.
        if (event.getName().equals(ProjectRoomObject.DOCUMENTS)) {
            final int elementId = (Integer)event.getKey();
        }
    }

    /** 
      * Finds the SwiftlyDocument, if loaded, connected with the supplied PathElement.
      * Returns null if the SwiftlyDocument was not found.
      */
    protected SwiftlyDocument getDocumentFromPath (PathElement pathElement)
    {
        for (SwiftlyDocument doc : _roomObj.documents) {
            if (doc.getPathElement().elementId == pathElement.elementId) {
                return doc;
            }
        }
        return null;
    }

    /** Initialize the file types that can be created */
    protected void initFileTypes ()
    {
        _createableFileTypes = new ArrayList<FileTypes>();
        _createableFileTypes.add(new FileTypes(_msgs.get("m.filetypes." + MediaDesc.TEXT_PLAIN),
                                               MediaDesc.mimeTypeToString(MediaDesc.TEXT_PLAIN)));
        _createableFileTypes.add(
            new FileTypes(_msgs.get("m.filetypes." + MediaDesc.TEXT_ACTIONSCRIPT),
                          MediaDesc.mimeTypeToString(MediaDesc.TEXT_ACTIONSCRIPT)));
    }

    protected void showPreview ()
    {
        String resultUrl = _roomObj.result.getBuildResultURL();
        try {
            URL url = new URL(resultUrl);
            if (_roomObj.project.projectType == Item.AVATAR) {
                url = new URL(url, AVATAR_VIEWER_PATH + URLEncoder.encode(url.toString(), "UTF-8"));
            }
            _ctx.getAppletContext().showDocument(url, "_blank");
        } catch (Exception e) {
            log.warning("Failed to display results [url=" + resultUrl + ", error=" + e + "].");
            // TODO: we sent ourselves a bad url? display an error dialog?
        }
    }

    /** Displays the build result on the console */
    protected void displayBuildResult ()
    {
        for (CompilerOutput output : _roomObj.result.getOutput()) {
            FlexCompilerOutput flexOut = (FlexCompilerOutput)output;
            consoleMessage(flexOut.toString());
        }
    }

    /** A dialog window to prompt the user for a file name and file type. */
    protected class CreateFileDialog extends JDialog
    {
        public CreateFileDialog () {
            super(new JFrame(), _msgs.get("m.dialog.create_file.title"), true);
            setLayout(new GridLayout(3, 3, 10, 10));
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            // file name input
            add(new JLabel(_msgs.get("m.dialog.create_file.name")));
            _text = new JTextField();
            _text.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    _cancelled = false;
                    setVisible(false);
                }
            });
            _text.setEditable(true);
            add(_text);

            // file type chooser
            add(new JLabel(_msgs.get("m.dialog.create_file.type")));
            _comboBox = new JComboBox(_createableFileTypes.toArray());
            add(_comboBox);

            // ok/cancel buttons
            JButton button = new JButton(_msgs.get("m.dialog.create_file.create"));
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    _cancelled = false;
                    setVisible(false);
                }
            });
            add(button);
            button = new JButton(_msgs.get("m.dialog.create_file.cancel"));
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    _cancelled = true;
                    setVisible(false);
                }
            });
            add(button);

            // display the dialog
            pack();
            setLocationRelativeTo(_projectPanel);
            setVisible(true);
        }

        public String getName ()
        {
            return _text.getText();
        }

        public String getMimeType ()
        {
            return ((FileTypes)_comboBox.getSelectedItem()).mimeType;
        }

        public boolean wasCancelled ()
        {
            return _cancelled;
        }

        protected JTextField _text;
        protected JComboBox _comboBox;

        /** Whether the user clicked cancel. defaults to true to deal with closing the dialog. */
        protected boolean _cancelled = true;
    }

    /** A class that maps a human friendly name to a mime type. */
    protected class FileTypes
    {
        public FileTypes (String displayName, String mimeType)
        {
            this.displayName = displayName;
            this.mimeType = mimeType;
        }

        public String toString()
        {
            return displayName;
        }

        public String displayName;
        public String mimeType;
    }

    /** A list of files that can be created by Swiftly. */
    protected ArrayList<FileTypes> _createableFileTypes;

    protected SwiftlyContext _ctx;
    protected ProjectRoomController _ctrl;
    protected MessageBundle _msgs;
    protected ProjectRoomObject _roomObj;
    protected PathElement _project;

    protected JSplitPane _contentPane;
    protected TabbedEditor _editorTabs;
    protected Console _console;
    protected EditorToolBar _toolbar;
    protected ProjectPanel _projectPanel;
    protected Action _previewAction;

    protected static final String AVATAR_VIEWER_PATH = "/clients/avatarviewer.swf?avatar=";
}

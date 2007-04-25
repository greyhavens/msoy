//
// $Id$

package com.threerings.msoy.swiftly.client;        

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import sdoc.Gutter;

import org.apache.commons.io.IOUtils;

import com.samskivert.swing.HGroupLayout;
import com.samskivert.swing.VGroupLayout;
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
import com.threerings.crowd.data.PlaceObject;

import com.threerings.micasa.client.ChatPanel;
import com.threerings.micasa.client.OccupantList;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;

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

        // setup the path to the avatar viewer using the client version
        _avatarViewerPath = "/clients/" + _ctx.getClient().getVersion() +
                            "/avatarviewer.swf?avatar=";

        setLayout(new VGroupLayout(
                      VGroupLayout.STRETCH, VGroupLayout.STRETCH, 5, VGroupLayout.TOP));
        // let's not jam ourselves up against the edges of the window
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // add our toolbar
        add(_toolbar = new EditorToolBar(ctrl, _ctx, this), VGroupLayout.FIXED);

        // set up the left pane: the tabbed editor
        _editorTabs = new TabbedEditor(_ctx, this);
        _editorTabs.setMinimumSize(new Dimension(400, 400));

        // set up the right pane: project panel and chat
        _projectPanel = new ProjectPanel(_ctx, this);
        _projectPanel.setMinimumSize(new Dimension(200, 200));

        JPanel chatPanel = new JPanel(
            new HGroupLayout(HGroupLayout.STRETCH, HGroupLayout.STRETCH, 5, HGroupLayout.LEFT));
        chatPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        OccupantList ol;
        chatPanel.add(ol = new OccupantList(_ctx), HGroupLayout.FIXED);
        ol.setPreferredSize(new Dimension(50, 0));
        chatPanel.add(new ChatPanel(_ctx, false));
        chatPanel.setMinimumSize(new Dimension(0, 0));

        _rightPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, _projectPanel, chatPanel);
        _rightPane.setOneTouchExpandable(true);

        // add the console window which starts hidden
        _console = new Console(_ctx, this);

        _contentPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, _editorTabs, _rightPane);
        _contentPane.setOneTouchExpandable(true);
        add(_contentPane);

        initFileTypes();
    }

    @Override // from Component
    public void doLayout ()
    {
        super.doLayout();

        // set up our divider location when we are first laid out
        if (getWidth() != 0 && _contentPane.getLastDividerLocation() == 0) {
            _contentPane.setDividerLocation(getWidth()-200);

            // start with the chat pane hidden
            _rightPane.setDividerLocation(getHeight());
        }
    }

    public void openPathElement (final PathElement pathElement)
    {
        // If the tab already exists, then select it and be done.
        if (_editorTabs.selectTab(pathElement)) {
            return;
        }

        // If the document is already in the dset, load that.
        SwiftlyDocument doc = _roomObj.getDocument(pathElement);
        if (doc != null) {
            doc.loadInEditor(this);
            return;
        }

        // Otherwise load the document from the backend.
        _roomObj.service.loadDocument(_ctx.getClient(), pathElement, new ConfirmListener() {
            public void requestProcessed () {
                SwiftlyDocument doc = _roomObj.getDocument(pathElement);
                if (doc == null) {
                    showErrorMessage(_msgs.get("e.load_document_failed"));
                } else {
                    doc.loadInEditor(SwiftlyEditor.this);
                }
            }
            public void requestFailed (String reason) {
                showErrorMessage(_msgs.xlate(reason));
            }
        });
    }

    public void closePathElement (PathElement element)
    {
        _editorTabs.closePathElementTab(element);
    }

    // from SwiftlyDocumentEditor
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

        // TODO: remove when the the textpane is no longer the document listener
        _roomObj.addListener(textPane);
    }

    // from SwiftlyDocumentEditor
    public List<FileTypes> getCreateableFileTypes ()
    {
        return _createableFileTypes; 
    }

    public void updateTabTitleAt (PathElement pathElement)
    {
        _editorTabs.updateTabTitleAt(pathElement);
    }

    public void tabRemoved (TabbedEditorComponent tab)
    {
        // TODO: remove when the the textpane is no longer the document listener
        if (tab instanceof TabbedEditorScroller) {
            Component comp = ((TabbedEditorScroller)tab).getViewport().getView();
            if (comp instanceof SwiftlyTextPane) {
                _roomObj.removeListener((SwiftlyTextPane)comp);
            }
        }
    }

    /**
     * See {@link Console} for documentation.
     */
    public void consoleMessage (String message)
    {
        _console.consoleMessage(message);
    }

    /**
     * See {@link Console} for documentation.
     */
    public void consoleErrorMessage (String message)
    {
        _console.errorMessage(message);
    }

    public AbstractAction createCloseCurrentTabAction ()
    {
        return _editorTabs.createCloseCurrentTabAction();
    }

    public Action getPreviewAction ()
    {
        if (_previewAction == null) {
            _previewAction = new AbstractAction(_msgs.get("m.action.preview")) {
                public void actionPerformed (ActionEvent e) {
                    showPreview();
                }
            };
            _previewAction.setEnabled(false);
        }
        return _previewAction;
    }

    public Action getExportAction ()
    {
        if (_exportAction == null) {
            _exportAction = new AbstractAction(_msgs.get("m.action.export")) {
                public void actionPerformed (ActionEvent e) {
                    exportResult();
                }
            };
            _exportAction.setEnabled(false);
        }
        return _exportAction;
    }

    public Action createShowConsoleAction ()
    {
        return new AbstractAction(_msgs.get("m.action.show_console")) {
            public void actionPerformed (ActionEvent e) {
                _console.setVisible(true);
            }
        };
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
    public CreateFileDialog showCreateFileDialog (PathElement parentElement)
    {
        CreateFileDialog dialog = new CreateFileDialog(this, _projectPanel, _msgs);
        // return null if the user hit cancelled or did not set a file name
        if (dialog.wasCancelled() || StringUtil.isBlank(dialog.getName())) {
            return null;
        }
        return dialog;
    }

    /**
     * Shows an error message to the user using the console.
     */
    public void showErrorMessage (String message)
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

    @Override // from PlacePanel
    public void willEnterPlace (PlaceObject plobj)
    {
        _roomObj = (ProjectRoomObject)plobj;
        _roomObj.addListener(this);

        // Raise any documents from the dead, re-binding transient
        // instance variables.
        for (SwiftlyDocument document : _roomObj.documents) {
            document.lazarus(_roomObj.pathElements);
        }

        // if we have already resolved the project, load it into the project panel
        // otherwise we'll wait for the message that the project has been resolved later
        if (_roomObj.project != null) {
            _projectPanel.setProject(_roomObj);
        }
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
        if (event.getName().equals(ProjectRoomObject.CONSOLE_OUT)) {
            consoleMessage(_msgs.xlate(_roomObj.consoleOut));

        } else if (event.getName().equals(ProjectRoomObject.CONSOLE_ERR)) {
            consoleErrorMessage(_msgs.xlate(_roomObj.consoleErr));

        } else if (event.getName().equals(ProjectRoomObject.RESULT)) {
            displayBuildResult();
            boolean haveResult = _roomObj.result.buildSuccessful() &&
                (_roomObj.result.getBuildResultURL() != null);
            _previewAction.setEnabled(haveResult);
            _exportAction.setEnabled(haveResult);

        } else if (event.getName().equals(ProjectRoomObject.BUILDING)) {
            _ctrl.buildAction.setEnabled(!_roomObj.building);

        // the project has been loaded or changed. tell the project panel to make the project tree
        } else if (event.getName().equals(ProjectRoomObject.PROJECT)) {
            _projectPanel.setProject(_roomObj);
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

    /** Initialize the file types that can be created */
    protected void initFileTypes ()
    {
        _createableFileTypes = new ArrayList<SwiftlyDocumentEditor.FileTypes>();
        _createableFileTypes.add(
            new FileTypes(_msgs.get("m.filetypes." + MediaDesc.TEXT_ACTIONSCRIPT),
                          MediaDesc.mimeTypeToString(MediaDesc.TEXT_ACTIONSCRIPT)));
        _createableFileTypes.add(new FileTypes(_msgs.get("m.filetypes." + MediaDesc.TEXT_PLAIN),
                                               MediaDesc.mimeTypeToString(MediaDesc.TEXT_PLAIN)));
    }

    protected void showPreview ()
    {
        String resultUrl = _roomObj.result.getBuildResultURL();
        try {
            URL url = new URL(resultUrl);
            if (_roomObj.project.projectType == Item.AVATAR) {
                url = new URL(url, _avatarViewerPath + URLEncoder.encode(url.toString(), "UTF-8"));
            }
            _ctx.getAppletContext().showDocument(url, "_blank");

        } catch (Exception e) {
            log.warning("Failed to display results [url=" + resultUrl + ", error=" + e + "].");
            // TODO: we sent ourselves a bad url? display an error dialog?
        }
    }

    protected void exportResult ()
    {
        File out = null;
        try {
            URL url = new URL(_roomObj.result.getBuildResultURL());
            out = new File(System.getProperty("user.home") + File.separator + "Desktop" +
                           File.separator + _roomObj.project.getOutputFileName());
            FileOutputStream ostream = new FileOutputStream(out);
            IOUtils.copy(url.openStream(), ostream);
            ostream.close();
            consoleMessage(_msgs.get("m.result_exported", out.getPath()));

        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to save build results [to=" + out + "].", e);
            // TODO: report an error
        }
    }

    /** Displays the build result on the console */
    protected void displayBuildResult ()
    {
        boolean didSucceed = _roomObj.result.buildSuccessful();
        for (CompilerOutput output : _roomObj.result.getOutput()) {
            FlexCompilerOutput flexOut = (FlexCompilerOutput)output;
            if (didSucceed) {
                consoleMessage(flexOut.toString());
            } else {
                consoleErrorMessage(flexOut.toString());
            }
        }
    }

    /** A list of files that can be created by this SwiftlyDocumentEditor. */
    protected ArrayList<SwiftlyDocumentEditor.FileTypes> _createableFileTypes;

    protected SwiftlyContext _ctx;
    protected ProjectRoomController _ctrl;
    protected MessageBundle _msgs;
    protected ProjectRoomObject _roomObj;

    protected JSplitPane _contentPane;
    protected JSplitPane _rightPane;
    protected TabbedEditor _editorTabs;
    protected Console _console;
    protected EditorToolBar _toolbar;
    protected ProjectPanel _projectPanel;
    protected Action _previewAction, _exportAction;

    protected String _avatarViewerPath;
}

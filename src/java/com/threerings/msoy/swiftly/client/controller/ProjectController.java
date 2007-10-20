
//
// $Id$

package com.threerings.msoy.swiftly.client.controller;

import java.awt.event.ActionEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.threerings.msoy.item.data.all.MediaDesc;

import com.threerings.msoy.swiftly.client.ShutdownNotifier;
import com.threerings.msoy.swiftly.client.SwiftlyApplication;
import com.threerings.msoy.swiftly.client.SwiftlyContext;
import com.threerings.msoy.swiftly.client.Translator;
import com.threerings.msoy.swiftly.client.event.AccessControlListener;
import com.threerings.msoy.swiftly.client.event.OccupantListener;
import com.threerings.msoy.swiftly.client.event.PathElementListener;
import com.threerings.msoy.swiftly.client.event.SwiftlyDocumentListener;
import com.threerings.msoy.swiftly.client.model.DocumentModel;
import com.threerings.msoy.swiftly.client.model.DocumentModelDelegate;
import com.threerings.msoy.swiftly.client.model.ProjectModel;
import com.threerings.msoy.swiftly.client.model.ProjectModelDelegate;
import com.threerings.msoy.swiftly.client.model.RequestId;
import com.threerings.msoy.swiftly.client.model.RequestIdFactory;
import com.threerings.msoy.swiftly.client.view.AccessControlComponent;
import com.threerings.msoy.swiftly.client.view.CompilerOutputComponent;
import com.threerings.msoy.swiftly.client.view.CompilerOutputGutter;
import com.threerings.msoy.swiftly.client.view.Console;
import com.threerings.msoy.swiftly.client.view.ConsoleView;
import com.threerings.msoy.swiftly.client.view.CreateFileDialog;
import com.threerings.msoy.swiftly.client.view.EditorToolBar;
import com.threerings.msoy.swiftly.client.view.EditorToolBarView;
import com.threerings.msoy.swiftly.client.view.ImageEditor;
import com.threerings.msoy.swiftly.client.view.ImageEditorView;
import com.threerings.msoy.swiftly.client.view.PositionLocation;
import com.threerings.msoy.swiftly.client.view.ProgressBar;
import com.threerings.msoy.swiftly.client.view.ProgressBarView;
import com.threerings.msoy.swiftly.client.view.ProjectPanel;
import com.threerings.msoy.swiftly.client.view.ProjectPanelView;
import com.threerings.msoy.swiftly.client.view.SwiftlyWindow.AttachCallback;
import com.threerings.msoy.swiftly.client.view.SwiftlyWindow;
import com.threerings.msoy.swiftly.client.view.SwiftlyWindowView;
import com.threerings.msoy.swiftly.client.view.TabbedEditor;
import com.threerings.msoy.swiftly.client.view.TabbedEditorComponent;
import com.threerings.msoy.swiftly.client.view.TabbedEditorScroller;
import com.threerings.msoy.swiftly.client.view.TabbedEditorView;
import com.threerings.msoy.swiftly.client.view.TextEditor;
import com.threerings.msoy.swiftly.client.view.TextEditorView;
import com.threerings.msoy.swiftly.data.BuildResult;
import com.threerings.msoy.swiftly.data.CompilerOutput.Level;
import com.threerings.msoy.swiftly.data.CompilerOutput;
import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.data.PathElementTreeNode;
import com.threerings.msoy.swiftly.data.ProjectTreeModel;
import com.threerings.msoy.swiftly.data.SwiftlyDocument;
import com.threerings.msoy.swiftly.data.SwiftlyImageDocument;
import com.threerings.msoy.swiftly.data.SwiftlyTextDocument;

/**
 * Acts as a controller for ProjectModel and DocumentModel data.
 */
public class ProjectController
    implements SwiftlyDocumentEditor, PathElementEditor, SwiftlyDocumentListener,
               PathElementListener, OccupantListener, TreeModelListener,
               TreeSelectionListener, AccessControlListener, EditorActionProvider,
               DocumentModelDelegate, ProjectModelDelegate, DocumentUpdateDispatcher,
               DocumentContentListener, ShutdownNotifier, AttachCallback
{
    public ProjectController (ProjectModel projModel, DocumentModel docModel, SwiftlyContext ctx)
    {
        _projModel = projModel;
        _docModel = docModel;
        _app = ctx.getApplication();
        _translator = ctx.getTranslator();

        // setup the actions vended by the controller
        // TODO: create an ActionFactory that gets the translator in the constructor
        _showConsoleAction = new EditorAction(ActionResource.SHOW_CONSOLE, _translator) {
            public void actionPerformed (ActionEvent e) {
                _console.displayConsole();
            }
        };

        _addFileAction = new EditorAction (ActionResource.ADD_FILE, _translator) {
            public void actionPerformed (ActionEvent e) {
                addDocument();
            }
        };

        _uploadFileAction = new EditorAction (ActionResource.UPLOAD_FILE, _translator) {
            public void actionPerformed (ActionEvent e) {
                try {
                    _app.showURL(new URL(
                        "javascript:showUploadDialog(" + _projModel.getProjectId() + ")"));
                } catch (MalformedURLException mue) {
                    // we shall not give ourselves a bad URL
                }
            }
        };

        _renameFileAction = new EditorAction (ActionResource.RENAME_FILE, _translator) {
            public void actionPerformed (ActionEvent e) {
                _projectPanel.renameCurrentElement();
            }
        };

        _deleteFileAction = new EditorAction (ActionResource.DELETE_FILE, _translator) {
            public void actionPerformed (ActionEvent e) {
                deleteDocument(_projectPanel.getSelectedPathElement());
            }
        };

        _closeCurrentTabAction = new EditorAction (ActionResource.CLOSE_CURRENT_TAB, _translator) {
            public void actionPerformed (ActionEvent e) {
                _editorTabs.closeCurrentTab();
            }
        };

        _buildAction = new EditorAction (ActionResource.BUILD, _translator) {
            public void actionPerformed (ActionEvent e) {
                buildStarted();
                _projModel.buildProject(_requestFactory.generateId(), ProjectController.this);
            }
        };

        _buildExportAction = new EditorAction (ActionResource.BUILD_EXPORT, _translator) {
            public void actionPerformed (ActionEvent e) {
                buildStarted();
                _projModel.buildAndExportProject(
                    _requestFactory.generateId(), ProjectController.this);
            }
        };

        // register the controller as a listener to the various models
        _projModel.addAccessControlListener(this);
        _docModel.addPathElementListener(this);
        _docModel.addSwiftlyDocumentListener(this);
        _docModel.addDocumentContentsListener(this);

        // initialize the file types this SwiftlyDocumentEditor knows how to create
        initFileTypes();

        // setup the views
        _treeModel = new ProjectTreeModel(
            this, _docModel.getRootElement(), _docModel.getPathElements());
        ProjectPanelView projectPanel = new ProjectPanelView(this, this, _translator, _treeModel);
        ProgressBarView progress = new ProgressBarView();
        EditorToolBarView toolbar = new EditorToolBarView(this, _translator, progress);
        TabbedEditorView editorTabs = new TabbedEditorView();
        SwiftlyWindowView window =
            new SwiftlyWindowView(projectPanel, toolbar, editorTabs, ctx, _translator, this);
        _notifier = _app.createNotifier();
        _console = new ConsoleView(_translator, this);

        // register the controller as a listener for tree model changes
        _treeModel.addTreeModelListener(this);

        // store references to the various views for later use
        _projectPanel = projectPanel;
        _editorTabs = editorTabs;
        _toolbar = toolbar;
        _progress = progress;
        _window = window;

        addAccessControlComponent(_toolbar, toolbar);

        // handle the initial state for access control
        if (_projModel.haveWriteAccess()) {
            writeAccessGranted();

        } else {
            readOnlyAccessGranted();
        }

        // set the initial state of the actions
        updateActions();

        // register the controller as a ShutdownNotifier on the application
        _app.addShutdownNotifier(this);

        // attach and display the SwiftlyWindow to the SwiftlyApplication
        _app.attachWindow(window);
    }

    // from PathElementEditor
    public void openPathElement (PathElement element)
    {
        // If a tab is already opened for this PathElment, then select it and be done.
        if (_editorTabs.selectTab(element) != null) {
            return;
        }

        if (element.getType() == PathElement.Type.FILE) {
            openDocument(element);
        }
    }

    // from PathElementEditor
    public void openPathElementAt (PathElement element, PositionLocation location)
    {
        // If a tab is already opened for this PathElment, then select it and go to the location.
        TabbedEditorComponent tab;
        if ((tab = _editorTabs.selectTab(element)) != null) {
            tab.gotoLocation(location);
            return;
        }

        if (element.getType() == PathElement.Type.FILE) {
            openDocument(element, location);
        }
    }

    // from PathElementEditor
    public void renamePathElement (final PathElement element, final String newName)
    {
        _docModel.renamePathElement(_requestFactory.generateId(), element, newName, this);
    }

    // from OccupantListener
    public void userEntered (String username)
    {
        _notifier.showInfo(_translator.xlate("m.user_entered", username));
        _window.showChatPanel();
    }

    // from OccupantListener
    public void userLeft (String username)
    {
        _notifier.showInfo(_translator.xlate("m.user_left", username));
        if (_projModel.occupantCount() == 1) {
            _window.hideChatPanel();
        }
    }

    // from DocumentModelDelegate
    public void documentLoaded (RequestId requestId, SwiftlyDocument doc, PositionLocation location)
    {
        doc.loadInEditor(this, location);
    }

    // from DocumentModelDelegate
    public void documentLoadingFailed (RequestId requestId, PathElement element,
        DocumentModelDelegate.FailureCode error)
    {
        _notifier.showError(_translator.xlate(error));
    }

    // from DocumentModelDelegate
    public void documentAdditionFailed (RequestId requestId, NewPathElement newElement,
        DocumentModelDelegate.FailureCode error)
    {
        _notifier.showError(_translator.xlate(error));
    }

    // from DocumentModelDelegate
    public void documentAdded (RequestId requestId, NewPathElement newElement)
    {
        // TODO: feedback in the ProjectPanel, highlight the new document? Also, show a different
        // message here rather than relying on the set listener
    }

    // from DocumentModelDelegate
    public void directoryAdditionFailed (RequestId requestId, PathElement element,
        DocumentModelDelegate.FailureCode error)
    {
        _notifier.showError(_translator.xlate(error));
    }

    // from DocumentModelDelegate
    public void directoryAdded (RequestId requestId, PathElement element)
    {
        // TODO Auto-generated method stub
    }

    // from DocumentModelDelegate
    public void textDocumentUpdateFailed (RequestId requestId, SwiftlyTextDocument doc,
        DocumentModelDelegate.FailureCode error)
    {
        _notifier.showError(_translator.xlate(error));
    }

    // from DocumentModelDelegate
    public void textDocumentUpdated (RequestId requestId, SwiftlyTextDocument doc)
    {
        // nada
    }

    // from DocumentModelDelegate
    public void documentDeleteFailed (RequestId requestId, PathElement element,
        DocumentModelDelegate.FailureCode error)
    {
        _notifier.showError(_translator.xlate(error));
    }

    // from DocumentModelDelegate
    public void documentDeleted (RequestId requestId, PathElement element)
    {
        _editorTabs.closePathElementTab(element);

        // TODO: show a confirm message here, don't just rely on the set listener
        // update the actions as the tree will not have a selection anymore
        updateActions();

        _openTextEditors.remove(element);
        _openImageEditors.remove(element);
        _compilerOutputComponents.remove(element);
    }

    // from DocumentModelDelegate
    public void pathElementRenameFailed (RequestId requestId, PathElement element,
        DocumentModelDelegate.FailureCode error)
    {
        _notifier.showError(_translator.xlate(error));
    }

    // from DocumentModelDelegate
    public void pathElementRenamed (RequestId requestId, PathElement element)
    {
        // TODO Auto-generated method stub
    }

    // from ProjectModelDelegate
    public void buildRequestFailed (RequestId requestId, ProjectModelDelegate.FailureCode error)
    {
        buildFinished();
        _notifier.showError(_translator.xlate(error));
    }

    // from ProjectModelDelegate
    public void buildRequestSucceeded (RequestId requestId, BuildResult result)
    {
        buildFinished();
        handleNewBuildResult(result);
    }

    // from ProjectModelDelegate
    public void buildAndExportRequestFailed (RequestId requestId,
        ProjectModelDelegate.FailureCode error)
    {
        buildFinished();
        _notifier.showError(_translator.xlate(error));
    }

    // from ProjectModelDelegate
    public void buildAndExportRequestSucceeded (RequestId requestId, BuildResult result)
    {
        buildFinished();
        _notifier.showInfo(_translator.xlate("m.build_export_succeeded"));
        handleNewBuildResult(result);
    }

    // from interface TreeModelListener
    public void treeNodesChanged (TreeModelEvent e)
    {
        Object[] children = e.getChildren();
        // children is null if the root node changed
        if (children == null) {
            return;
        }

        // this assumes single selection mode for the tree
        PathElementTreeNode node = (PathElementTreeNode)children[0];
        if (node.getElement().getType() == PathElement.Type.FILE) {
            _editorTabs.updateTabTitleAt(node.getElement());
        }
    }

    // from interface TreeModelListener
    public void treeNodesInserted (TreeModelEvent e)
    {
        // nada
    }

    // from interface TreeModelListener
    public void treeNodesRemoved (TreeModelEvent e)
    {
        // nada
    }

    // from interface TreeModelListener
    public void treeStructureChanged (TreeModelEvent e)
    {
        // nada
    }

    // from interface TreeSelectionListener
    public void valueChanged (TreeSelectionEvent e)
    {
        // the selection has changed, update any actions
        updateActions();

        PathElement element = _projectPanel.getSelectedPathElement();
        if (element == null) {
            return;
        }

        openPathElement(element);
    }

    // from AccessControlListener
    public void readOnlyAccessGranted ()
    {
        updateActions();

        for (AccessControlComponent component : _accessControlComponents) {
            component.showReadOnlyAccess();
        }
    }

    // from AccessControlListener
    public void writeAccessGranted ()
    {
        updateActions();

        for (AccessControlComponent component : _accessControlComponents) {
            component.showWriteAccess();
        }
    }

    // from PathElementListener
    public void elementAdded (PathElement element)
    {
        _treeModel.elementAdded(element);
        // inform the user that an element was added
        _notifier.showInfo(_translator.xlate("m.element_added", element.getName()));
    }

    // from PathElementListener
    public void elementRemoved (PathElement element)
    {
        _treeModel.elementRemoved(element);
        // inform the user that an element was deleted
        _notifier.showInfo(_translator.xlate("m.element_deleted", element.getName()));
        // update the actions as the tree may have lost the element it had selected
        updateActions();
    }

    // from PathElementListener
    public void elementUpdated (PathElement element)
    {
        _treeModel.elementUpdated(element);
        _editorTabs.updateTabTitleAt(element);
        // inform the user that an element was updated
        _notifier.showInfo(_translator.xlate("m.element_updated", element.getName()));
    }

    // from SwiftlyDocumentListener
    public void documentAdded (SwiftlyDocument doc)
    {
        // nada
    }

    // from SwiftlyDocumentListener
    public void documentRemoved (SwiftlyDocument doc)
    {
        // nada
    }

    // from SwiftlyDocumentListener
    public void documentUpdated (SwiftlyTextDocument doc)
    {
        TextEditor editor = _openTextEditors.get(doc.getPathElement());

        // if a TextEditor is working on this document, tell it to load the updated document
        if (editor == null) {
            return;
        }
        editor.loadDocument(doc);
    }

    // from SwiftlyDocumentListener
    public void documentUpdated (SwiftlyImageDocument doc)
    {
        ImageEditor editor = _openImageEditors.get(doc.getPathElement());

        // if an ImageEditor is working on this document, tell it to load the updated document
        if (editor == null) {
            return;
        }
        editor.loadDocument(doc);
    }

    // from DocumentContentsListener
    public void documentContentsChanged (SwiftlyTextDocument doc) {
        // lookup the TextEditor working on this document
        TextEditor editor = _openTextEditors.get(doc.getPathElement());

        // if a TextEditor is working on this document, tell it to refresh
        if (editor == null) {
            return;
        }
        editor.documentTextChanged();
    }

    // from DocumentUpdateDispatcher
    public void documentTextChanged (SwiftlyTextDocument doc, String text)
    {
        _docModel.updateTextDocument(_requestFactory.generateId(), doc, text, this);
    }

    // from EditorActionProvider
    public Action getAddFileAction ()
    {
        return _addFileAction;
    }

    // from EditorActionProvider
    public Action getBuildAction ()
    {
        return _buildAction;
    }

    // from EditorActionProvider
    public Action getBuildExportAction ()
    {
        return _buildExportAction;
    }

    // from EditorActionProvider
    public Action getCloseCurrentTabAction ()
    {
        return _closeCurrentTabAction;
    }

    // from EditorActionProvider
    public Action getShowConsoleAction ()
    {
        return _showConsoleAction;
    }

    // from EditorActionProvider
    public Action getDeleteFileAction ()
    {
        return _deleteFileAction;
    }

    // from EditorActionProvider
    public Action getRenameFileAction ()
    {
        return _renameFileAction;
    }

    // from EditorActionProvider
    public Action getUploadFileAction ()
    {
        return _uploadFileAction;
    }

    // from SwiftlyDocumentEditor
    public void editTextDocument (SwiftlyTextDocument document, PositionLocation location)
    {
        PathElement element = document.getPathElement();
        TextEditorView textEditor = new TextEditorView(this, _translator, document, this);
        TabbedEditorScroller scroller = new TabbedEditorScroller(textEditor, element);
        CompilerOutputGutter gutter = new CompilerOutputGutter(textEditor, scroller);
        scroller.setRowHeaderView(gutter);

        // disable editing if the user does not have write access on the project
        if (_projModel.haveWriteAccess()) {
            textEditor.showWriteAccess();
        } else {
            textEditor.showReadOnlyAccess();
        }

        // display any existing compiler output in the gutter
        displayCurrentCompilerOutput(element, gutter);

        // add the tab
        _editorTabs.addEditorTab(scroller, element);

        // goto the starting location
        textEditor.gotoLocation(location);

        // add the various components to the removal/tracking collections
        addTextEditor(element, textEditor);
        addCompilerOutputComponent(element, gutter, gutter);
        addAccessControlComponent(textEditor, textEditor);
    }

    // from SwiftlyDocumentEditor
    public void editImageDocument (SwiftlyImageDocument document)
    {
        PathElement element = document.getPathElement();
        ImageEditorView imageEditor = new ImageEditorView(document);
        TabbedEditorScroller scroller = new TabbedEditorScroller(imageEditor, element);

        // add the tab
        _editorTabs.addEditorTab(scroller, element);

        // add the ImageEditor to the removal/tracking collections
        addImageEditor(element, imageEditor);
    }

    // from SwiftlyDocumentEditor
    public List<FileTypes> getCreateableFileTypes ()
    {
        return _createableFileTypes;
    }

    // from ShutdownNotifier
    public void shuttingDown ()
    {
        // destroy the console window
        _console.destroy();
    }

    // from AttachCallback
    public void windowDisplayed ()
    {
        // start with the chat panel hidden if no one else is in the room
        if (_projModel.occupantCount() > 1) {
            _window.showChatPanel();
        } else {
            _window.hideChatPanel();
        }
    }

    /**
     * Requests that the SwiftlyDocument associated with the given PathElement be loaded and
     * displayed in the SwiftlyDocumentEditor.
     */
    private void openDocument (PathElement element)
    {
        // otherwise ask that the element be opened at the starting position
        openDocument(element, new PositionLocation(1, 1, false));
    }

    /**
     * Requests that the SwiftlyDocument associated with the given PathElement be loaded and
     * displayed in the SwiftlyDocumentEditor at the the provided PositionLocation.
     */
    private void openDocument (PathElement element, PositionLocation location)
    {
        // ask the model to load the document. the delegate will display the document.
        _docModel.loadDocument(_requestFactory.generateId(), element, location, this);
    }

    /**
     * Adds a SwiftlyDocument to the project.
     */
    private void addDocument ()
    {
        PathElement parent = _projectPanel.getCurrentParent();
        if (parent == null) {
            _notifier.showError(_translator.xlate("e.document_no_parent"));
        }

        CreateFileDialog dialog = _window.showCreateFileDialog(this);
        if (!dialog.isValid() || dialog.wasCancelled()) {
            // TODO: if not valid display some kind of error in the dialog itself
            return; // if the user hit cancel or didn't enter valid data, do nothing
        }

        NewPathElement newElement = new NewPathElement(
            dialog.getName(), parent, dialog.getMimeType());

        // report an error if this path already exists
        if (_docModel.pathElementExists(newElement.name, parent)) {
            _notifier.showError(_translator.xlate("e.document_already_exists"));
            return;
        }

        _docModel.addDocument(_requestFactory.generateId(), newElement, this);
    }

    /**
     * Adds a directory to the project.
     */
    @Deprecated // not yet used, possibly remove for now
    private void addDirectory ()
    {
        PathElement parent = _projectPanel.getCurrentParent();
        if (parent == null) {
            _notifier.showError(_translator.xlate("e.directory_no_parent"));
        }

        // prompt the user for the name of the path element
        String name = _window.showSelectPathElementNameDialog(PathElement.Type.DIRECTORY);
        if (name == null) {
            return; // if the user hit cancel do no more
        }
        PathElement element = PathElement.createDirectory(name, parent);
        // _docModel.addPathElement(element, this);
    }

    /**
     * Removes a document [referenced by a path element] from the project.
     */
    private void deleteDocument (final PathElement element)
    {
        // Confirm the user actually wants to delete this PathElement
        if (!_window.showConfirmDialog(_translator.xlate("m.dialog.confirm_delete",
            element.getName()))) {
            return;
        }

        _docModel.deleteDocument(_requestFactory.generateId(), element, this);
    }

    /**
     * Disable the build actions while a build is happening.
     */
    private void buildStarted ()
    {
        _buildAction.setEnabled(false);
        _buildExportAction.setEnabled(false);
        _progress.showProgress(_projModel.getLastBuildTime());
    }

    /**
     * Enable the build actions when a build is finished.
     */
    private void buildFinished ()
    {
        if (_projModel.haveWriteAccess()) {
            _buildAction.setEnabled(true);
            _buildExportAction.setEnabled(true);
        }
        _progress.stopProgress();
    }

    /**
     * Handle displaying a new BuildResult.
     */
    private void handleNewBuildResult (BuildResult result)
    {
        if (result.buildSuccessful()) {
            _notifier.showInfo(_translator.xlate("m.build_succeeded"));

        } else {
            _notifier.showError(_translator.xlate("m.build_failed"));
        }

        _console.clearConsole();
        clearCompilerOutputComponents();

        for (CompilerOutput line : result.getOutput()) {
            if (line.getLevel() == Level.IGNORE || line.getLevel() == Level.UNKNOWN) {
                continue;
            }

            if (line.hasPath()) {
                PathElement element = _docModel.findPathElementByPath(line.getPath());
                Set<CompilerOutputComponent> set = _compilerOutputComponents.get(element);
                if (set != null) {
                    for (CompilerOutputComponent comp : set) {
                        comp.displayCompilerOutput(line);
                    }
                }
                _console.appendCompilerOutput(line, element);

            } else {
                _console.appendCompilerOutput(line);
            }
        }
    }

    /**
     * Registers an AccessControlComponent as being displayed.
     */
    private void addAccessControlComponent (AccessControlComponent comp, JComponent jcomp)
    {
        new ComponentSetHandler<AccessControlComponent>(comp, jcomp, _accessControlComponents);
    }

    /**
     * Registers a TextEditor as displaying the given PathElement.
     */
    private void addTextEditor (PathElement element, TextEditorView editor)
    {
        new ComponentMapHandler<PathElement, TextEditor>(
                editor, editor, element, _openTextEditors);
    }

    /**
     * Registers an ImageEditor as displaying the given PathElement.
     */
    private void addImageEditor (PathElement element, ImageEditorView editor)
    {
        new ComponentMapHandler<PathElement, ImageEditor>(
                editor, editor, element, _openImageEditors);
    }

    /**
     * Registers a CompilerOutputComponent as displaying the given PathElement.
     */
    private void addCompilerOutputComponent (PathElement element, CompilerOutputComponent comp,
                                             JComponent jcomp)
    {
        Set<CompilerOutputComponent> set = _compilerOutputComponents.get(element);
        if (set == null) {
            set = Sets.newHashSet();
            _compilerOutputComponents.put(element, set);
        }

        new ComponentSetHandler<CompilerOutputComponent>(comp, jcomp, set);
    }

    /**
     * Displays the current CompilerOutput for the given PathElement in the supplied
     * CompilerOutputComponent.
     */
    private void displayCurrentCompilerOutput (PathElement element, CompilerOutputComponent comp)
    {
        if (_projModel.getLastBuildResult() == null) {
            return;
        }

        for (CompilerOutput line : _projModel.getLastBuildResult().getOutputForPath(element)) {
            comp.displayCompilerOutput(line);
        }
    }

    /**
     * Asks all CompilerOutputComponents to clear any output they are currently displaying.
     */
    private void clearCompilerOutputComponents ()
    {
        for (Set<CompilerOutputComponent> set : _compilerOutputComponents.values()) {
            for (CompilerOutputComponent comp : set) {
                comp.clearCompilerOutput();
            }
        }
    }

    /**
     * Called whenever an event should change which Actions are enabled.
     */
    private void updateActions ()
    {
        // close current tab is always enabled
        _closeCurrentTabAction.setEnabled(true);

        // everyone can use the console.
        _showConsoleAction.setEnabled(true);

        // disable all remaining actions
        _buildAction.setEnabled(false);
        _buildExportAction.setEnabled(false);
        _addFileAction.setEnabled(false);
        _uploadFileAction.setEnabled(false);
        _renameFileAction.setEnabled(false);
        _deleteFileAction.setEnabled(false);
        _projectPanel.disableEditing();

        // if the user does not have write access, they can use no more of the actions
        if (!_projModel.haveWriteAccess()) {
            return;
        }

        // enable the build actions
        _buildAction.setEnabled(true);
        _buildExportAction.setEnabled(true);

        // enable the upload action which works even if no element is selected
        // [even though one day it will probably want to pass the current path element]
        _uploadFileAction.setEnabled(true);

        // grab the currently selected path element in the project panel
        PathElement element = _projectPanel.getSelectedPathElement();

        // if no element is selected, leave the rest of the actions disabled.
        if (element == null) {
            return;
        }

        // enable the add file actions since an element is selected to act as the parent
        _addFileAction.setEnabled(true);

        // handle the rename action
        if (_docModel.isRenameable(element)) {
            _renameFileAction.setEnabled(true);
            _projectPanel.enableEditing();
        }

        // handle the delete action
        if (_docModel.isDeleteable(element)) {
            _deleteFileAction.setEnabled(true);
        }
    }

    /** Initialize the file types that can be created. */
    private void initFileTypes ()
    {
        _createableFileTypes.add(new FileTypes(
            _translator.xlate("m.filetypes." + MediaDesc.TEXT_ACTIONSCRIPT),
            MediaDesc.mimeTypeToString(MediaDesc.TEXT_ACTIONSCRIPT)));
        _createableFileTypes.add(new FileTypes(
            _translator.xlate("m.filetypes." + MediaDesc.TEXT_PLAIN),
            MediaDesc.mimeTypeToString(MediaDesc.TEXT_PLAIN)));
    }

    /** A list of files that can be created by this SwiftlyDocumentEditor. */
    private final List<SwiftlyDocumentEditor.FileTypes> _createableFileTypes =
        new ArrayList<SwiftlyDocumentEditor.FileTypes>();

    /** Maps PathElements to the TextEditors working on them. */
    private final Map<PathElement, TextEditor> _openTextEditors = Maps.newHashMap();

    /** Maps PathElements to the ImageEditors working on them. */
    private final Map<PathElement, ImageEditor> _openImageEditors = Maps.newHashMap();

    /**
     * Maps PathElements to the components interested in displaying CompilerOutput associated with
     * that PathElement.
     */
    private final Map<PathElement, Set<CompilerOutputComponent>> _compilerOutputComponents =
        Maps.newHashMap();

    /** The set of all components which can display access permissions. */
    private final Set<AccessControlComponent> _accessControlComponents = Sets.newHashSet();

    /** A factory for generating new RequestIds. */
    private final RequestIdFactory _requestFactory = new RequestIdFactory();

    /** The SwiftlyApplication provides a few useful services from the root view component. */
    private final SwiftlyApplication _app;

    /** The view components driven by this controller. */
    private final SwiftlyWindow _window;
    private final ProjectPanel _projectPanel;
    private final ProjectTreeModel _treeModel;
    private final TabbedEditor _editorTabs;
    private final EditorToolBar _toolbar;
    private final Console _console;
    private final ProgressBar _progress;

    /** The Actions provided by this controller to the various views. */
    private final Action _buildAction;
    private final Action _buildExportAction;
    private final Action _showConsoleAction;
    private final Action _closeCurrentTabAction;
    private final Action _addFileAction;
    private final Action _uploadFileAction;
    private final Action _renameFileAction;
    private final Action _deleteFileAction;

    /** The models used by this controller. */
    private final ProjectModel _projModel;
    private final DocumentModel _docModel;

    /** Utility classes used by this controller. */
    private final Translator _translator;
    private final PassiveNotifier _notifier;
}

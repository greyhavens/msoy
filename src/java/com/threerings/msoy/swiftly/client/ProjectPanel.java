//
// $Id$

package com.threerings.msoy.swiftly.client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.data.PathElementTreeNode;
import com.threerings.msoy.swiftly.data.ProjectRoomObject;
import com.threerings.msoy.swiftly.data.ProjectTreeModel;
import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.msoy.swiftly.util.SwiftlyContext;
import com.threerings.presents.client.InvocationService.ConfirmListener;
import com.threerings.presents.client.InvocationService.InvocationListener;
import com.threerings.util.MessageBundle;

public class ProjectPanel extends JPanel
    implements TreeSelectionListener, TreeModelListener, AccessControlListener
{
    public ProjectPanel (SwiftlyContext ctx, SwiftlyEditor editor)
    {
        super(new BorderLayout());
        _ctx = ctx;
        _editor = editor;
        _msgs = _ctx.getMessageManager().getBundle(SwiftlyCodes.SWIFTLY_MSGS);

        // setup the actions
        _addFileAction = createAddFileAction();
        _uploadFileAction = createUploadFileAction();
        _deleteFileAction = createDeleteFileAction();
        _renameFileAction = createRenameFileAction();

        // setup the toolbar
        _toolbar.add(createButton(_addFileAction));
        _toolbar.add(createButton(_uploadFileAction));
        _toolbar.add(createButton(_deleteFileAction));
        _toolbar.setFloatable(false);

        // add the project panel as an access control listener
        _editor.addAccessControlListener(this);

        add(_toolbar, BorderLayout.PAGE_START);
        setupPopup();
        add(_scrollPane, BorderLayout.CENTER);
    }

    /**
     * Initializes and adds the {@link ProjectTreeModel} to the panel.
     * @param roomObj the {@link ProjectRoomObject} used as the root node.
     */
    public void setProject (ProjectRoomObject roomObj)
    {
        _roomObj = roomObj;
        _treeModel = new ProjectTreeModel(roomObj, this, _ctx);
        _treeModel.addTreeModelListener(this);

        _tree = new JTree(_treeModel);
        // XXX disable dragging until the rest of the support can be wired up
        // _tree.setDragEnabled(true);
        _tree.setEditable(true);
        _tree.setShowsRootHandles(false);
        _tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        _tree.addTreeSelectionListener(this);
        _tree.addMouseListener(new PopupListener());
        _tree.setCellRenderer(new ProjectTreeCellRenderer());
        ToolTipManager.sharedInstance().registerComponent(_tree);

        _scrollPane.getViewport().setView(_tree);
        updateActions();
    }

    // from AccessControlListener
    public void writeAccessGranted ()
    {
        updateActions();
    }

    // from AccessControlListener
    public void readOnlyAccessGranted ()
    {
        updateActions();
    }

    // from interface TreeSelectionListener
    public void valueChanged (TreeSelectionEvent e)
    {
        PathElementTreeNode node = (PathElementTreeNode) _tree.getLastSelectedPathComponent();
        if (node == null) {
            return;
        }

        // the selection has changed, update any actions
        updateActions();

        PathElement element = getSelectedPathElement();
        if (element.getType() == PathElement.Type.FILE) {
            _editor.openPathElement(element);
        }
    }

    // from interface TreeModelListener
    public void treeNodesChanged (TreeModelEvent e)
    {
        Object[] children = e.getChildren();
        // children is null if the root node changed
        if (children == null) {
            return;
        }

        // TODO we are in single selection mode so only one node will ever change [right?]
        PathElementTreeNode node = (PathElementTreeNode)children[0];
        if (node.getElement().getType() == PathElement.Type.FILE) {
            _editor.pathElementChanged(node.getElement());
        }
    }

    // from interface TreeModelListener
    public void treeNodesInserted (TreeModelEvent e)
    {
        // TODO is it clear this is what we want to happen?
        PathElementTreeNode node = (PathElementTreeNode)e.getChildren()[0];
        _tree.scrollPathToVisible(new TreePath(node.getPath()));
    }

    // from interface TreeModelListener
    public void treeNodesRemoved (TreeModelEvent e)
    {
        // TODO iterate over every child and close any open tabs.
    }

    // from interface TreeModelListener
    public void treeStructureChanged (TreeModelEvent e)
    {
        // nada
    }

    @Override // from JComponent
    public Dimension getPreferredSize ()
    {
        Dimension d = super.getPreferredSize();
        d.width = Math.min(250, d.width);
        return d;
    }

    /**
     * Renames a {@link PathElement} and broadcasts that fact to the server.
     */
    public void renamePathElement (final PathElement element, String newName,
                                   final TreePath path)
    {
        _roomObj.service.renamePathElement(_ctx.getClient(), element.elementId, newName,
            new ConfirmListener () {
            public void requestProcessed () {
                // TODO: update the change on this client?
                // _treeModel.updateNodeName(element, path);
            }
            public void requestFailed (String reason) {
                _ctx.showErrorMessage(_msgs.get(reason));
            }
        });
    }

    protected Action createDeleteFileAction ()
    {
        URL imageURL = getClass().getResource(DELETE_FILE_ICON);
        Action action =
        new AbstractAction(_msgs.get("m.action.delete_file"), new ImageIcon(imageURL)) {
            public void actionPerformed (ActionEvent e) {
                deletePathElement();
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, _msgs.get("m.tooltip.delete_file"));
        return action;
    }

    protected Action createRenameFileAction ()
    {
        URL imageURL = getClass().getResource(RENAME_FILE_ICON);
        Action action =
        new AbstractAction(_msgs.get("m.action.rename_file"), new ImageIcon(imageURL)) {
            public void actionPerformed (ActionEvent e) {
                // tell the tree to start editing the selected path
                _tree.startEditingAtPath(_tree.getSelectionPath());
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, _msgs.get("m.tooltip.rename_file"));
        return action;
    }

    protected Action createAddFileAction ()
    {
        URL imageURL = getClass().getResource(ADD_FILE_ICON);
        Action action =
            new AbstractAction(_msgs.get("m.action.add_file"), new ImageIcon(imageURL)) {
            public void actionPerformed (ActionEvent e) {
                addPathElement(PathElement.Type.FILE);
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, _msgs.get("m.tooltip.add_file"));
        return action;
    }

    protected Action createUploadFileAction ()
    {
        URL imageURL = getClass().getResource(UPLOAD_FILE_ICON);
        Action action =
            new AbstractAction(_msgs.get("m.action.upload_file"), new ImageIcon(imageURL)) {
            public void actionPerformed (ActionEvent e) {
                try {
                    _ctx.getAppletContext().showDocument(new URL("javascript:showUploadDialog(" +
                        _roomObj.project.projectId + ")"));
                } catch (MalformedURLException mue) {
                    // we shall not give ourselves a bad URL
                }
            }
        };
        action.putValue(AbstractAction.SHORT_DESCRIPTION, _msgs.get("m.tooltip.upload_file"));
        return action;
    }

    /* TODO: disabled until backend support is added
    protected Action createAddDirectoryAction ()
    {
        return new AbstractAction(_msgs.get("m.action.add_directory")) {
            public void actionPerformed (ActionEvent e) {
                addPathElement(PathElement.Type.DIRECTORY);
            }
        };
    }
    */

    /**
     * Adds a {@link PathElement} to the tree and broadcasts that fact to the server.
     */
    protected void addPathElement (PathElement.Type type)
    {
        PathElement parentElement = getCurrentParent();

        PathElement element = null;
        if (type == PathElement.Type.DIRECTORY) {
            // prompt the user for the name of the path element
            String name = _editor.showSelectPathElementNameDialog(type);
            if (name == null) {
                return; // if the user hit cancel do no more
            }
            element = PathElement.createDirectory(name, parentElement);
            // TODO: this is clearly broken. no directory is actually being created on the server
            _roomObj.service.addPathElement(_ctx.getClient(), element, new InvocationListener () {
                public void requestFailed (String reason)
                {
                    _ctx.showErrorMessage(_msgs.get(reason));
                }
            });

        } else if (type == PathElement.Type.FILE) {
            CreateFileDialog dialog = _editor.showCreateFileDialog(parentElement);
            if (dialog == null) {
                return; // if the user hit cancel do no more
            }

            String fileName = dialog.getName();
            String mimeType = dialog.getMimeType();

            // report an error if this path already exists
            if (_roomObj.pathElementExists(fileName, parentElement)) {
                _ctx.showErrorMessage(_msgs.get("e.document_already_exists"));
                return;
            }

            _roomObj.service.addDocument(_ctx.getClient(), fileName, parentElement, mimeType,
                new InvocationListener () {
                public void requestFailed (String reason)
                {
                    _ctx.showErrorMessage(_msgs.get(reason));
                }
            });
        }
    }

    /**
     * Removes a {@link PathElement} and broadcasts that fact to the server.
     */
    protected void deletePathElement ()
    {
        final PathElement element = getSelectedPathElement();

        // Confirm the user actually wants to delete this PathElement
        if (!_editor.showConfirmDialog(_msgs.get("m.dialog.confirm_delete", element.getName()))) {
            return;
        }

        if (element.getType() == PathElement.Type.FILE) {
            // close the tab if the pathelement was open in the editor
            _editor.closePathElement(element);
        } else if (element.getType() == PathElement.Type.DIRECTORY) {
            // TODO oh god we have to remove all the tabs associated with this directory
            // soo.. every tab that has a common parent id() ?
        }
        _roomObj.service.deletePathElement(_ctx.getClient(), element.elementId,
            new ConfirmListener () {
            public void requestProcessed () {
                updateActions();
            }
            public void requestFailed (String reason) {
                _ctx.showErrorMessage(_msgs.get(reason));
            }
        });
    }

    protected PathElement getCurrentParent ()
    {
        // the parent element is the directory or project the selected element is in, or if
        // a project or directory is selected, that is the parent element
        PathElement parentElement = getSelectedPathElement();
        if (parentElement.getType() == PathElement.Type.FILE) {
            parentElement = parentElement.getParent();
        }
        return parentElement;
    }

    protected JButton createButton (Action action)
    {
        JButton button = new JButton(action);
        // hide the action text
        button.setText("");
        return button;
    }

    protected void setupPopup ()
    {
        _popup = new JPopupMenu();
        _popup.add(_deleteFileAction);
        _popup.add(_renameFileAction);
    }

    /**
     * Called whenever a user action should change which actions are enabled.
     */
    protected void updateActions ()
    {
        // if the user does not have write access disable all actions
        if (!_roomObj.hasWriteAccess(_ctx.getMember())) {
            _addFileAction.setEnabled(false);
            _uploadFileAction.setEnabled(false);
            _deleteFileAction.setEnabled(false);
            _renameFileAction.setEnabled(false);
            _tree.setEditable(false);
        }

        PathElement element = getSelectedPathElement();
        // if no element is selected, disable everything but file upload
        if (element == null) {
            _addFileAction.setEnabled(false);
            _uploadFileAction.setEnabled(true);
            _deleteFileAction.setEnabled(false);
            _renameFileAction.setEnabled(false);
            _tree.setEditable(false);

        // if the selected node is the root or the project template, disable delete and rename
        } else if (_roomObj.project.getTemplateSourceName().equals(element.getName()) ||
                element.getType() == PathElement.Type.ROOT) {
            _addFileAction.setEnabled(true);
            _uploadFileAction.setEnabled(true);
            _deleteFileAction.setEnabled(false);
            _renameFileAction.setEnabled(false);
            _tree.setEditable(false);

        // else enable all the actions
        } else {
            _addFileAction.setEnabled(true);
            _uploadFileAction.setEnabled(true);
            _deleteFileAction.setEnabled(true);
            _renameFileAction.setEnabled(true);
            _tree.setEditable(true);
        }
    }

    protected PathElementTreeNode getSelectedNode ()
    {
        if (_tree.getSelectionPath() == null) {
            return null;
        }
        return (PathElementTreeNode)_tree.getSelectionPath().getLastPathComponent();
    }

    protected PathElement getSelectedPathElement ()
    {
        if (getSelectedNode() == null) {
            return null;
        }
        return getSelectedNode().getElement();
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
            if (!e.isPopupTrigger()) {
                return;
            }
            TreePath path = _tree.getPathForLocation(e.getX(), e.getY());
            if (path != null) {
                _tree.setSelectionPath(path);
                _popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    /** The location of various icons */
    protected static final String ADD_FILE_ICON = "/rsrc/icons/swiftly/new.gif";
    protected static final String UPLOAD_FILE_ICON = "/rsrc/icons/swiftly/upload.gif";
    protected static final String DELETE_FILE_ICON = "/rsrc/icons/swiftly/delete.gif";
    protected static final String RENAME_FILE_ICON = "/rsrc/icons/swiftly/rename.gif";

    protected SwiftlyContext _ctx;
    protected SwiftlyEditor _editor;
    protected MessageBundle _msgs;
    protected ProjectRoomObject _roomObj;
    protected ProjectTreeModel _treeModel;

    protected JTree _tree;
    protected JToolBar _toolbar = new JToolBar();
    protected Action _addFileAction;
    protected Action _uploadFileAction;
    protected Action _deleteFileAction;
    protected Action _renameFileAction;
    protected JScrollPane _scrollPane = new JScrollPane();
    protected JPopupMenu _popup;
}

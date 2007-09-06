//
// $Id$

package client.swiftly;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import client.util.ClickCallback;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.data.SwiftlyProject;

/**
 * Display a dialog to edit a project used only by the project owner.
 */
public class ProjectEdit extends FlexTable
{
    /**
     * A callback interface for classes that want to know when a project was committed.
     */
    public static interface ProjectEditListener
    {
        public void projectSubmitted (SwiftlyProject project);
    }

    public ProjectEdit (int projectId, ProjectEditListener listener)
    {
        _listener = listener;
        setStyleName("projectEdit");

        loadProject(projectId);
    }

    protected void closeDialog()
    {
        removeFromParent();
        if (_listener != null) {
            _listener.projectSubmitted(_project);
        }
    }

    protected void loadProject (final int projectId)
    {
        CSwiftly.swiftlysvc.loadProject(CSwiftly.ident, projectId, new AsyncCallback() {
            public void onSuccess (Object result) {
                _project = (SwiftlyProject)result;
                // now that we have our project, load our collaborators.
                loadCollaborators();
            }
            public void onFailure (Throwable caught) {
                CSwiftly.log("Loadling project failed projectId=[" + projectId + "]", caught);
                SwiftlyPanel.displayError(CSwiftly.serverError(caught));
            }
        });
    }

    protected void displayProject ()
    {
        int row = 0;
        int col = 0;
        HorizontalPanel cell = new HorizontalPanel();
        cell.add(new Label(CSwiftly.msgs.projectName()));
        TextBox projectName = new TextBox();
        cell.add(projectName);
        projectName.setText(_project.projectName);
        projectName.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                _project.projectName = ((TextBox)sender).getText().trim();
            }
        });

        Label remixable = new Label(CSwiftly.msgs.remixable());
        remixable.addStyleName("LeftSpacer");
        cell.add(remixable);
        cell.add(_remixable = new CheckBox());
        _remixable.setChecked(_project.remixable);
        _remixable.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                _project.remixable = _remixable.isChecked();
            }
        });
        setWidget(row, col++, cell);

        cell = new HorizontalPanel();

        cell.add(new Label(CSwiftly.msgs.collaborators()));
        _collaboratorsList = new ListBox();
        cell.add(_collaboratorsList);

        cell.add(_removeCollaboratorButton = new Button(CSwiftly.msgs.remove(),
            new ClickListener() {
            public void onClick (Widget sender) {
                int tx = _collaboratorsList.getSelectedIndex();
                if (tx == -1) {
                    return;
                }
                MemberName name =
                    (MemberName)_collaborators.get(new Integer(_collaboratorsList.getValue(tx)));
                if (name != null) {
                    removeCollaborator(name);
                }
            }
        }));

        Label friends = new Label(CSwiftly.msgs.friends());
        friends.addStyleName("LeftSpacer");
        cell.add(friends);
        _friendList = new ListBox();
        cell.add(_friendList);

        cell.add(_addFriendButton = new Button(CSwiftly.msgs.add(), new ClickListener() {
            public void onClick (Widget sender) {
                int tx = _friendList.getSelectedIndex();
                if (tx == -1) {
                    return;
                }

                FriendEntry friend =
                    (FriendEntry)_friends.get(new Integer(_friendList.getValue(tx)));
                if (friend != null) {
                    addCollaborator(friend.name);
                }
            }
        }));
        setWidget(row, col++, cell);

        cell = new HorizontalPanel();
        // Submit button
        Button submit = new Button(CSwiftly.msgs.submit());
        cell.add(submit);
        new ClickCallback(submit) {
            public boolean callService () {
                CSwiftly.swiftlysvc.updateProject(CSwiftly.ident, _project, this);
                return true;
            }
            public boolean gotResult (Object result) {
                closeDialog();
                return true;
            }
        };

        // Close button
        cell.add(new Button(CSwiftly.msgs.cancel(), new ClickListener() {
            public void onClick (Widget sender) {
                closeDialog();
            }
        }));
        getFlexCellFormatter().setHorizontalAlignment(row, col, HasAlignment.ALIGN_RIGHT);
        setWidget(row, col, cell);
        col++;
    }

    protected void loadCollaborators ()
    {
        _collaborators = new HashMap();
        CSwiftly.swiftlysvc.getProjectCollaborators(CSwiftly.ident, _project.projectId,
            new AsyncCallback() {
            public void onSuccess (Object result) {
                Iterator iter = ((List)result).iterator();
                while (iter.hasNext()) {
                    MemberName name = (MemberName)iter.next();
                    _collaborators.put(new Integer(name.getMemberId()), name);
                }
                // now that we have our collaborators, load our friends.
                loadFriends();
            }
            public void onFailure (Throwable caught) {
                CSwiftly.log("Listing collaborators failed memberId=[" + CSwiftly.getMemberId() +
                    "]", caught);
                SwiftlyPanel.displayError(CSwiftly.serverError(caught));
            }
        });
    }

    protected void updateCollaboratorsList ()
    {
        _collaboratorsList.clear();
        Iterator iter = _collaborators.values().iterator();

        boolean foundCollaborator = false;
        while (iter.hasNext()) {
            MemberName name = (MemberName)iter.next();
            // don't display the owner in the collaborators list since they cannot be removed
            if (name.getMemberId() == _project.ownerId) {
                continue;
            }
            foundCollaborator = true;
            _collaboratorsList.addItem(name.toString(), String.valueOf(name.getMemberId()));
        }

        if (foundCollaborator) {
            _removeCollaboratorButton.setEnabled(true);
        } else {
            _removeCollaboratorButton.setEnabled(false);
            _collaboratorsList.addItem(CSwiftly.msgs.noCollaborators());
        }
    }

    /**
     * Loads this users current list of friends from the backend.
     */
    protected void loadFriends ()
    {
        _friends = new HashMap();
        CSwiftly.swiftlysvc.getFriends(CSwiftly.ident, new AsyncCallback() {
            public void onSuccess (Object result) {
                Iterator iter = ((List)result).iterator();
                while (iter.hasNext()) {
                    FriendEntry friend = (FriendEntry)iter.next();
                    _friends.put(new Integer(friend.getMemberId()), friend);
                }
                // we now have our collaborators and friends, so display them both. displaying of
                // friends relies on both the collaborators list and friends list so we need them
                // both before displaying.
                displayProject();
                updateCollaboratorsList();
                updateFriendList();
            }
            public void onFailure (Throwable caught) {
                CSwiftly.log(
                    "Listing friends failed memberId=[" + CSwiftly.getMemberId() + "]", caught);
                SwiftlyPanel.displayError(CSwiftly.serverError(caught));
            }
        });
    }

    /**
     * Populate the list box for use by owners when adding collaborators of their project.
     */
    protected void updateFriendList ()
    {
        _friendList.clear();
        Iterator iter = _friends.values().iterator();

        boolean foundFriend = false;
        while (iter.hasNext()) {
            final FriendEntry friend = (FriendEntry)iter.next();
            // do not display friends who are already collaborators
            if (_collaborators.containsKey(new Integer(friend.name.getMemberId()))) {
                continue;
            }
            foundFriend = true;
            _friendList.addItem(friend.name.toString(), String.valueOf(friend.name.getMemberId()));
        }

        if (foundFriend) {
            _addFriendButton.setEnabled(true);
        } else {
            _addFriendButton.setEnabled(false);
            _friendList.addItem(CSwiftly.msgs.noFriends());
        }
    }

    /**
     * Add the indicated member to this project.
     *
     * @param memberId The member to add.
     */
    protected void addCollaborator (final MemberName name)
    {
        CSwiftly.swiftlysvc.joinCollaborators(
            CSwiftly.ident, _project.projectId, name, new AsyncCallback() {
            public void onSuccess (Object result) {
                _collaborators.put(new Integer(name.getMemberId()), name);
                updateCollaboratorsList();
                updateFriendList();
            }
            public void onFailure (Throwable caught) {
                CSwiftly.log("Failed to add collaborator [projectId=" + _project.projectId +
                           ", memberId=" + name.getMemberId() + "]", caught);
                SwiftlyPanel.displayError(CSwiftly.serverError(caught));
            }
        });
    }

    /**
     * Remove the indicated member from this project.
     *
     * @param memberId The member to remove.
     */
    protected void removeCollaborator (final MemberName name)
    {
        CSwiftly.swiftlysvc.leaveCollaborators(
            CSwiftly.ident, _project.projectId, name, new AsyncCallback() {
            public void onSuccess (Object result) {
                _collaborators.remove(new Integer(name.getMemberId()));
                updateCollaboratorsList();
                updateFriendList();
            }
            public void onFailure (Throwable caught) {
                CSwiftly.log("Failed to remove collaborator [projectId=" + _project.projectId +
                             ", memberId=" + name.getMemberId() + "]", caught);
                SwiftlyPanel.displayError(CSwiftly.serverError(caught));
            }
        });
    }

    protected SwiftlyProject _project;
    protected ProjectEditListener _listener;
    protected HashMap _collaborators;
    protected HashMap _friends;
    protected Button _addFriendButton;
    protected Button _removeCollaboratorButton;

    protected CheckBox _remixable;
    protected ListBox _collaboratorsList;
    protected ListBox _friendList;
}

//
// $Id$

package client.swiftly;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import client.shell.ShellMessages;
import client.util.ClickCallback;

import com.google.gwt.core.client.GWT;
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
import com.threerings.msoy.swiftly.data.all.SwiftlyProject;

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
        void projectSubmitted (SwiftlyProject project);
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
        CSwiftly.swiftlysvc.loadProject(projectId, new AsyncCallback<SwiftlyProject>() {
            public void onSuccess (SwiftlyProject project) {
                _project = project;
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
                MemberName name = _collaborators.get(new Integer(_collaboratorsList.getValue(tx)));
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

                FriendEntry friend = _friends.get(new Integer(_friendList.getValue(tx)));
                if (friend != null) {
                    addCollaborator(friend.name);
                }
            }
        }));
        setWidget(row, col++, cell);

        cell = new HorizontalPanel();
        // Submit button
        Button submit = new Button(_cmsgs.change());
        cell.add(submit);
        new ClickCallback<Void>(submit) {
            @Override protected boolean callService () {
                CSwiftly.swiftlysvc.updateProject(_project, this);
                return true;
            }
            @Override protected boolean gotResult (Void result) {
                closeDialog();
                return true;
            }
        };

        // Close button
        cell.add(new Button(_cmsgs.cancel(), new ClickListener() {
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
        _collaborators = new HashMap<Integer, MemberName>();
        CSwiftly.swiftlysvc.getProjectCollaborators(_project.projectId,
            new AsyncCallback<List<MemberName>>() {
                public void onSuccess (List<MemberName> collaborators) {
                    for (MemberName name : collaborators) {
                        _collaborators.put(new Integer(name.getMemberId()), name);
                    }
                    // now that we have our collaborators, load our friends.
                    loadFriends();
                }
                public void onFailure (Throwable caught) {
                    CSwiftly.log("Listing collaborators failed memberId=[" +
                        CSwiftly.getMemberId() + "]", caught);
                    SwiftlyPanel.displayError(CSwiftly.serverError(caught));
                }
            });
    }

    protected void updateCollaboratorsList ()
    {
        _collaboratorsList.clear();

        boolean foundCollaborator = false;
        for (MemberName name : _collaborators.values()) {
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
        _friends = new HashMap<Integer, FriendEntry>();
        CSwiftly.swiftlysvc.getFriends(new AsyncCallback<List<FriendEntry>>() {
            public void onSuccess (List<FriendEntry> friends) {
                for (FriendEntry friend : friends) {
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

        Iterator<FriendEntry> iter = _friends.values().iterator();

        boolean foundFriend = false;
        while (iter.hasNext()) {
            final FriendEntry friend = iter.next();
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
        CSwiftly.swiftlysvc.joinCollaborators(_project.projectId, name, new AsyncCallback<Void>() {
            public void onSuccess (Void result) {
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
        CSwiftly.swiftlysvc.leaveCollaborators(_project.projectId, name, new AsyncCallback<Void>() {
            public void onSuccess (Void result) {
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
    protected HashMap<Integer, MemberName> _collaborators;
    protected HashMap<Integer, FriendEntry> _friends;
    protected Button _addFriendButton;
    protected Button _removeCollaboratorButton;

    protected CheckBox _remixable;
    protected ListBox _collaboratorsList;
    protected ListBox _friendList;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}

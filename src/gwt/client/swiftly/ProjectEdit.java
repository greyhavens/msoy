//
// $Id$

package client.swiftly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import client.shell.Application;
import client.util.ClickCallback;

import com.google.gwt.user.client.Command;
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
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.ui.InlineLabel;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.data.SwiftlyProject;

/**
 * Display a dialog to edit a project.
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

    public ProjectEdit (SwiftlyProject project, ProjectEditListener listener)
    {
        _project = project;
        _listener = listener;
        _amOwner = (CSwiftly.getMemberId() == project.ownerId);

        setStyleName("projectEdit");

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

        cell.add(new Label(CSwiftly.msgs.remixable()));
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
        _collaboratorsPanel = new HorizontalPanel();
        cell.add(_collaboratorsPanel);

        // Add friends list if project owner
        if (_amOwner) {
            cell.add(new Label(CSwiftly.msgs.addCollaborators()));
            _friendList = new ListBox();
            _friendList.addChangeListener(new ChangeListener() {
                public void onChange (Widget sender) {
                    int tx = _friendList.getSelectedIndex();
                    // first item is the menu or no friends message
                    if (tx < 1) {
                        return;
                    }
                    FriendEntry friend =
                        (FriendEntry)_friends.get(new Integer(_friendList.getValue(tx)));
                    addCollaborator(friend.name);
                }
            });
            cell.add(_friendList);
        }
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

        loadCollaborators();
        loadFriends();
    }

    // from BorderedDialog.  This is called in the super constructor, so no UI components that
    // depend on members that are set in this object's constructor can be used here.
    public Widget createContents ()
    {
        return new FlexTable();
    }

    protected void closeDialog()
    {
        removeFromParent();
        if (_listener != null) {
            _listener.projectSubmitted(_project);
        }
    }

    protected void loadCollaborators ()
    {
        _collaborators = new ArrayList();
        CSwiftly.swiftlysvc.getProjectCollaborators(CSwiftly.ident, _project.projectId,
            new AsyncCallback() {
            public void onSuccess (Object result) {
                _collaborators.addAll((List)result);
                displayCollaborators();
            }
            public void onFailure (Throwable caught) {
                CSwiftly.log("Listing collaborators failed memberId=[" + CSwiftly.getMemberId() +
                    "]", caught);
                SwiftlyPanel.displayError(CSwiftly.serverError(caught));
            }
        });
    }

    protected void displayCollaborators ()
    {
        _collaboratorsPanel.clear();
        Iterator iter = _collaborators.iterator();
        if (!iter.hasNext()) {
            _collaboratorsPanel.add(new Label(CSwiftly.msgs.noCollaborators()));
            return;
        }

        while (iter.hasNext()) {
            MemberName name = (MemberName)iter.next();
            final PopupPanel collabMenuPanel = new PopupPanel(true);
            MenuBar menu = getOwnerMenuBar(name, collabMenuPanel);
            collabMenuPanel.add(menu);
            final InlineLabel collaborator = new InlineLabel(name.toString());
            collaborator.addStyleName("LabelLink");
            // use a MouseListener instead of ClickListener to get at the mouse (x,y)
            collaborator.addMouseListener(new MouseListenerAdapter() {
                public void onMouseDown (Widget sender, int x, int y) {
                    collabMenuPanel.setPopupPosition(collaborator.getAbsoluteLeft() + x,
                                                     collaborator.getAbsoluteTop() + y);
                    collabMenuPanel.show();
                }
            });
            _collaboratorsPanel.add(collaborator);
            if (iter.hasNext()) {
                _collaboratorsPanel.add(new InlineLabel(", "));
            }
        }
    }

    /**
     * Get the menus for use by owners when perusing the collaborators of their project.
     */
    protected MenuBar getOwnerMenuBar(final MemberName name, final PopupPanel parent)
    {
        // MenuBar(true) creates a vertical menu
        MenuBar menu = new MenuBar(true);
        menu.addItem(Application.createLinkHtml(CSwiftly.msgs.viewProfile(), "profile",
            "" + name.getMemberId()), true, new Command() {
            public void execute () {
                parent.hide();
                ProjectEdit.this.removeFromParent();
            }
        });

        MenuItem remove = new MenuItem(CSwiftly.msgs.viewRemove(), new Command() {
            public void execute() {
                parent.hide();
                removeCollaborator(name);
            }
        });

        // show actions that we don't have permission to take, but make sure they are
        // disabled. disable removal for the owner on themselves
        if (!_amOwner || _project.ownerId == name.getMemberId()) {
            remove.setCommand(null);
            remove.addStyleName("Disabled");
        }
        menu.addItem(remove);
        return menu;
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
            if (_collaborators.contains(friend.name)) {
                continue;
            }

            // add a title as the first element
            if (!foundFriend) {
                _friendList.addItem(CSwiftly.msgs.friends());
                foundFriend = true;
            }
            _friendList.addItem(friend.name.toString(), String.valueOf(friend.name.getMemberId()));
        }
        if (!foundFriend) {
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
                _collaborators.add(name);
                displayCollaborators();
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
                _collaborators.remove(name);
                displayCollaborators();
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
    protected List _collaborators;
    protected HashMap _friends;
    protected boolean _amOwner;

    protected CheckBox _remixable;
    protected HorizontalPanel _collaboratorsPanel;
    protected ListBox _friendList;
}

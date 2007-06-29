//
// $Id$

package client.swiftly;

import java.util.Iterator;
import java.util.HashSet;
import java.util.List;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.data.SwiftlyProject;
import com.threerings.gwt.ui.InlineLabel;

import client.shell.Application;
import client.util.BorderedDialog;
import client.util.ClickCallback;
import client.util.MsoyUI;
import client.util.PromptPopup;

/**
 * Display a dialog to edit a project.
 */
public class ProjectEdit extends BorderedDialog
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
        // no autohiding, have a close button, disable dragging
        super(false, false, false);

        _project = project;
        _listener = listener;
        _amOwner = (CSwiftly.getMemberId() == project.ownerId);

        _header.add(new InlineLabel(_project.projectName));

        FlexTable contents = (FlexTable)_contents;
        contents.setStyleName("projectEdit");

        int idx = 0;
        contents.setText(idx, 0, CSwiftly.msgs.projectName());
        TextBox projectName = new TextBox();
        contents.setWidget(idx++, 1, projectName);
        projectName.setText(_project.projectName);
        projectName.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                _project.projectName = ((TextBox)sender).getText().trim();
            }
        });

        contents.setText(idx, 0, CSwiftly.msgs.remixable());
        contents.setWidget(idx++, 1, _remixable = new CheckBox());
        _remixable.setChecked(_project.remixable);
        _remixable.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                _project.remixable = _remixable.isChecked();
            }
        });

        contents.setText(idx, 0, CSwiftly.msgs.collaborators());
        contents.getFlexCellFormatter().setHeight(idx, 0, "100%");
        _collaborators = new HorizontalPanel();
        _collaboratorsSet = new HashSet();
        contents.setWidget(idx, 1, _collaborators);

        // Add collaborators button if project owner
        if (_amOwner) {
            _collabMenuPanel = new PopupPanel(true);
            // MenuBar(true) creates a vertical menu
            _collabMenu = new MenuBar(true);
            _collabMenuPanel.add(_collabMenu);
            final Label addCollabs = new Label(CSwiftly.msgs.addCollaborators());
            addCollabs.addStyleName("LabelLink");
            addCollabs.addMouseListener(new MouseListenerAdapter() {
                public void onMouseDown (Widget sender, int x, int y) {
                    updateCollabMenu();
                    _collabMenuPanel.setPopupPosition(addCollabs.getAbsoluteLeft() + x,
                        addCollabs.getAbsoluteTop() + y);
                    _collabMenuPanel.show();
                }
            });
            contents.setWidget(idx, 2, addCollabs);
        }
        idx++;

        // Submit button
        Button submit = new Button(CSwiftly.msgs.submit());
        _footer.add(submit);
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

        // Cancel button
        _footer.add(new Button(CSwiftly.msgs.cancel(), new ClickListener() {
            public void onClick (Widget sender) {
                closeDialog();
            }
        }));

        // TODO: don't call this all over the place
        loadCollaborators();
    }

    // from BorderedDialog.  This is called in the super constructor, so no UI components that
    // depend on members that are set in this object's constructor can be used here.
    public Widget createContents ()
    {
        return new FlexTable();
    }

    protected void closeDialog()
    {
        hide();
        if (_listener != null) {
            _listener.projectSubmitted(_project);
        }
    }

    protected void loadCollaborators()
    {
        CSwiftly.swiftlysvc.getProjectCollaborators(
            CSwiftly.ident, _project.projectId, new AsyncCallback() {
            public void onSuccess (Object result) {
                gotCollaborators((List)result);
            }
            public void onFailure (Throwable caught) {
                CSwiftly.log("getProjectCollaborators failed", caught);
                MsoyUI.error(CSwiftly.serverError(caught));
            }
        });
    }

    protected void gotCollaborators (List members)
    {
        _collaborators.clear();
        _collaboratorsSet.clear();

        Iterator iter = members.iterator();
        if (!iter.hasNext()) {
            _collaborators.add(new Label(CSwiftly.msgs.noCollaborators()));
            return;
        }

        while (iter.hasNext()) {
            MemberName name = (MemberName)iter.next();
            _collaboratorsSet.add(name.toString());
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
            _collaborators.add(collaborator);
            if (iter.hasNext()) {
                _collaborators.add(new InlineLabel(", "));
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
                                                "" + name.getMemberId()), true, (Command)null);
        MenuItem remove = new MenuItem(CSwiftly.msgs.viewRemove(), new Command() {
            public void execute() {
                (new PromptPopup(CSwiftly.msgs.viewRemovePrompt(name.toString(),
                    _project.projectName)) {
                    public void onAffirmative () {
                        parent.hide();
                        removeCollaborator(name.getMemberId());
                    }
                    public void onNegative () { }
                }).prompt();
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
     * Get the menu for use by owners when adding collaborators of their project.
     */
    protected void updateCollabMenu ()
    {
        _collabMenu.clearItems();
        final int memberId = CSwiftly.getMemberId();
        CSwiftly.swiftlysvc.getFriends(CSwiftly.ident, new AsyncCallback() {
            public void onSuccess (Object result) {
                Iterator iter = ((List)result).iterator();
                boolean foundFriend = false;
                while (iter.hasNext()) {
                    final FriendEntry friend = (FriendEntry)iter.next();
                    // do not display friends who are already collaborators
                    if (_collaboratorsSet.contains(friend.name.toString())) {
                        continue;
                    }
                    // add a menu title element as the first element
                    if (!foundFriend) {
                        MenuItem title = new MenuItem(CSwiftly.msgs.friends(), new Command() {
                            public void execute() {
                                // noop
                            }
                        });
                        title.addStyleName("MenuTitle");
                        _collabMenu.addItem(title);
                        foundFriend = true;
                    }
                    MenuItem member = new MenuItem(friend.name.toString(), new Command() {
                        public void execute() {
                            addCollaborator(friend.name.getMemberId());
                            _collabMenuPanel.hide();
                        }
                    });
                    _collabMenu.addItem(member);
                }
                if (!foundFriend) {
                    MenuItem noFriends = new MenuItem(CSwiftly.msgs.noFriends(), new Command() {
                        public void execute() {
                            // noop
                            _collabMenuPanel.hide();
                        }
                    });
                    _collabMenu.addItem(noFriends);
                }
            }
            public void onFailure (Throwable caught) {
                CSwiftly.log("Listing friends failed memberId=[" + memberId + "]", caught);
                MsoyUI.error(CSwiftly.serverError(caught));
            }
        });
    }

    /**
     * Add the indicated member to this project. 
     *
     * @param memberId The member to add.
     */
    protected void addCollaborator (final int memberId)
    {
        CSwiftly.swiftlysvc.joinCollaborators(
            CSwiftly.ident, _project.projectId, memberId, new AsyncCallback() {
            public void onSuccess (Object result) {
                loadCollaborators();
            }
            public void onFailure (Throwable caught) {
                CSwiftly.log("Failed to add collaborator [projectId=" + _project.projectId +
                           ", memberId=" + memberId + "]", caught);
                MsoyUI.error(CSwiftly.serverError(caught));
            }
        });
    }

    /**
     * Remove the indicated member from this project. 
     *
     * @param memberId The member to remove.
     */
    protected void removeCollaborator (final int memberId)
    {
        CSwiftly.swiftlysvc.leaveCollaborators(
            CSwiftly.ident, _project.projectId, memberId, new AsyncCallback() {
            public void onSuccess (Object result) {
                loadCollaborators();
            }
            public void onFailure (Throwable caught) {
                CSwiftly.log("Failed to remove collaborator [projectId=" + _project.projectId +
                             ", memberId=" + memberId + "]", caught);
                MsoyUI.error(CSwiftly.serverError(caught));
            }
        });
    }

    protected SwiftlyProject _project;
    protected ProjectEditListener _listener;
    protected HashSet _collaboratorsSet;
    protected boolean _amOwner;

    protected CheckBox _remixable;
    protected HorizontalPanel _collaborators;
    protected MenuBar _collabMenu;
    protected PopupPanel _collabMenuPanel;
}

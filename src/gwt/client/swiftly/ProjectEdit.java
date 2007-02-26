//
// $Id$

package client.swiftly;

import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.MouseListener;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.FriendEntry;
import com.threerings.msoy.web.data.MemberName;
import com.threerings.msoy.web.data.SwiftlyProject;
import com.threerings.gwt.ui.InlineLabel;

import client.shell.MsoyEntryPoint;
import client.util.BorderedDialog;
import client.util.PromptPopup;

/**
 * Display a dialog to edit a project.
 */
public class ProjectEdit extends BorderedDialog
{
    /**
     * A callback interface for classes that want to know when a project was committed.
     */
    public static interface ProjectEditListener {
        public void projectSubmitted(SwiftlyProject project);
    }
        
    public ProjectEdit (SwiftlyProject project, ProjectEditListener listener)
    {
        super();

        _project = project;
        _listener = listener;
        _amOwner = (CSwiftly.creds.getMemberId() == project.ownerId);

        VerticalPanel contents = (VerticalPanel)_contents;
        _header.add(createTitleLabel(_project.projectName, "ProjectName"));

        TextBox projectName = new TextBox();
        projectName.setText(_project.projectName);
        projectName.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                _project.projectName = ((TextBox)sender).getText().trim();
            }
        });
        contents.add(new InlineLabel(CSwiftly.msgs.projectName()));
        contents.add(projectName);
        _remixable = new CheckBox(CSwiftly.msgs.remixable());
        _remixable.setChecked(_project.remixable);
        _remixable.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                _project.remixable = _remixable.isChecked();
            }
        });
        contents.add(_remixable);

        // List of project collaborators
        _collaborators = new HorizontalPanel();
        contents.add(_collaborators);

        // Add collaborators button if project owner
        if (_amOwner) {
            final PopupPanel collabMenuPanel = new PopupPanel(true);
            MenuBar menu = getCollaboratorsMenu(collabMenuPanel);
            collabMenuPanel.add(menu);
            final Label addCollabs = new Label(CSwiftly.msgs.addCollaborators());
            addCollabs.addMouseListener(new MouseListener() {
                public void onMouseDown (Widget sender, int x, int y) {
                    collabMenuPanel.setPopupPosition(addCollabs.getAbsoluteLeft() + x,
                        addCollabs.getAbsoluteTop() + y);
                    collabMenuPanel.show();
                }
                public void onMouseLeave (Widget sender) { }
                public void onMouseUp (Widget sender, int x, int y) { }
                public void onMouseEnter (Widget sender) { }
                public void onMouseMove (Widget sender, int x, int y) { }
            });
            contents.add(addCollabs);
        }

        // Submit button
        _footer.add(new Button(CSwiftly.msgs.submit(), new ClickListener() {
            public void onClick (Widget sender) {
                commitEdit();
            }
        }));

        // Cancel button
        _footer.add(new Button(CSwiftly.msgs.cancel(),new ClickListener() {
            public void onClick (Widget sender) {
                closeDialog();
            }
        }));

        loadCollaborators();
    }

    // from BorderedDialog.  This is called in the super constructor, so no UI components
    // that depend on members that are set in this object's constructor can be used here.
    public Widget createContents ()
    {
        VerticalPanel contents = new VerticalPanel();
        contents.setStyleName("projectEdit");
        return contents;
    }

    protected void commitEdit()
    {
        // save the project record
        CSwiftly.swiftlysvc.updateProject(CSwiftly.creds, _project, new AsyncCallback() {
            public void onSuccess (Object result) {
                closeDialog();
            }
            public void onFailure (Throwable cause) {
                CSwiftly.serverError(cause);
                // TODO: addError(CSwiftly.serverError(caught));
            }
        });
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
        CSwiftly.swiftlysvc.getProjectCollaborators(CSwiftly.creds, _project.projectId,
            new AsyncCallback() {
            public void onSuccess (Object result) {
                Iterator iter = ((List)result).iterator();
                if (!iter.hasNext()) {
                    _collaborators.add(new Label(CSwiftly.msgs.noCollaborators()));
                } else {
                    _collaborators.add(new InlineLabel(CSwiftly.msgs.collaborators()));
                    while (iter.hasNext()) {
                        MemberName name = (MemberName)iter.next();
                        final PopupPanel collabMenuPanel = new PopupPanel(true);
                        MenuBar menu = getOwnerMenuBar(name, collabMenuPanel);
                        collabMenuPanel.add(menu);
                        final InlineLabel collaborator = new InlineLabel(name.toString());
                        // use a MouseListener instead of ClickListener to get at the mouse (x,y)
                        collaborator.addMouseListener(new MouseListener() {
                            public void onMouseDown (Widget sender, int x, int y) {
                                collabMenuPanel.setPopupPosition(collaborator.getAbsoluteLeft() + x,
                                    collaborator.getAbsoluteTop() + y);
                                collabMenuPanel.show();
                            }
                            public void onMouseLeave (Widget sender) { }
                            public void onMouseUp (Widget sender, int x, int y) { }
                            public void onMouseEnter (Widget sender) { }
                            public void onMouseMove (Widget sender, int x, int y) { }
                        });
                        _collaborators.add(collaborator);
                        if (iter.hasNext()) {
                            _collaborators.add(new InlineLabel(", "));
                        }
                    }
                }
            }
            public void onFailure (Throwable caught) {
                CSwiftly.log("getProjectCollaborators failed", caught);
                // TODO: addError(CSwiftly.serverError(caught));
            }
        });
    }

    /**
     * Get the menus for use by owners when perusing the collaborators of their project.
     */
    protected MenuBar getOwnerMenuBar(final MemberName name, final PopupPanel parent)
    {
        // MenuBar(true) creates a vertical menu
        MenuBar menu = new MenuBar(true);
        menu.addItem("<a href='" + MsoyEntryPoint.memberViewPath(name.getMemberId()) + "'>" +
            CSwiftly.msgs.viewProfile() + "</a>", true, (Command)null);
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
     * Get the menus for use by owners when perusing adding collaborators of their project.
     */
    protected MenuBar getCollaboratorsMenu(final PopupPanel parent)
    {
        final int memberId = CSwiftly.creds.getMemberId();
        // MenuBar(true) creates a vertical menu
        final MenuBar menu = new MenuBar(true);
        CSwiftly.swiftlysvc.getFriends(CSwiftly.creds, new AsyncCallback() {
            public void onSuccess (Object result) {
                Iterator iter = ((List)result).iterator();
                // TODO: this should filter friends who are already collaborators
                if (!iter.hasNext()) {
                    // TODO: _membersProjects.add(new Label(CSwiftly.msgs.noTypes()));
                } else {
                    while (iter.hasNext()) {
                        final FriendEntry friend = (FriendEntry)iter.next();
                        MenuItem member = new MenuItem(friend.name.toString(), new Command() {
                            public void execute() {
                                addCollaborator(friend.name.getMemberId());
                                parent.hide();
                            }
                    });
                    menu.addItem(member);
                    }
                }
            }
            public void onFailure (Throwable caught) {
                CSwiftly.log("Listing friends failed memberId=[" + memberId + "]", caught);
                // TODO: addError(CSwiftly.serverError(caught));
            }
        });
        return menu;
    }

    /**
     * Remove the indicated member from this project. 
     *
     * @param memberId The member to remove.
     */
    protected void removeCollaborator (final int memberId)
    {
        CSwiftly.swiftlysvc.leaveCollaborators(CSwiftly.creds, _project.projectId, memberId,
            new AsyncCallback() {
            public void onSuccess (Object result) {
                // TODO: remove the item from the _collaborators list instead?
                _collaborators.clear();
                loadCollaborators();
            }
            public void onFailure (Throwable caught) {
                CSwiftly.log("Failed to remove collaborator [projectId=" + _project.projectId +
                           ", memberId=" + memberId + "]", caught);
                // TODO: addError(CSwiftly.serverError(caught));
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
        CSwiftly.swiftlysvc.joinCollaborators(CSwiftly.creds, _project.projectId, memberId,
            new AsyncCallback() {
            public void onSuccess (Object result) {
                // TODO: add the item from the _collaborators list instead?
                _collaborators.clear();
                loadCollaborators();
            }
            public void onFailure (Throwable caught) {
                CSwiftly.log("Failed to add collaborator [projectId=" + _project.projectId +
                           ", memberId=" + memberId + "]", caught);
                // TODO: addError(CSwiftly.serverError(caught));
            }
        });
    }

    protected SwiftlyProject _project;
    protected ProjectEditListener _listener;
    protected boolean _amOwner;
    protected CheckBox _remixable;
    protected HorizontalPanel _collaborators;
}

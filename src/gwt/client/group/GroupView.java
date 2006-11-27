//
// $Id$

package client.group;

import java.util.Iterator;
import java.util.Map.Entry;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.client.WebContext;
import com.threerings.msoy.web.data.Group;
import com.threerings.msoy.web.data.GroupDetail;
import com.threerings.msoy.web.data.GroupMembership;
import com.threerings.msoy.web.data.MemberGName;

import client.shell.MsoyEntryPoint;
import client.util.HeaderValueTable;
import client.util.InlineLabel;

import client.group.GroupEdit.GroupSubmissionListener;

/**
 * Display the details of a group, including all its members, and let managers remove other members
 * (unless the group's policy is PUBLIC) and pop up the group editor.
 */
public class GroupView extends DockPanel
    implements GroupSubmissionListener
{
    public GroupView (WebContext ctx, int groupId)
    {
        super();
        _ctx = ctx;

        _errorContainer = new VerticalPanel();
        _errorContainer.setStyleName("groupDetailErrors");
        add(_errorContainer, DockPanel.NORTH);

        loadGroup(groupId);
    }

    /**
     * Called by {@link GroupEdit}; reloads the group.
     */
    public void groupSubmitted (Group group)
    {
        loadGroup(group.groupId);
    }

    /**
     * Fetches the details of the group from the backend and trigger a UI rebuild.
     */
    protected void loadGroup (int groupId)
    {
        _ctx.groupsvc.getGroupDetail(_ctx.creds, groupId, new AsyncCallback() {
            public void onSuccess (Object result) {
                _detail = (GroupDetail) result;
                _group = _detail.group;
                Byte myRank = null;
                if (_ctx.creds != null) {
                    // TODO: Is this too ugly?
                    myRank = (Byte) _detail.members.get(new MemberGName("", _ctx.creds.memberId));
                }
                _amAdmin = (myRank != null) ?
                    myRank.byteValue() == GroupMembership.RANK_MANAGER : false;
                buildUI();
            }
            public void onFailure (Throwable caught) {
                GWT.log("loadGroup failed", caught);
                // TODO: if ServiceException, translate
                addError("Failed to load group.");
            }
        });
    }

    /**
     * Rebuilds the UI from scratch.
     */
    protected void buildUI ()
    {
        // the button panel
        if (_buttonPanel != null) {
            remove(_buttonPanel);
        }
        _buttonPanel = new HorizontalPanel();
        add(_buttonPanel, DockPanel.SOUTH);

        // admins can pop up the group editor
        if (_amAdmin) {
            Button editButton = new Button("Edit");
            _buttonPanel.add(editButton);
            editButton.setStyleName("groupEditorButton");
            editButton.addClickListener(new ClickListener() {
                public void onClick (Widget widget) {
                    new GroupEdit(_ctx, _group, GroupView.this).show();
                }
            });
        }

        // the group data
        if (_table != null) {
            remove(_table);
        }
        _table = new HeaderValueTable();
        add(_table, DockPanel.CENTER);

        _table.addHeader("Group Information");
        _table.addRow("Group ID", String.valueOf(_group.groupId), "Name", _group.name);
        _table.addRow("Charter", _group.charter != null ? _group.charter : "(none written)");
        String policyString = _group.policy == Group.POLICY_PUBLIC ? "Public" :
            _group.policy == Group.POLICY_EXCLUSIVE ? "Exclusive" : "Invitation Only";

        // TODO: the logo's dimensions should be constrained, and/or we should show a thumb here
        String path = _group.logo == null ? "/msoy/images/default_logo.png" :
            MsoyEntryPoint.toMediaPath(_group.logo.getMediaPath());
        _table.addRow("Policy", policyString, "Logo", new Image(path));
        _table.addRow("Created by", _detail.creator.memberName,
               "Created on", _group.creationDate.toString().substring(0, 20));

        // TODO: the member display is very simplistic at the moment, and probably needs the
        // TODO: most work, both aesthetically and functionality-wise.

        FlowPanel memberFlow = new FlowPanel();
        Iterator i = _detail.members.entrySet().iterator();
        boolean first = true;
        while (i.hasNext()) {
            Entry e = (Entry) i.next();
            byte rank = ((Byte) e.getValue()).byteValue();
            final MemberGName name = (MemberGName) e.getKey();
            if (first) {
                first = false;
            } else {
                memberFlow.add(new InlineLabel(" . "));
            }
            // TODO: popup view of member? a popup menu with possible actions on a member?
            memberFlow.add(new InlineLabel(name.memberName));
            // if we're an admin, we can remove non-admins from non-public groups
            if (_amAdmin && rank != GroupMembership.RANK_MANAGER &&
                _group.policy != Group.POLICY_PUBLIC) {
                Label removeLabel = new InlineLabel("(-)");
                removeLabel.addClickListener(new ClickListener() {
                    public void onClick (Widget sender) {
                        removeMember(name.memberId);
                    }
                });
                memberFlow.add(removeLabel);
            }
        }
        _table.addRow("Members", memberFlow);
    }

    /**
     * Removes a member from the group, and then trigger a reload/UI rebuild.
     */
    protected void removeMember (final int memberId)
    {
        _ctx.groupsvc.leaveGroup(_ctx.creds, _group.groupId, memberId, new AsyncCallback() {
            public void onSuccess (Object result) {
                loadGroup(_group.groupId);
            }
            public void onFailure (Throwable caught) {
                GWT.log("Failed to remove member [groupId=" + _group.groupId +
                        ", memberId=" + memberId + "]", caught);
                addError("Failed to remove member: " + caught.getMessage());
            }
        });
    }

    protected void addError (String error)
    {
        _errorContainer.add(new Label(error));
    }

    protected void clearErrors ()
    {
        _errorContainer.clear();
    }

    protected WebContext _ctx;
    protected Group _group;
    protected GroupDetail _detail;
    protected boolean _amAdmin;

    protected HeaderValueTable _table;
    protected HorizontalPanel _buttonPanel;
    protected VerticalPanel _errorContainer;
    protected FlowPanel _memberContainer;
}

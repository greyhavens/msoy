//
// $Id$

package client.group;

import java.util.Iterator;
import java.util.List;

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
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;

import com.threerings.msoy.web.client.WebContext;
import com.threerings.msoy.web.data.Group;
import com.threerings.msoy.web.data.GroupDetail;
import com.threerings.msoy.web.data.GroupMembership;
import com.threerings.msoy.web.data.MemberName;

import client.shell.MsoyEntryPoint;
import client.util.HeaderValueTable;

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
                    GroupMembership me = GroupView.findMember(_detail.members, _ctx.creds.memberId);
                    if (me != null) {
                        myRank = new Byte(me.rank);
                    }
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
        _table.addRow("Created by", _detail.creator.toString(),
               "Created on", _group.creationDate.toString().substring(0, 20));

        // TODO: the member display is very simplistic at the moment, and probably needs the
        // TODO: most work, both aesthetically and functionality-wise.

        FlowPanel memberFlow = new FlowPanel();
        Iterator i = _detail.members.iterator();
        boolean first = true;
        while (i.hasNext()) {
            final GroupMembership membership = (GroupMembership) i.next();
            final MemberName name = (MemberName) membership.member;
            if (first) {
                first = false;
            } else {
                memberFlow.add(new InlineLabel(" . "));
            }
            Label memberLabel = new InlineLabel(name.toString());
            memberLabel.addClickListener(new ClickListener() {
                public void onClick (Widget widget) {
                    new MemberView(_ctx, membership, _amAdmin).show();
                }
            });
            memberFlow.add(memberLabel);
        }
        _table.addRow("Members", memberFlow, "Scenes", new HTML("<a href='/world/index.html#g" + 
            _group.groupId + "'>Home</a>"));
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

    /**
     * performs a simple scan of the list of GroupMembership objects to find and return the 
     * first GroupMembership to refers to the requested memberId.
     */
    static protected GroupMembership findMember (List members, int memberId) 
    {
        Iterator i = members.iterator();
        GroupMembership member = null;
        while ((member == null || member.member.getMemberId() != memberId) && i.hasNext()) {
            member = (GroupMembership)i.next();
        }
        return member;
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

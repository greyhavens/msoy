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
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Anchor;
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
                // in case this object is used more than once, make sure that _me is at least 
                // not stale
                _me = null;
                if (_ctx.creds != null) {
                    _me = GroupView.findMember(_detail.members, _ctx.creds.memberId);
                }
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
        clear();
        boolean amManager = _me != null && _me.rank == GroupMembership.RANK_MANAGER;

        VerticalPanel logoPanel = new VerticalPanel();
        String path = _group.logo == null ? "/msoy/images/default_logo.png" : 
            MsoyEntryPoint.toMediaPath(_group.logo.getMediaPath());
        logoPanel.add(new Image(path));
        HorizontalPanel links = new HorizontalPanel();
        links.add(new Anchor("", "Hall"));
        links.add(new Anchor("", "Forum"));
        if (_group.homepageUrl != null) {
            links.add(new Anchor(_group.homepageUrl, "Homepage"));
        }
        if (amManager) {
            Hyperlink edit = new Hyperlink();
            edit.setText("Edit Group");
            edit.addClickListener(new ClickListener() {
                public void onClick(Widget sender) {
                    new GroupEdit(_ctx, _group, GroupView.this).show();
                }
            });
            links.add(edit);
        }
        logoPanel.add(links);
        add(logoPanel, DockPanel.WEST);

        // All the info that describes the group: name, date, blurb, profile, etc.
        // TODO: There is no earthly reason why these should all be Labels - this is only 
        // priliminary.  This probably won't be a simple FlowPanel either...
        FlowPanel description = new FlowPanel();
        description.add(new Label(_group.name));
        description.add(new Label(_group.creationDate.toLocaleString()));
        description.add(new Label("by " + _detail.creator.toString()));
        description.add(new Label(_group.blurb));
        description.add(new Label(_group.charter));
        add(description, DockPanel.CENTER);

        HeaderValueTable people = new HeaderValueTable();
        add(people, DockPanel.SOUTH);
        FlowPanel managers = new FlowPanel();
        people.addRow("Managers", managers);
        FlowPanel members = new FlowPanel();
        people.addRow("Members", members);
        Iterator i = _detail.members.iterator();
        boolean firstManager = true;
        boolean firstMember = true;
        while (i.hasNext()) {
            final GroupMembership membership = (GroupMembership) i.next();
            final MemberName name = (MemberName) membership.member;
            if (membership.rank == GroupMembership.RANK_MANAGER) {
                if (firstManager) {
                    firstManager = false;
                } else {
                    managers.add(new InlineLabel(", "));
                }
                managers.add(new InlineLabel(name.toString()));
            } else {
                if (firstMember) {
                    firstMember = false;
                } else {
                    members.add(new InlineLabel(", "));
                }
                members.add(new InlineLabel(name.toString()));
            }
        }
    }

    /**
     * performs a simple scan of the list of GroupMembership objects to find and return the 
     * first GroupMembership that refers to the requested memberId.
     */
    static protected GroupMembership findMember (List members, int memberId) 
    {
        Iterator i = members.iterator();
        GroupMembership member = null;
        while ((member == null || member.member.getMemberId() != memberId) && i.hasNext()) {
            member = (GroupMembership)i.next();
        }
        return (member != null && member.member.getMemberId() == memberId) ? member : null;
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
    protected GroupMembership _me;

    protected VerticalPanel _errorContainer;
}

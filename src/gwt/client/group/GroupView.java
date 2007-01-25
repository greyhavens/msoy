//
// $Id$

package client.group;

import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.MouseListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.gwt.ui.Anchor;
import com.threerings.gwt.ui.InlineLabel;

import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.web.data.Group;
import com.threerings.msoy.web.data.GroupExtras;
import com.threerings.msoy.web.data.GroupDetail;
import com.threerings.msoy.web.data.GroupMembership;
import com.threerings.msoy.web.data.MemberName;

import client.shell.MsoyEntryPoint;
import client.util.MediaUtil;
import client.util.PromptPopup;

import client.group.GroupEdit.GroupSubmissionListener;

/**
 * Display the details of a group, including all its members, and let managers remove other members
 * (unless the group's policy is PUBLIC) and pop up the group editor.
 */
public class GroupView extends VerticalPanel
    implements GroupSubmissionListener
{
    public GroupView (GroupContext ctx, int groupId)
    {
        super();
        _ctx = ctx;

        _errorContainer = new VerticalPanel();
        _errorContainer.setStyleName("groupDetailErrors");
        add(_errorContainer);

        _table = new MyFlexTable();
        add(_table);

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
                _extras = _detail.extras;
                // in case this object is used more than once, make sure that _me is at least 
                // not stale
                _me = null;
                if (_ctx.creds != null) {
                    _me = GroupView.findMember(_detail.members, _ctx.creds.getMemberId());
                }
                buildUI();
            }
            public void onFailure (Throwable caught) {
                _ctx.log("loadGroup failed", caught);
                addError(_ctx.serverError(caught));
            }
        });
    }

    /**
     * Rebuilds the UI from scratch.
     */
    protected void buildUI ()
    {
        _table.clear();
        _table.setStyleName("groupView");
        _table.setCellSpacing(0);
        _table.setCellPadding(0);
        boolean amManager = _me != null && _me.rank == GroupMembership.RANK_MANAGER;

        VerticalPanel logoPanel = new VerticalPanel();
        logoPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        logoPanel.setStyleName("LogoPanel");
        logoPanel.add(MediaUtil.createMediaView(_group.logo, MediaDesc.THUMBNAIL_SIZE));
        HorizontalPanel links = new HorizontalPanel();
        links.setStyleName("Links");
        links.setSpacing(8);
        links.add(new Anchor("/world/index.html#g" +  _group.groupId, _ctx.msgs.viewHall()));
        links.add(new Anchor("", _ctx.msgs.viewForum()));
        if (_extras.homepageUrl != null) {
            links.add(new Anchor(_extras.homepageUrl, _ctx.msgs.viewHomepage()));
        }
        logoPanel.add(links);
        VerticalPanel established = new VerticalPanel();
        established.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        established.setStyleName("Established");
        established.add(new InlineLabel(_ctx.msgs.viewEstablishedAbbreviated() + 
            (new SimpleDateFormat("MMM dd, yyyy")).format(_group.creationDate)));
        HorizontalPanel creatorPanel = new HorizontalPanel();
        // this inline div is not letting space display to the right of it, and we need a space.
        InlineLabel byLabel = new InlineLabel(_ctx.msgs.viewBy());
        DOM.setStyleAttribute(byLabel.getElement(), "marginRight", "3px");
        creatorPanel.add(byLabel);
        creatorPanel.add(new Anchor(MsoyEntryPoint.memberViewPath(  
            _detail.creator.getMemberId()), _detail.creator.toString()));
        established.add(creatorPanel);
        logoPanel.add(established);
        InlineLabel policy = new InlineLabel(getPolicyName(_group.policy));
        policy.setStyleName("Policy");
        logoPanel.add(policy);
        if (amManager) {
            logoPanel.add(new Button(_ctx.msgs.viewEdit(), new ClickListener() {
                public void onClick (Widget sender) {
                    new GroupEdit(_ctx, _group, _extras, GroupView.this).show();
                }
            }));
        }
        if (_me == null) {
            if (_group.policy == Group.POLICY_PUBLIC) {
                logoPanel.add(new Button(_ctx.msgs.viewJoin(), new ClickListener() {
                    public void onClick (Widget sender) {
                        (new PromptPopup(_ctx.msgs.viewJoinPrompt(_group.name)) {
                            public void onAffirmative () {
                                joinGroup();
                            }
                            public void onNegative () { }
                        }).prompt();
                    }
                }));
            }
        } else {
            logoPanel.add(new Button(_ctx.msgs.viewLeave(), new ClickListener() {
                public void onClick (Widget sender) {
                    (new PromptPopup(_ctx.msgs.viewLeavePrompt(_group.name)) {
                        public void onAffirmative () {
                            removeMember(_me.member.getMemberId(), false);
                        }
                        public void onNegative () { }
                    }).prompt();
                }
            }));
        } 
        _table.setWidget(0, 0, logoPanel);
        if (_extras.infoBackground != null) {
            _table.getMyFlexCellFormatter().setBackgroundImage(0, 0, 
                MsoyEntryPoint.toMediaPath(_extras.infoBackground.getMediaPath()));
        }

        VerticalPanel description = new VerticalPanel();
        description.setStyleName("DescriptionPanel");
        description.setSpacing(0);
        Label nameLabel = new Label(_group.name);
        nameLabel.setStyleName("Name");
        description.add(nameLabel);
        if (_group.blurb != null) {
            Label blurbLabel = new Label(_group.blurb);
            blurbLabel.setStyleName("Blurb");
            description.add(blurbLabel);
        }
        if (_extras.charter != null) {
            Label charterLabel = new Label(_extras.charter);
            charterLabel.setStyleName("Charter");
            description.add(charterLabel);
        }
        _table.setWidget(0, 1, description);
        if (_extras.detailBackground != null) {
            _table.getMyFlexCellFormatter().setBackgroundImage(0, 1, 
                MsoyEntryPoint.toMediaPath(_extras.detailBackground.getMediaPath()));
        }
        _table.getMyFlexCellFormatter().fillWidth(0, 1);

        FlexTable people = new FlexTable();
        people.setStyleName("PeoplePanel");
        people.setText(0, 0, "Managers:");
        people.setText(1, 0, "Members:");
        FlowPanel managers = new FlowPanel();
        FlowPanel members = new FlowPanel();
        Iterator i = _detail.members.iterator();
        boolean firstManager = true;
        boolean firstMember = true;
        while (i.hasNext()) {
            final GroupMembership membership = (GroupMembership) i.next();
            final MemberName name = (MemberName) membership.member;
            FlowPanel peoplePanel;
            if (membership.rank == GroupMembership.RANK_MANAGER) {
                if (firstManager) {
                    firstManager = false;
                } else {
                    managers.add(new InlineLabel(", "));
                }
                peoplePanel = managers;
            } else {
                if (firstMember) {
                    firstMember = false;
                } else {
                    members.add(new InlineLabel(", "));
                }
                peoplePanel = members;
            }
            if (amManager) {
                final PopupPanel personMenuPanel = new PopupPanel(true);
                MenuBar menu = getManagerMenuBar(membership, personMenuPanel);
                personMenuPanel.add(menu);
                final InlineLabel person = new InlineLabel(name.toString());
                person.addStyleName("LabelLink");
                // use a MouseListener instead of ClickListener so we can get at the mouse (x,y)
                person.addMouseListener(new MouseListener() {
                    public void onMouseDown (Widget sender, int x, int y) { 
                        personMenuPanel.setPopupPosition(person.getAbsoluteLeft() + x, 
                            person.getAbsoluteTop() + y);
                        personMenuPanel.show();
                    }
                    public void onMouseLeave (Widget sender) { }
                    public void onMouseUp (Widget sender, int x, int y) { }
                    public void onMouseEnter (Widget sender) { }
                    public void onMouseMove (Widget sender, int x, int y) { }
                });
                peoplePanel.add(person);
            } else {
                peoplePanel.add(new Anchor(MsoyEntryPoint.memberViewPath(
                    name.getMemberId()), name.toString()));
            }
        }
        people.setWidget(0, 1, managers);
        people.setWidget(1, 1, members);
        _table.setWidget(1, 0, people);
        _table.getFlexCellFormatter().setColSpan(1, 0, 2);
        if (_extras.peopleBackground != null) {
            _table.getMyFlexCellFormatter().setBackgroundImage(1, 0, 
                MsoyEntryPoint.toMediaPath(_extras.peopleBackground.getMediaPath()));
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

    protected String getPolicyName (int policy)
    {
        String policyName;
        switch(policy) {
        case Group.POLICY_PUBLIC: policyName = _ctx.msgs.policyPublic(); break;
        case Group.POLICY_INVITE_ONLY: policyName = _ctx.msgs.policyInvite(); break;
        case Group.POLICY_EXCLUSIVE: policyName = _ctx.msgs.policyExclusive(); break;
        default: policyName = _ctx.msgs.errUnknownPolicy(Integer.toString(policy));
        }
        return policyName;
    }

    /**
     * Get the menus for use by managers when perusing the members of their group.
     */
    protected MenuBar getManagerMenuBar(final GroupMembership membership, final PopupPanel parent) 
    {
        // MenuBar(true) creates a vertical menu
        MenuBar menu = new MenuBar(true);
        menu.addItem("<a href='" + MsoyEntryPoint.memberViewPath(
            membership.member.getMemberId()) + "'>" + _ctx.msgs.viewViewProfile() + "</a>", true, 
            (Command)null);
        MenuItem promote = new MenuItem(_ctx.msgs.viewPromote(), new Command() {
            public void execute() {
                (new PromptPopup(_ctx.msgs.viewPromotePrompt(membership.member.toString())) {
                    public void onAffirmative () {
                        parent.hide();
                        updateMemberRank(membership.member.getMemberId(),
                            GroupMembership.RANK_MANAGER);
                    }
                    public void onNegative () { }
                }).prompt();
            }
        });
        MenuItem demote = new MenuItem(_ctx.msgs.viewDemote(), new Command() {
            public void execute() {
                (new PromptPopup(_ctx.msgs.viewPromotePrompt(membership.member.toString())) {
                    public void onAffirmative () {
                        parent.hide();
                        updateMemberRank(membership.member.getMemberId(),
                            GroupMembership.RANK_MEMBER);
                    }
                    public void onNegative () { }
                }).prompt();
            }
        });
        MenuItem remove = new MenuItem(_ctx.msgs.viewRemove(), new Command() {
            public void execute() {
                (new PromptPopup(_ctx.msgs.viewRemovePrompt(membership.member.toString(), 
                    _group.name)) { 
                    public void onAffirmative () {
                        parent.hide();
                        removeMember(membership.member.getMemberId(), true);
                    }
                    public void onNegative () { }
                }).prompt();
            }
        });

        // show actions that we don't have permission to take, but make sure they are
        // disabled
        if (!isSenior(_me, membership)) {
            // you can't do jack!
            promote.setCommand(null);
            promote.addStyleName("Disabled");
            demote.setCommand(null);
            demote.addStyleName("Disabled");
            remove.setCommand(null);
            remove.addStyleName("Disabled");
        } else if (membership.rank == GroupMembership.RANK_MANAGER) {
            promote.setCommand(null);
            promote.addStyleName("Disabled");
        } else {
            demote.setCommand(null);
            demote.addStyleName("Disabled");
        }
        menu.addItem(promote);
        menu.addItem(demote);
        menu.addItem(remove);
        return menu;
    }

    public boolean isSenior (GroupMembership member1, GroupMembership member2) 
    {
        if (member1.rank == GroupMembership.RANK_MANAGER && 
            (member2.rank == GroupMembership.RANK_MEMBER || 
            member1.rankAssignedDate < member2.rankAssignedDate)) {
            return true;
        } else {
            return false;
        }
    }

    protected void updateMemberRank (final int memberId, final byte rank) 
    {
        _ctx.groupsvc.updateMemberRank(_ctx.creds, _group.groupId, memberId, rank,
            new AsyncCallback() {
            public void onSuccess (Object result) {
                loadGroup(_group.groupId);
            }
            public void onFailure (Throwable caught) {
                _ctx.log("Failed to update member rank [groupId=" + _group.groupId +
                         ", memberId=" + memberId + ", newRank=" + rank + "]", caught);
                addError(_ctx.serverError(caught));
            }
        });
    }

    /**
     * remove the indicated member from this group. 
     *
     * @param memberId The member to remove.
     * @param refresh if <code>true</code>, this page info is refreshed, otherwise the GroupList 
     * page is loaded.
     */
    protected void removeMember (final int memberId, final boolean reload)
    {
        _ctx.groupsvc.leaveGroup(_ctx.creds, _group.groupId, memberId, new AsyncCallback() {
            public void onSuccess (Object result) {
                if (reload) {
                    loadGroup(_group.groupId);
                } else { 
                    // will reload the GroupList page
                    History.newItem("list");
                }
            }
            public void onFailure (Throwable caught) {
                _ctx.log("Failed to remove member [groupId=" + _group.groupId +
                         ", memberId=" + memberId + "]", caught);
                addError(_ctx.serverError(caught));
            }
        });
    }

    protected void joinGroup () 
    {
        _ctx.groupsvc.joinGroup(
            _ctx.creds, _group.groupId, _ctx.creds.getMemberId(), new AsyncCallback() {
                public void onSuccess (Object result) {
                    loadGroup(_group.groupId);
                }
                public void onFailure (Throwable caught) {
                    _ctx.log("Failed to join group [groupId=" + _group.groupId +
                             ", memberId=" + _ctx.creds.getMemberId() + "]", caught);
                    addError(_ctx.serverError(caught));
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

    protected class MyFlexTable extends FlexTable {
        public class MyFlexCellFormatter extends FlexTable.FlexCellFormatter {
            public void setBackgroundImage (int row, int column, String url) {
                DOM.setStyleAttribute(getElement(row, column), "backgroundImage", "url(" + url + 
                    ")");
            }
            public void fillWidth (int row, int column) {
                DOM.setStyleAttribute(getElement(row, column), "width", "100%");
            }
        }

        public MyFlexTable () {
            setCellFormatter(new MyFlexCellFormatter());
        }

        public MyFlexCellFormatter getMyFlexCellFormatter() {
            return (MyFlexCellFormatter)getCellFormatter();
        }
    }

    protected GroupContext _ctx;
    protected Group _group;
    protected GroupExtras _extras;
    protected GroupDetail _detail;
    protected GroupMembership _me;

    protected MyFlexTable _table;
    protected VerticalPanel _errorContainer;
}

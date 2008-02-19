//
// $Id$

package client.whirleds;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
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
import com.google.gwt.user.client.ui.Hyperlink;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.gwt.ui.Anchor;
import com.threerings.gwt.ui.InlineLabel;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.group.data.Group;
import com.threerings.msoy.group.data.GroupDetail;
import com.threerings.msoy.group.data.GroupExtras;
import com.threerings.msoy.group.data.GroupMembership;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.msgs.ForumModels;
import client.msgs.ForumPanel;
import client.shell.Application;
import client.shell.Args;
import client.shell.Frame;
import client.shell.Page;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.PopupMenu;
import client.util.PrettyTextPanel;
import client.util.PromptPopup;
import client.util.RowPanel;
import client.util.TagDetailPanel;

/**
 * Display the details of a group, including all its members, and let managers remove other members
 * (unless the group's policy is PUBLIC) and pop up the group editor.
 */
public class GroupView extends VerticalPanel
{
    public GroupView (ForumModels fmodels)
    {
        super();
        setWidth("100%");

        add(_table = new MyFlexTable());
        add(_forums = new ForumPanel(fmodels));
    }

    /**
     * Configures this view to display the specified group.
     */
    public void setGroup (int groupId, boolean refresh)
    {
        if (_group == null || _group.groupId != groupId || refresh) {
            _me = null; // we'll recompute this when we get the group detail
            loadGroup(groupId);
        }
        _forums.displayGroupThreads(groupId);
    }

    /**
     * Returns the currently loaded group.
     */
    public Group getGroup ()
    {
        return _group;
    }

    /**
     * Returns the currently loaded group extras.
     */
    public GroupExtras getGroupExtras ()
    {
        return _extras;
    }

    /**
     * Fetches the details of the group from the backend and trigger a UI rebuild.
     */
    protected void loadGroup (int groupId)
    {
        CWhirleds.groupsvc.getGroupDetail(CWhirleds.ident, groupId, new MsoyCallback() {
            public void onSuccess (Object result) {
                setGroupDetail((GroupDetail) result);
            }
        });
    }

    /**
     * Configures this view with its group detail and sets up the UI from scratch.
     */
    protected void setGroupDetail (GroupDetail detail)
    {
        _detail = detail;
        if (_detail == null) {
            _group = null;
            return; // the forum list will have already reported no such group, so just bail
        }

        Frame.setTitle(_detail.group.name);
        _group = _detail.group;
        _extras = _detail.extras;

        _me = null; // make sure that _me is not stale
        if (CWhirleds.ident != null) {
            _me = GroupView.findMember(_detail.members, CWhirleds.getMemberId());
        }

        _table.clear();
        _table.setStyleName("groupView");
        _table.setCellSpacing(0);
        _table.setCellPadding(0);
        int col = 0;

        _table.getFlexCellFormatter().setVerticalAlignment(0, col, VerticalPanel.ALIGN_TOP);
        _table.getFlexCellFormatter().setStyleName(0, col, "Back");
        _table.setWidget(0, col++, MsoyUI.createBackArrow());

        VerticalPanel infoPanel = new VerticalPanel();
        infoPanel.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
        infoPanel.setStyleName("LogoPanel");
        infoPanel.setSpacing(0);
        ClickListener click = new ClickListener() {
            public void onClick (Widget sender) {
                Application.go(Page.WORLD, "g" + _group.groupId);
            }
        };
        infoPanel.add(MediaUtil.createMediaView(_group.getLogo(), MediaDesc.THUMBNAIL_SIZE, click));

        HorizontalPanel links = new HorizontalPanel();
        links.setStyleName("Links");
        links.setSpacing(8);
        links.add(Application.createLink(CWhirleds.msgs.viewHall(), "world", "g" +  _group.groupId));
        if (_extras.homepageUrl != null) {
            links.add(new Anchor(_extras.homepageUrl, CWhirleds.msgs.viewHomepage()));
        }
        infoPanel.add(links);

        RowPanel buttons = new RowPanel();
        if (amManager()) {
            buttons.add(new Button(CWhirleds.msgs.viewEdit(), new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.WHIRLEDS, Args.compose("edit", _group.groupId));
                }
            }));
        }
        if (_me == null) {
            if (_group.policy == Group.POLICY_PUBLIC && CWhirleds.getMemberId() > 0) {
                buttons.add(new Button(CWhirleds.msgs.viewJoin(), new ClickListener() {
                    public void onClick (Widget sender) {
                        (new PromptPopup(CWhirleds.msgs.viewJoinPrompt(_group.name)) {
                            public void onAffirmative () {
                                joinGroup();
                            }
                            public void onNegative () { }
                        }).prompt();
                    }
                }));
            }
        } else {
            buttons.add(new Button(CWhirleds.msgs.viewLeave(), new ClickListener() {
                public void onClick (Widget sender) {
                    (new PromptPopup(CWhirleds.msgs.viewLeavePrompt(_group.name)) {
                        public void onAffirmative () {
                            removeMember(_me.member.getMemberId(), false);
                        }
                        public void onNegative () { }
                    }).prompt();
                }
            }));
        } 
        if (buttons.getWidgetCount() > 0) {
            infoPanel.add(buttons);
        }
        _table.setWidget(0, col++, infoPanel);

        _table.setBackgroundImage(
            _extras.background, _extras.backgroundControl == GroupExtras.BACKGROUND_TILED);

        VerticalPanel description = new VerticalPanel();
        description.setStyleName("DescriptionPanel");
        description.setSpacing(0);
        description.add(MsoyUI.createLabel(_group.name, "Name"));

        FlowPanel established = new FlowPanel();
        established.setStyleName("Established");
        established.add(new InlineLabel(CWhirleds.msgs.groupEst(_efmt.format(_group.creationDate)),
                                        false, false, true));
        established.add(new InlineLabel(CWhirleds.msgs.viewBy(), false, false, true));
        established.add(Application.memberViewLink
                        (_detail.creator.toString(), _detail.creator.getMemberId()));
        description.add(established);

        if (_group.blurb != null) {
            Label blurbLabel = new Label(_group.blurb);
            blurbLabel.setStyleName("Blurb");
            description.add(blurbLabel);
        }
        if (_extras.charter != null) {
            PrettyTextPanel charter = new PrettyTextPanel(_extras.charter);
            charter.setStyleName("Charter");
            description.add(charter);
        }
        _table.getFlexCellFormatter().setWidth(0, col, "100%");
        _table.setWidget(0, col++, description);

        final FlexTable people = new FlexTable();
        people.setStyleName("PeoplePanel");
        people.setCellPadding(0);
        people.setCellSpacing(5);
        people.setText(0, 0, CWhirleds.msgs.viewManagers());
        people.setWidget(0, 1, createMembersPanel(GroupMembership.RANK_MANAGER));

        people.setText(1, 0, CWhirleds.msgs.viewMembers());
        RowPanel meminfo = new RowPanel();
        meminfo.add(new Label("" + detail.members.size()));
        meminfo.add(MsoyUI.createActionLabel(CWhirleds.msgs.viewShowMembers(), new ClickListener() {
            public void onClick (Widget sender) {
                people.setWidget(1, 1, createMembersPanel(GroupMembership.RANK_MEMBER));
            }
        }));
        people.setWidget(1, 1, meminfo);

        if (_group.policy != Group.POLICY_EXCLUSIVE) {
            people.setWidget(3, 0, new TagDetailPanel(new TagDetailPanel.TagService() {
                public void tag (String tag, AsyncCallback callback) {
                    CWhirleds.groupsvc.tagGroup(
                        CWhirleds.ident, _group.groupId, tag, true, callback);
                }
                public void untag (String tag, AsyncCallback callback) {
                    CWhirleds.groupsvc.tagGroup(
                        CWhirleds.ident, _group.groupId, tag, false, callback);
                }
                public void getRecentTags (AsyncCallback callback) {
                    CWhirleds.groupsvc.getRecentTags(CWhirleds.ident, callback);
                }
                public void getTags (AsyncCallback callback) {
                    CWhirleds.groupsvc.getTags(CWhirleds.ident, _group.groupId, callback);
                }
                public boolean supportFlags () {
                    return false;
                }
                public void setFlags (byte flag) {
                    // nada
                }
                public void addMenuItems (final String tag, PopupMenu menu) {
                    menu.addMenuItem(CWhirleds.msgs.viewTagLink(), new Command() {
                        public void execute () {
                            Application.go(Page.WHIRLEDS, Args.compose("tag", tag));
                        }
                    });
                }
            }, amManager()));
            people.getFlexCellFormatter().setColSpan(3, 0, 2);
        }
        _table.setWidget(0, col++, people);
    }

    protected FlowPanel createMembersPanel (byte rank)
    {
        FlowPanel panel = new FlowPanel();
        for (Iterator i = _detail.members.iterator(); i.hasNext(); ) {
            GroupMembership membership = (GroupMembership) i.next();
            if (membership.rank != rank) {
                continue;
            }
            if (panel.getWidgetCount() > 0) {
                panel.add(new InlineLabel(", "));
            }

            MemberName name = membership.member;
            if (amManager()) {
                final PopupPanel menuPanel = new PopupPanel(true);
                MenuBar menu = getManagerMenuBar(membership, menuPanel);
                menuPanel.add(menu);
                final InlineLabel person = new InlineLabel(name.toString(), false, false, false);
                person.addStyleName("LabelLink");
                person.addMouseListener(new MouseListenerAdapter() {
                    public void onMouseDown (Widget sender, int x, int y) { 
                        menuPanel.setPopupPosition(
                            person.getAbsoluteLeft() + x, person.getAbsoluteTop() + y);
                        menuPanel.show();
                    }
                });
                panel.add(person);
            } else {
                panel.add(Application.memberViewLink(name.toString(), name.getMemberId()));
            }
        }
        return panel;
    }

    protected boolean amManager ()
    {
        return (_me != null) && (_me.rank == GroupMembership.RANK_MANAGER);
    }

    protected String getPolicyName (int policy)
    {
        String policyName;
        switch(policy) {
        case Group.POLICY_PUBLIC: policyName = CWhirleds.msgs.policyPublic(); break;
        case Group.POLICY_INVITE_ONLY: policyName = CWhirleds.msgs.policyInvite(); break;
        case Group.POLICY_EXCLUSIVE: policyName = CWhirleds.msgs.policyExclusive(); break;
        default: policyName = CWhirleds.msgs.errUnknownPolicy(Integer.toString(policy));
        }
        return policyName;
    }

    /**
     * Get the menus for use by managers when perusing the members of their group.
     */
    protected MenuBar getManagerMenuBar (final GroupMembership membership, final PopupPanel parent) 
    {
        // MenuBar(true) creates a vertical menu
        MenuBar menu = new MenuBar(true);
        menu.addItem(CWhirleds.msgs.viewViewProfile(), new Command() {
            public void execute () {
                Application.go(Page.PEOPLE, "" + membership.member.getMemberId());
            }
        });
        MenuItem promote = new MenuItem(CWhirleds.msgs.viewPromote(), new Command() {
            public void execute() {
                (new PromptPopup(CWhirleds.msgs.viewPromotePrompt(membership.member.toString())) {
                    public void onAffirmative () {
                        parent.hide();
                        updateMemberRank(
                            membership.member.getMemberId(), GroupMembership.RANK_MANAGER);
                    }
                    public void onNegative () { }
                }).prompt();
            }
        });
        MenuItem demote = new MenuItem(CWhirleds.msgs.viewDemote(), new Command() {
            public void execute() {
                (new PromptPopup(CWhirleds.msgs.viewPromotePrompt(membership.member.toString())) {
                    public void onAffirmative () {
                        parent.hide();
                        updateMemberRank(membership.member.getMemberId(),
                            GroupMembership.RANK_MEMBER);
                    }
                    public void onNegative () { }
                }).prompt();
            }
        });
        MenuItem remove = new MenuItem(CWhirleds.msgs.viewRemove(), new Command() {
            public void execute() {
                (new PromptPopup(CWhirleds.msgs.viewRemovePrompt(membership.member.toString(), 
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
             member1.rankAssignedDate.longValue() < member2.rankAssignedDate.longValue())) {
            return true;
        } else {
            return false;
        }
    }

    protected void updateMemberRank (final int memberId, final byte rank) 
    {
        CWhirleds.groupsvc.updateMemberRank(CWhirleds.ident, _group.groupId, memberId, rank,
            new MsoyCallback() {
            public void onSuccess (Object result) {
                loadGroup(_group.groupId);
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
        CWhirleds.groupsvc.leaveGroup(CWhirleds.ident, _group.groupId, memberId, new MsoyCallback() {
            public void onSuccess (Object result) {
                if (reload) {
                    loadGroup(_group.groupId);
                } else { 
                    // will reload the GroupList page
                    Application.go(Page.WHIRLEDS, "list");
                }
            }
        });
    }

    protected void joinGroup () 
    {
        CWhirleds.groupsvc.joinGroup(
            CWhirleds.ident, _group.groupId, CWhirleds.getMemberId(), new MsoyCallback() {
            public void onSuccess (Object result) {
                loadGroup(_group.groupId);
            }
        });
    }

    /**
     * performs a simple scan of the list of GroupMembership objects to find and return the 
     * first GroupMembership that refers to the requested memberId.
     */
    protected static GroupMembership findMember (List members, int memberId) 
    {
        Iterator i = members.iterator();
        GroupMembership member = null;
        while ((member == null || member.member.getMemberId() != memberId) && i.hasNext()) {
            member = (GroupMembership)i.next();
        }
        return (member != null && member.member.getMemberId() == memberId) ? member : null;
    }

    protected class MyFlexTable extends FlexTable {
        public void setBackgroundImage (MediaDesc background, boolean repeat) {
            if (background == null) {
                DOM.setStyleAttribute(getElement(), "background", "none");
            } else {
                DOM.setStyleAttribute(
                    getElement(), "backgroundImage", "url(" + background.getMediaPath() + ")");
                DOM.setStyleAttribute(
                    getElement(), "backgroundRepeat", repeat ? "repeat" : "no-repeat");
            }
        }
    }

    protected Group _group;
    protected GroupExtras _extras;
    protected GroupDetail _detail;
    protected GroupMembership _me;

    protected MyFlexTable _table;
    protected ForumPanel _forums;

    protected static SimpleDateFormat _efmt = new SimpleDateFormat("MMM dd, yyyy");
}

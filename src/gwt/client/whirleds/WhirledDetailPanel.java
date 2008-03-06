//
// $Id$

package client.whirleds;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.gwt.ui.Anchor;
import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.group.data.Group;
import com.threerings.msoy.group.data.GroupDetail;
import com.threerings.msoy.group.data.GroupExtras;
import com.threerings.msoy.group.data.GroupMembership;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.shell.Application;
import client.shell.Args;
import client.shell.Frame;
import client.shell.Page;
import client.shell.WorldClient;
import client.util.CreatorLabel;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.PopupMenu;
import client.util.PrettyTextPanel;
import client.util.PromptPopup;
import client.util.RoundBox;
import client.util.StyledTabPanel;
import client.util.TagDetailPanel;

/**
 * Displays the details of a Whirled.
 */
public class WhirledDetailPanel extends VerticalPanel
{
    public WhirledDetailPanel ()
    {
        setStyleName("whirledDetail");
    }

    /**
     * Configures this view to display the specified group.
     */
    public void setGroup (int groupId, boolean refresh)
    {
        if (_group == null || _group.groupId != groupId || refresh) {
            loadGroup(groupId);
        }
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
        clear();

        _detail = detail;
        if (_detail == null) {
            _group = null;
            add(MsoyUI.createLabel("That Whirled could not be found.", "infoLabel"));
            return;
        }

        Frame.setTitle(_detail.group.name);
        _group = _detail.group;
        _extras = _detail.extras;

        HorizontalPanel main = new HorizontalPanel();
        main.setSpacing(10);
        main.setVerticalAlignment(HorizontalPanel.ALIGN_TOP);
        add(main);

        FlowPanel window = new FlowPanel();
        SimplePanel panel = new SimplePanel();
        panel.setStyleName("Whirled");
        window.add(panel);
        window.add(MsoyUI.createLabel(CWhirleds.msgs.detailEnter(), "Enter"));

        if (_group.policy != Group.POLICY_EXCLUSIVE) {
            window.add(WidgetUtil.makeShim(10, 10));
            window.add(new TagDetailPanel(new TagDetailPanel.TagService() {
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
                    menu.addMenuItem(CWhirleds.msgs.detailTagLink(), new Command() {
                        public void execute () {
                            Application.go(Page.WHIRLEDS, Args.compose("tag", tag));
                        }
                    });
                }
            }, _detail.myRank == GroupMembership.RANK_MANAGER));
        }

        main.add(window);
        WorldClient.displayFeaturedPlace(_group.homeSceneId, panel);

        RoundBox bits = new RoundBox(RoundBox.BLUE);
        bits.add(MsoyUI.createLabel(_group.name, "Name"));

        FlowPanel established = new FlowPanel();
        established.setStyleName("Established");
        established.add(new InlineLabel(CWhirleds.msgs.groupEst(_efmt.format(_group.creationDate)),
                                        false, false, true));
        CreatorLabel creator = new CreatorLabel(_detail.creator);
        creator.addStyleName("inline");
        established.add(creator);
        bits.add(established);

        if (_group.blurb != null) {
            bits.add(WidgetUtil.makeShim(10, 10));
            RoundBox blurb = new RoundBox(RoundBox.WHITE);
            blurb.setWidth("100%");
            blurb.add(MsoyUI.createLabel(_group.blurb, "Blurb"));
            bits.add(blurb);
        }

        HorizontalPanel buttons = new HorizontalPanel();
        if (_detail.myRank == GroupMembership.RANK_NON_MEMBER) {
            if (_group.policy == Group.POLICY_PUBLIC && CWhirleds.getMemberId() > 0) {
                addButton(buttons, MsoyUI.SHORT_THIN, CWhirleds.msgs.detailJoin(),
                          new PromptPopup(CWhirleds.msgs.detailJoinPrompt(_group.name)) {
                              public void onAffirmative () {
                                  joinGroup();
                              }
                              public void onNegative () { /* nada */ }
                          });
            }
        } else {
            addButton(buttons, MsoyUI.SHORT_THIN, CWhirleds.msgs.detailLeave(),
                      new PromptPopup(CWhirleds.msgs.detailLeavePrompt(_group.name)) {
                          public void onAffirmative () {
                              removeMember(CWhirleds.getMemberId());
                          }
                          public void onNegative () { }
                      });
        } 
        if (_detail.myRank == GroupMembership.RANK_MANAGER) {
            addButton(buttons, MsoyUI.SHORT_THIN, CWhirleds.msgs.detailEdit(), new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.WHIRLEDS, Args.compose("edit", _group.groupId));
                }
            });
        }
        addButton(buttons, MsoyUI.MEDIUM_THIN, CWhirleds.msgs.detailForums(),
                  Application.createLinkListener(Page.WHIRLEDS, Args.compose("f", _group.groupId)));
        bits.add(WidgetUtil.makeShim(10, 10));
        bits.add(buttons);
        main.add(bits);

        add(_tabs = new StyledTabPanel());

        String charter = (_extras.charter == null) ?
            CWhirleds.msgs.detailNoCharter() : _extras.charter;
        _tabs.add(new PrettyTextPanel(charter), CWhirleds.msgs.detailTabCharter());
        _tabs.selectTab(0);

        // TODO: make this prettier; get data on demand; use MemberCard
        FlowPanel managers = new FlowPanel();
        for (int ii = 0; ii < _detail.managers.size(); ii++) {
            if (ii > 0) {
                managers.add(new InlineLabel(", ", false, false, true));
            }
            GroupMembership mgr = (GroupMembership)_detail.managers.get(ii);
            managers.add(Application.memberViewLink(mgr.member));
        }
        _tabs.add(managers, CWhirleds.msgs.detailTabManagers());

        // TODO: add members tab

//         setBackgroundImage(
//             _extras.background, _extras.backgroundControl == GroupExtras.BACKGROUND_TILED);
    }

    protected void addButton (HorizontalPanel panel, String type, String label, ClickListener click)
    {
        if (panel.getWidgetCount() > 0) {
            panel.add(WidgetUtil.makeShim(5, 5));
        }
        panel.add(MsoyUI.createButton(type, label, click));
    }

//     protected FlowPanel createMembersPanel (byte rank)
//     {
//         FlowPanel panel = new FlowPanel();
//         for (Iterator i = _detail.members.iterator(); i.hasNext(); ) {
//             GroupMembership membership = (GroupMembership) i.next();
//             if (membership.rank != rank) {
//                 continue;
//             }
//             if (panel.getWidgetCount() > 0) {
//                 panel.add(new InlineLabel(", "));
//             }

//             MemberName name = membership.member;
//             if (amManager()) {
//                 final PopupPanel menuPanel = new PopupPanel(true);
//                 MenuBar menu = getManagerMenuBar(membership, menuPanel);
//                 menuPanel.add(menu);
//                 final InlineLabel person = new InlineLabel(name.toString(), false, false, false);
//                 person.addStyleName("LabelLink");
//                 person.addMouseListener(new MouseListenerAdapter() {
//                     public void onMouseDown (Widget sender, int x, int y) { 
//                         menuPanel.setPopupPosition(
//                             person.getAbsoluteLeft() + x, person.getAbsoluteTop() + y);
//                         menuPanel.show();
//                     }
//                 });
//                 panel.add(person);
//             } else {
//                 panel.add(Application.memberViewLink(name.toString(), name.getMemberId()));
//             }
//         }
//         return panel;
//     }

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

//     /**
//      * Get the menus for use by managers when perusing the members of their group.
//      */
//     protected MenuBar getManagerMenuBar (final GroupMembership membership, final PopupPanel parent) 
//     {
//         // MenuBar(true) creates a vertical menu
//         MenuBar menu = new MenuBar(true);
//         menu.addItem(CWhirleds.msgs.detailViewProfile(), new Command() {
//             public void execute () {
//                 Application.go(Page.PEOPLE, "" + membership.member.getMemberId());
//             }
//         });
//         MenuItem promote = new MenuItem(CWhirleds.msgs.detailPromote(), new Command() {
//             public void execute() {
//                 (new PromptPopup(CWhirleds.msgs.detailPromotePrompt(membership.member.toString())) {
//                     public void onAffirmative () {
//                         parent.hide();
//                         updateMemberRank(
//                             membership.member.getMemberId(), GroupMembership.RANK_MANAGER);
//                     }
//                     public void onNegative () { }
//                 }).prompt();
//             }
//         });
//         MenuItem demote = new MenuItem(CWhirleds.msgs.detailDemote(), new Command() {
//             public void execute() {
//                 (new PromptPopup(CWhirleds.msgs.detailPromotePrompt(membership.member.toString())) {
//                     public void onAffirmative () {
//                         parent.hide();
//                         updateMemberRank(membership.member.getMemberId(),
//                             GroupMembership.RANK_MEMBER);
//                     }
//                     public void onNegative () { }
//                 }).prompt();
//             }
//         });
//         MenuItem remove = new MenuItem(CWhirleds.msgs.detailRemove(), new Command() {
//             public void execute() {
//                 (new PromptPopup(CWhirleds.msgs.detailRemovePrompt(membership.member.toString(), 
//                     _group.name)) { 
//                     public void onAffirmative () {
//                         parent.hide();
//                         removeMember(membership.member.getMemberId());
//                     }
//                     public void onNegative () { }
//                 }).prompt();
//             }
//         });

//         // show actions that we don't have permission to take, but make sure they are
//         // disabled
//         if (!isSenior(_me, membership)) {
//             // you can't do jack!
//             promote.setCommand(null);
//             promote.addStyleName("Disabled");
//             demote.setCommand(null);
//             demote.addStyleName("Disabled");
//             remove.setCommand(null);
//             remove.addStyleName("Disabled");
//         } else if (membership.rank == GroupMembership.RANK_MANAGER) {
//             promote.setCommand(null);
//             promote.addStyleName("Disabled");
//         } else {
//             demote.setCommand(null);
//             demote.addStyleName("Disabled");
//         }
//         menu.addItem(promote);
//         menu.addItem(demote);
//         menu.addItem(remove);
//         return menu;
//     }

//     public boolean isSenior (GroupMembership member1, GroupMembership member2) 
//     {
//         if (member1.rank == GroupMembership.RANK_MANAGER && 
//             (member2.rank == GroupMembership.RANK_MEMBER || 
//              member1.rankAssignedDate.longValue() < member2.rankAssignedDate.longValue())) {
//             return true;
//         } else {
//             return false;
//         }
//     }

    protected void setBackgroundImage (MediaDesc background, boolean repeat)
    {
        if (background == null) {
            DOM.setStyleAttribute(getElement(), "background", "none");
        } else {
            DOM.setStyleAttribute(
                getElement(), "backgroundImage", "url(" + background.getMediaPath() + ")");
            DOM.setStyleAttribute(
                getElement(), "backgroundRepeat", repeat ? "repeat" : "no-repeat");
        }
    }

//     protected void updateMemberRank (final int memberId, final byte rank) 
//     {
//         CWhirleds.groupsvc.updateMemberRank(CWhirleds.ident, _group.groupId, memberId, rank,
//             new MsoyCallback() {
//             public void onSuccess (Object result) {
//                 loadGroup(_group.groupId);
//             }
//         });
//     }

    protected void removeMember (final int memberId)
    {
        CWhirleds.groupsvc.leaveGroup(CWhirleds.ident, _group.groupId, memberId, new MsoyCallback() {
            public void onSuccess (Object result) {
                loadGroup(_group.groupId);
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

    protected static GroupMembership findMember (List members, int memberId) 
    {
        Iterator i = members.iterator();
        GroupMembership member = null;
        while ((member == null || member.member.getMemberId() != memberId) && i.hasNext()) {
            member = (GroupMembership)i.next();
        }
        return (member != null && member.member.getMemberId() == memberId) ? member : null;
    }

    protected Group _group;
    protected GroupExtras _extras;
    protected GroupDetail _detail;
    protected StyledTabPanel _tabs;

    protected static SimpleDateFormat _efmt = new SimpleDateFormat("MMM dd, yyyy");
}

//
// $Id$

package client.groups;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.orth.data.MediaDesc;
import com.threerings.orth.data.MediaDescSize;

import com.threerings.gwt.ui.FloatPanel;
import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.DateUtil;
import com.threerings.gwt.util.PopupCallback;

import com.threerings.msoy.data.all.Theme;
import com.threerings.msoy.group.data.all.Group;
import com.threerings.msoy.group.data.all.GroupMembership.Rank;
import com.threerings.msoy.group.gwt.GroupDetail;
import com.threerings.msoy.group.gwt.GroupExtras;
import com.threerings.msoy.group.gwt.GroupMemberCard;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.groups.GroupsPage.Nav;
import client.item.ShopUtil;
import client.room.SceneUtil;
import client.shell.CShell;
import client.shell.ShellMessages;
import client.ui.MsoyUI;
import client.ui.PromptPopup;
import client.ui.RoundBox;
import client.ui.ThumbBox;
import client.util.ClickCallback;
import client.util.InfoCallback;
import client.util.Link;
import client.util.MediaUtil;

/**
 * Displays the details of a Group.
 */
public class GroupDetailPanel extends FlowPanel
{
    public GroupDetailPanel () {
        setStyleName("groupDetail");
    }

    /**
     * Returns the currently loaded group detail.
     */
    public GroupDetail getGroupDetail ()
    {
        return _detail;
    }

    /**
     * Returns the currently loaded group.
     */
    public Group getGroup () {
        return _group;
    }

    /**
     * Returns the currently loaded group extras.
     */
    public GroupExtras getGroupExtras () {
        return _extras;
    }

    /**
     * Fetches the details of the group from the backend and trigger a UI rebuild.
     */
    protected void loadGroup (int groupId) {
        _groupsvc.getGroupDetail(groupId, new InfoCallback<GroupDetail>() {
            public void onSuccess (GroupDetail detail) {
                setGroupDetail(detail);
            }
        });
    }

    /**
     * Configures this view with its group detail and sets up the UI from scratch.
     */
    protected void setGroupDetail (GroupDetail detail) {
        clear();

        _detail = detail;
        if (_detail == null) {
            _group = null;
            add(MsoyUI.createLabel(_msgs.detailGroupNotFound(), "infoLabel"));
            return;
        }
        CShell.frame.setTitle(_detail.group.name);
        _group = _detail.group;
        _extras = _detail.extras;

        SmartTable mainDetails = new SmartTable();
        mainDetails.getRowFormatter().setVerticalAlign(0, HasAlignment.ALIGN_TOP);
        mainDetails.setStyleName("MainDetails");
        add(mainDetails);

        FlowPanel leftPanel = MsoyUI.createFlowPanel("Left");
        mainDetails.setWidget(0, 0, leftPanel);

        // icon, group name, creator, members
        RoundBox titleBox = new RoundBox(RoundBox.MEDIUM_BLUE);
        titleBox.addStyleName("TitleBox");
        leftPanel.add(titleBox);

        // use the default logo if this group doesn't have one
        MediaDesc logoMedia = _group.logo;
        if (logoMedia == null) {
            logoMedia = Group.getDefaultGroupLogoMedia();
        }
        SimplePanel logo = MsoyUI.createSimplePanel(MediaUtil.createMediaView(logoMedia,
            MediaDescSize.HALF_THUMBNAIL_SIZE), "Logo");
        titleBox.add(logo);
        titleBox.add(MsoyUI.createLabel(_group.name, "Name"));

        // est. [date] by [name-link] all inline but limited width
        FlowPanel established = MsoyUI.createFlowPanel("Established");
        established.add(new InlineLabel(_msgs.groupEst(DateUtil.formatDate(_group.creationDate)),
            false, false, true));
        established.add(new InlineLabel(_cmsgs.creatorBy(), false, false, true));
        Widget creator = Link.memberView(_detail.creator);
        creator.addStyleName("Creator");
        established.add(creator);
        titleBox.add(established);

        // members opens in contentPanel
        titleBox.add(new InlineLabel(_group.memberCount + " "));
        InlineLabel members = new InlineLabel(_msgs.detailMembers());
        titleBox.add(members);
        members.addStyleName("actionLabel");
        ClickHandler membersClick = new ClickHandler() {
            public void onClick (ClickEvent event) {
                _contentPanel.showMembers();
            }
        };
        members.addClickHandler(membersClick);

        // enter, discussions and medals buttons
        FloatPanel buttons = new FloatPanel("Buttons");
        leftPanel.add(buttons);
        PushButton enterButton = MsoyUI.createButton(MsoyUI.MEDIUM_THIN, _msgs.detailEnter(),
            Link.createHandler(Pages.WORLD, "s" + _group.homeSceneId));
        enterButton.addStyleName("EnterButton");
        buttons.add(enterButton);
        PushButton discussionsButton = MsoyUI.createButton(MsoyUI.MEDIUM_THIN,
            _msgs.detailForums(), Link.createHandler(
                Pages.GROUPS, GroupsPage.Nav.FORUM.composeArgs(_group.groupId)));
        discussionsButton.addStyleName("DiscussionsButton");
        buttons.add(discussionsButton);
        PushButton medalsButton = MsoyUI.createButton(MsoyUI.MEDIUM_THIN,
            _msgs.detailViewMedals(), Link.createHandler(
                Pages.GROUPS, GroupsPage.Nav.MEDALS.composeArgs(_group.groupId)));
        medalsButton.addStyleName("MedalsButton");
        buttons.add(medalsButton);

        // join, charter, shop, manage, etc
        FlowPanel actions = MsoyUI.createFlowPanel("Actions");
        leftPanel.add(actions);

        FlowPanel basicActions = MsoyUI.createFlowPanel("BasicActions");
        actions.add(basicActions);

        // link to the game for this group
        if (_detail.group.gameId != 0) {
            basicActions.add(MsoyUI.createActionLabel(_msgs.detailPlayGame(),
                            Link.createHandler(Pages.GAMES, "d", _detail.group.gameId)));
        }

        if (_detail.myRank == Rank.NON_MEMBER) {
            if (Group.canJoin(_group.policy)) {
                Label join = MsoyUI.createLabel(_msgs.detailJoin(), null);
                new ClickCallback<Void>(join, _msgs.detailJoinPrompt()) {
                    @Override protected boolean callService () {
                        if (!MsoyUI.requireValidated()) {
                            return false;
                        }
                        _groupsvc.joinGroup(_group.groupId, this);
                        return true;
                    }
                    @Override protected boolean gotResult (Void result) {
                        loadGroup(_group.groupId);
                        return true;
                    }
                    protected String getPromptContext () {
                        return _msgs.detailJoinContext(_group.name);
                    }
                };
                basicActions.add(join);
            }

        } else {
            // leave this group
            basicActions.add(MsoyUI.createActionLabel(_msgs.detailLeave(), new PromptPopup(
                _msgs.detailLeavePrompt(_group.name), removeMember(CShell.getMemberId()))));
        }

        // invite others to it
        if (Group.canInvite(detail.group.policy, detail.myRank)) {
            basicActions.add(MsoyUI.createActionLabel(_msgs.detailInvite(),
                            Link.createHandler(Pages.MAIL, "w", "g", _detail.group.groupId)));
        }

        // shop
        if (_extras.catalogTag != null && !_extras.catalogTag.equals("")) {
            Args args = ShopUtil.composeArgs(_extras.catalogItemType, _extras.catalogTag, null, 0);
            basicActions.add(MsoyUI.createActionLabel(
                            _msgs.detailShop(), Link.createHandler(Pages.SHOP, args)));
        }

        // read charter
        basicActions.add(MsoyUI.createActionLabel(_msgs.detailReadCharter(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                _contentPanel.showCharter();
            }
        }));

        // edit this group & manage rooms
        if (_detail.myRank == Rank.MANAGER || CShell.isSupport()) {
            FlowPanel managerActions = MsoyUI.createFlowPanel("ManagerActions");
            actions.add(managerActions);

            Args args = GroupsPage.Nav.EDIT.composeArgs(_group.groupId);
            managerActions.add(MsoyUI.createActionLabel(
                _msgs.detailEdit(), "inline", Link.createHandler(Pages.GROUPS, args)));

            managerActions.add(new InlineLabel(" | "));
            managerActions.add(MsoyUI.createActionLabel(
                _msgs.detailManageRooms(), "inline", new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        _contentPanel.showRooms();
                    }
                }));

            managerActions.add(new InlineLabel(" | "));
            managerActions.add(MsoyUI.createActionLabel(
                _msgs.detailAwardMedals(), "inline", new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        _contentPanel.showAwardMedals();
                    }
                }));
        }

        // manage themes
        if (CShell.isSupport() || (Theme.isLive() && _detail.myRank == Rank.MANAGER)) {
            _themeActions = MsoyUI.createFlowPanel("ThemeActions");
            actions.add(_themeActions);
            updateThemes();
        }

        FlowPanel rightPanel = MsoyUI.createFlowPanel("Right");
        mainDetails.setWidget(0, 1, rightPanel);

        // screenshot, #online, blurb
        FlowPanel screenshot = MsoyUI.createFlowPanel("ScreenshotBox");
        rightPanel.add(screenshot);

        // display a screenshot of the group that can be clicked for a live view
        Widget liveView = SceneUtil.createSceneView(_group.homeSceneId, detail.homeSnapshot);
        liveView.addStyleName("Screenshot");
        screenshot.add(liveView);

        if (_detail.population > 0) {
            screenshot.add(MsoyUI.createHTML(_msgs.detailOnline("" + _detail.population),
                "Online"));
        }

        if (_group.blurb != null) {
            screenshot.add(MsoyUI.createHTML(_group.blurb, "Blurb"));
        }

        HorizontalPanel lowerArea = new HorizontalPanel();
        add(lowerArea);
        // content panel defaults to discussions
        lowerArea.add(_contentPanel = new DetailContentPanel(_detail));
        _contentPanel.showDiscussions();
        // list managers and some members
        lowerArea.add(new TopMembersPanel());
    }

    protected void updateThemes ()
    {
        _themeActions.clear();

        if (CShell.isSupport() || Theme.isLive()) {
            final int groupId = _detail.group.groupId;
            if (_detail.theme != null) {
                _themeActions.add(MsoyUI.createActionLabel(_msgs.detailEditTheme(), "inline",
                    Link.createHandler(Pages.GROUPS, Nav.THEME_EDIT.composeArgs(groupId))));

                _themeActions.add(new InlineLabel(" | "));
                _themeActions.add(MsoyUI.createActionLabel(_msgs.detailViewLineup(), "inline",
                        Link.createHandler(Pages.STUFF, "l", groupId)));

                _themeActions.add(new InlineLabel(" | "));
                _themeActions.add(MsoyUI.createActionLabel(_msgs.detailViewTemplateRooms(),
                    "inline", new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        _contentPanel.showTemplates();
                    }
                }));

//                _themeActions.add(new InlineLabel(" | "));
//                _themeActions.add(MsoyUI.createActionLabel(_msgs.detailViewShop(), "inline",
//                    Link.createHandler(Pages.SHOP, "j", groupId)));

            } else {
                _themeActions.add(MsoyUI.createActionLabel(
                    _msgs.detailCreateTheme(), "inline", new ClickHandler() {
                        public void onClick (ClickEvent event) {
                            AsyncCallback<Theme> ourCallback = new PopupCallback<Theme>() {
                                public void onSuccess (Theme theme) {
                                    _buyPopup.hide();
                                    _detail.theme = theme;
                                    updateThemes();
                                }
                            };

                            _buyPopup = ThemeBuyPanel.buyTheme(
                                groupId, _detail.group.name, ourCallback);
                        }
                    }));
            }
        }
    }

    protected Command removeMember (final int memberId) {
        return new Command() {
            public void execute () {
                _groupsvc.leaveGroup(_group.groupId, memberId, refresh());
            }
        };
    }

    protected InfoCallback<Void> refresh () {
        return new InfoCallback<Void>() {
            public void onSuccess (Void result) {
                loadGroup(_group.groupId);
            }
        };
    }

    /**
     * Displays managers and most recently online members
     */
    protected class TopMembersPanel extends FlowPanel
    {
        public TopMembersPanel () {
            setStyleName("TopMembersPanel");
            add(MsoyUI.createSimplePanel(MsoyUI.createHTML(
                _msgs.detailTopMembersTitle(), null), "Title"));

            FlowPanel content = new FlowPanel();
            content.setStyleName("Content");
            add(content);

            SmartTable members = new SmartTable("Members", 0, 0);
            content.add(members);
            for (int ii = 0; ii < _detail.topMembers.size(); ii++) {
                GroupMemberCard member = _detail.topMembers.get(ii);
                ThumbBox icon = new ThumbBox(member.photo, MediaDescSize.HALF_THUMBNAIL_SIZE,
                                             Pages.PEOPLE, ""+member.name.getId());
                members.setWidget(ii * 2, 0, icon, 1, "Icon");
                members.getFlexCellFormatter().setRowSpan(ii * 2, 0, 2);

                int shares = _detail.brand.getShares(member.name.getId());
                String shareDesc = shares + "/" + _detail.brand.getTotalShares();
                String desc;
                if (member.rank == Rank.MANAGER) {
                    if (shares > 0) {
                        desc = _msgs.detailTopMembersManagerWithShares(shareDesc);
                    } else {
                        desc = _msgs.detailTopMembersManager();
                    }

                } else if (_detail.brand != null && shares > 0) {
                    desc = _msgs.detailTopMembersShareholder(shareDesc);

                } else {
                    desc = "";
                }
                members.setHTML(ii * 2, 1, desc, 1, "Manager");
                SimplePanel name = MsoyUI.createSimplePanel(Link.memberView(member), "Name");
                members.setWidget((ii * 2) + 1, 0, name);
            }

            // see all opens in contentPanel
            Label seeAllLink = new Label(_msgs.detailTopMembersSeeAll());
            content.add(seeAllLink);
            seeAllLink.addStyleName("SeeAll");
            seeAllLink.addStyleName("actionLabel");
            ClickHandler membersClick = new ClickHandler() {
                public void onClick (ClickEvent event) {
                    _contentPanel.showMembers();
                }
            };
            seeAllLink.addClickHandler(membersClick);
        }
    }

    protected Group _group;
    protected GroupDetail _detail;
    protected GroupExtras _extras;
    protected DetailContentPanel _contentPanel;
    protected FlowPanel _themeActions;
    protected PopupPanel _buyPopup;

    protected static final GroupsMessages _msgs = GWT.create(GroupsMessages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final GroupServiceAsync _groupsvc = GWT.create(GroupService.class);
}

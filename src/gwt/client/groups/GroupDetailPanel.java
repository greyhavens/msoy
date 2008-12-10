//
// $Id$

package client.groups;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.FloatPanel;
import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.group.data.all.Group;
import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.gwt.GroupDetail;
import com.threerings.msoy.group.gwt.GroupExtras;
import com.threerings.msoy.group.gwt.GroupMemberCard;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.item.ShopUtil;
import client.room.SceneUtil;
import client.shell.CShell;
import client.shell.ShellMessages;
import client.ui.MsoyUI;
import client.ui.PrettyTextPanel;
import client.ui.PromptPopup;
import client.ui.RoundBox;
import client.ui.ThumbBox;
import client.util.ClickCallback;
import client.util.Link;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * Displays the details of a Group.
 */
public class GroupDetailPanel extends FlowPanel
{
    public GroupDetailPanel () {
        setStyleName("groupDetail");
    }

    /**
     * Configures this view to display the specified group.
     */
    public void setGroup (int groupId, boolean refresh) {
        if (_group == null || _group.groupId != groupId || refresh) {
            loadGroup(groupId);
        }
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
        _groupsvc.getGroupDetail(groupId, new MsoyCallback<GroupDetail>() {
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

        FloatPanel mainDetails = new FloatPanel("MainDetails");
        add(mainDetails);

        FlowPanel leftPanel = MsoyUI.createFlowPanel("Left");
        mainDetails.add(leftPanel);

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
            MediaDesc.HALF_THUMBNAIL_SIZE), "Logo");
        titleBox.add(logo);
        titleBox.add(MsoyUI.createLabel(_group.name, "Name"));

        // est. [date] by [name-link] all inline but limited width
        FlowPanel established = MsoyUI.createFlowPanel("Established");
        established.add(new InlineLabel(_msgs.groupEst(MsoyUI.formatDate(_group.creationDate)),
            false, false, true));
        established.add(new InlineLabel(_cmsgs.creatorBy(), false, false, true));
        Widget creator = Link.memberView(_detail.creator.toString(),
            _detail.creator.getMemberId());
        creator.addStyleName("Creator");
        established.add(creator);
        titleBox.add(established);

        // members opens in contentPanel
        titleBox.add(new InlineLabel(_group.memberCount + " "));
        InlineLabel members = new InlineLabel(_msgs.detailMembers());
        titleBox.add(members);
        members.addStyleName("actionLabel");
        ClickListener membersClick = new ClickListener() {
            public void onClick (Widget sender) {
                _contentPanel.showMembers();
            }
        };
        members.addClickListener(membersClick);

        // enter and discussions buttons
        FloatPanel buttons = new FloatPanel("Buttons");
        leftPanel.add(buttons);
        PushButton enterButton = MsoyUI.createButton(MsoyUI.LONG_THIN, _msgs.detailEnter(),
            Link.createListener(Pages.WORLD, "s" + _group.homeSceneId));
        enterButton.addStyleName("EnterButton");
        buttons.add(enterButton);
        PushButton discussionsButton = MsoyUI.createButton(MsoyUI.MEDIUM_THIN,
            _msgs.detailForums(), Link.createListener(Pages.GROUPS, Args.compose("f",
                _group.groupId)));
        discussionsButton.addStyleName("DiscussionsButton");
        buttons.add(discussionsButton);

        // join, charter, shop, manage, etc
        FlowPanel actions = MsoyUI.createFlowPanel("Actions");
        leftPanel.add(actions);

        // link to the game for this group
        if (_detail.group.gameId != 0) {
            actions.add(MsoyUI.createActionLabel(_msgs.detailPlayGame(), Link.createListener(
                Pages.GAMES, Args.compose("d", "" + _detail.group.gameId))));
        }

        if (_detail.myRank == GroupMembership.RANK_NON_MEMBER) {
            // join this group
            if (Group.canJoin(_group.policy) && !CShell.isGuest()) {
                Label join = MsoyUI.createLabel(_msgs.detailJoin(), null);
                new ClickCallback<Void>(join, _msgs.detailJoinPrompt()) {
                    @Override protected boolean callService () {
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
                actions.add(join);
            }

        } else {
            // leave this group
            actions.add(MsoyUI.createActionLabel(_msgs.detailLeave(), new PromptPopup(
                _msgs.detailLeavePrompt(_group.name), removeMember(CShell.getMemberId()))));
        }

        // invite others to it
        if (Group.canInvite(detail.group.policy, detail.myRank)) {
            String args = Args.compose("w", "g", "" + _detail.group.groupId);
            actions.add(MsoyUI.createActionLabel(
                            _msgs.detailInvite(), Link.createListener(Pages.MAIL, args)));
        }

        // shop
        if (_extras.catalogTag != null && !_extras.catalogTag.equals("")) {
            String args = ShopUtil.composeArgs(_extras.catalogItemType, _extras.catalogTag, null, 0);
            actions.add(MsoyUI.createActionLabel(
                            _msgs.detailShop(), Link.createListener(Pages.SHOP, args)));
        }

        // read charter
        actions.add(MsoyUI.createActionLabel(_msgs.detailReadCharter(), new ClickListener() {
            public void onClick (Widget sender) {
                _contentPanel.showCharter();
            }
        }));

        if (DeploymentConfig.devDeployment) {
            // view medals
            actions.add(MsoyUI.createActionLabel(_msgs.detailViewMedals(), new ClickListener() {
                public void onClick (Widget sender) {
                    _contentPanel.showMedals();
                }
            }));
        }

        // edit this group & manage rooms
        if (_detail.myRank == GroupMembership.RANK_MANAGER || CShell.isSupport()) {
            FlowPanel managerActions = MsoyUI.createFlowPanel("ManagerActions");
            actions.add(managerActions);

            String args = Args.compose("edit", _group.groupId);
            managerActions.add(MsoyUI.createActionLabel(
                _msgs.detailEdit(), "inline", Link.createListener(Pages.GROUPS, args)));

            managerActions.add(new InlineLabel(" | "));
            managerActions.add(MsoyUI.createActionLabel(
                _msgs.detailManageRooms(), "inline", new ClickListener() {
                    public void onClick (Widget sender) {
                        _contentPanel.showRooms();
                    }
                }));

            if (DeploymentConfig.devDeployment) {
                managerActions.add(new InlineLabel(" | "));
                managerActions.add(MsoyUI.createActionLabel(
                    _msgs.detailAwardMedals(), "inline", new ClickListener() {
                        public void onClick (Widget sender) {
                            _contentPanel.showAwardMedals();
                        }
                    }));
            }
        }

        FlowPanel rightPanel = MsoyUI.createFlowPanel("Right");
        mainDetails.add(rightPanel);

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
        lowerArea.add(_contentPanel = new ContentPanel());
        _contentPanel.showDiscussions();
        // list managers and some members
        lowerArea.add(new TopMembersPanel());
    }

    protected Command removeMember (final int memberId) {
        return new Command() {
            public void execute () {
                _groupsvc.leaveGroup(_group.groupId, memberId, refresh());
            }
        };
    }

    protected MsoyCallback<Void> refresh () {
        return new MsoyCallback<Void>() {
            public void onSuccess (Void result) {
                loadGroup(_group.groupId);
            }
        };
    }

    /**
     * Displays multiple panels: Discussions, Charter, Members, Rooms. Panels are cached and not
     * regenerated when swapping back to one.
     */
    protected class ContentPanel extends FlowPanel
    {
        public ContentPanel () {
            setStyleName("ContentPanel");
            add(_title = MsoyUI.createSimplePanel(null, "ContentPanelTitle"));

            // contains content and discussions button for css min-height
            FlowPanel container = new FlowPanel();
            container.setStyleName("ContentPanelContainer");
            add(container);
            container.add(_content = new SimplePanel());
            _content.setStyleName("ContentPanelContent");

            // back to discussions button hidden by default
            container.add(_backButton = new Label(_msgs.detailBackToDiscussions()));
            _backButton.setVisible(false);
            _backButton.addStyleName("actionLabel");
            _backButton.addStyleName("ContentBackButton");
            ClickListener backClick = new ClickListener() {
                public void onClick (Widget sender) {
                    _contentPanel.showDiscussions();
                }
            };
            _backButton.addClickListener(backClick);
        }

        public void showDiscussions () {
            if (_discussions != null && _content.getWidget() == _discussions) {
                return;
            }
            _title.setWidget(new Label(_msgs.detailTabDiscussions()));
            if (_discussions == null) {
                _discussions = new GroupDiscussionsPanel(_detail);
            }
            _content.setWidget(_discussions);
            _backButton.setVisible(false);
        }

        public void showCharter () {
            if (_charter != null && _content.getWidget() == _charter) {
                return;
            }
            _title.setWidget(new Label(_msgs.detailTabCharter()));
            if (_charter == null) {
                String charterText = (_extras.charter == null) ? _msgs.detailNoCharter()
                    : _extras.charter;
                _charter = new PrettyTextPanel(charterText);
            }
            _content.setWidget(_charter);
            _backButton.setVisible(true);
        }

        public void showMembers () {
            if (_members != null && _content.getWidget() == _members) {
                return;
            }
            _title.setWidget(new Label(_msgs.detailTabMembers()));
            if (_members == null) {
                _members = new GroupMembersPanel(_detail);
            }
            _content.setWidget(_members);
            _backButton.setVisible(true);
        }

        public void showRooms () {
            if (_rooms != null && _content.getWidget() == _rooms) {
                return;
            }
            _title.setWidget(new Label(_msgs.detailTabRooms()));
            if (_rooms == null) {
                _rooms = new GroupRoomsPanel(_detail);
            }
            _content.setWidget(_rooms);
            _backButton.setVisible(true);
        }

        public void showMedals () {
            _title.setWidget(new Label(_msgs.detailTabViewMedals()));
            _content.clear();
            // TODO: Currently being mocked.  Managers will have an extra link to edit any of the
            // badges.  They'll also have an extra link at the bottom to add a new badge.
        }

        public void showAwardMedals () {
            _title.setWidget(new Label(_msgs.detailTabAwardMedals()));
            _content.clear();
            //TODO: award medal panel with searching for members, then awarding medals to a member
            // return in the search results.
        }

        protected SimplePanel _title;
        protected SimplePanel _content;
        protected Label _backButton;
        protected GroupDiscussionsPanel _discussions;
        protected PrettyTextPanel _charter;
        protected GroupMembersPanel _members;
        protected GroupRoomsPanel _rooms;
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
                ThumbBox icon = new ThumbBox(member.photo, MediaDesc.HALF_THUMBNAIL_SIZE,
                                             Pages.PEOPLE, ""+member.name.getMemberId());
                members.setWidget(ii * 2, 0, icon, 1, "Icon");
                members.getFlexCellFormatter().setRowSpan(ii * 2, 0, 2);
                if (member.rank == GroupMembership.RANK_MANAGER) {
                    members.setHTML(ii * 2, 1, _msgs.detailTopMembersManager(), 1, "Manager");
                }
                SimplePanel name = MsoyUI.createSimplePanel(Link.memberView(""
                    + member.name, member.name.getMemberId()), "Name");
                members.setWidget((ii * 2) + 1, 0, name);
            }

            // see all opens in contentPanel
            Label seeAllLink = new Label(_msgs.detailTopMembersSeeAll());
            content.add(seeAllLink);
            seeAllLink.addStyleName("SeeAll");
            seeAllLink.addStyleName("actionLabel");
            ClickListener membersClick = new ClickListener() {
                public void onClick (Widget sender) {
                    _contentPanel.showMembers();
                }
            };
            seeAllLink.addClickListener(membersClick);
        }
    }

    protected Group _group;
    protected GroupDetail _detail;
    protected GroupExtras _extras;
    protected ContentPanel _contentPanel;

    protected static final GroupsMessages _msgs = GWT.create(GroupsMessages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final GroupServiceAsync _groupsvc = (GroupServiceAsync)ServiceUtil.bind(
        GWT.create(GroupService.class), GroupService.ENTRY_POINT);
}

//
// $Id$

package client.whirleds;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.group.data.all.Group;
import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.gwt.GroupDetail;
import com.threerings.msoy.group.gwt.GroupExtras;
import com.threerings.msoy.group.gwt.GroupMemberCard;

import client.item.ShopUtil;
import client.shell.Args;
import client.shell.Page;
import client.shell.ShellMessages;
import client.shell.WorldClient;
import client.ui.MsoyUI;
import client.ui.PrettyTextPanel;
import client.ui.PromptPopup;
import client.ui.ThumbBox;
import client.util.Link;
import client.util.MediaUtil;
import client.util.MsoyCallback;

/**
 * Displays the details of a Whirled.
 */
public class WhirledDetailPanel extends FlowPanel
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
        CWhirleds.groupsvc.getGroupDetail(
            CWhirleds.ident, groupId, new MsoyCallback<GroupDetail>() {
                public void onSuccess (GroupDetail detail) {
                    setGroupDetail(detail);
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
        CWhirleds.frame.setTitle(_detail.group.name);
        _group = _detail.group;
        _extras = _detail.extras;

        // objects in top area are absolutely positioned in css
        AbsolutePanel mainDetails = new AbsolutePanel();
        mainDetails.setStyleName("MainDetails");
        add(mainDetails);

        // icon, whirled name, creator, members
        FlowPanel titleBox = new FlowPanel();
        titleBox.addStyleName("TitleBox");
        mainDetails.add(titleBox);

        // use the default logo if this group doesn't have one
        MediaDesc logoMedia = _group.logo;
        if (logoMedia == null) {
            logoMedia = Group.getDefaultGroupLogoMedia();
        }
        SimplePanel logo = new SimplePanel();
        logo.setStyleName("Logo");
        logo.setWidget(MediaUtil.createMediaView(logoMedia, MediaDesc.HALF_THUMBNAIL_SIZE));
        titleBox.add(logo);
        titleBox.add(MsoyUI.createLabel(_group.name, "Name"));

        // est. [date] by [name-link] all inline but limited width
        FlowPanel established = new FlowPanel();
        established.setStyleName("Established");
        established.add(new InlineLabel(
            CWhirleds.msgs.groupEst(_efmt.format(_group.creationDate)), false, false, true));
        established.add(new InlineLabel(_cmsgs.creatorBy(), false, false, true));
        Hyperlink creator = Link.memberView(
            _detail.creator.toString(), _detail.creator.getMemberId());
        creator.addStyleName("Creator");
        established.add(creator);
        titleBox.add(established);

        // members opens in contentPanel
        titleBox.add(new InlineLabel(_group.memberCount + " "));
        InlineLabel members = new InlineLabel(CWhirleds.msgs.detailMembers());
        titleBox.add(members);
        members.addStyleName("actionLabel");
        ClickListener membersClick = new ClickListener() {
            public void onClick (Widget sender) {
                _contentPanel.showMembers();
            }
        };
        members.addClickListener(membersClick);

        // enter and discussions buttons
        PushButton enterButton = MsoyUI.createButton(
            MsoyUI.LONG_THIN, CWhirleds.msgs.detailEnter(), Link.createListener(
                Page.WORLD, "s"+_group.homeSceneId));
        enterButton.addStyleName("EnterButton");
        mainDetails.add(enterButton);
        PushButton discussionsButton = MsoyUI.createButton(
            MsoyUI.MEDIUM_THIN, CWhirleds.msgs.detailForums(), Link.createListener(
                Page.WHIRLEDS, Args.compose("f", _group.groupId)));
        discussionsButton.addStyleName("DiscussionsButton");
        mainDetails.add(discussionsButton);

        // join, charter, shop, manage, etc
        FlowPanel actions = new FlowPanel();
        actions.setStyleName("Actions");
        mainDetails.add(actions);

        // join this whirled
        if (_detail.myRank == GroupMembership.RANK_NON_MEMBER) {
            if (Group.canJoin(_group.policy) && !CWhirleds.isGuest()) {
                actions.add(MsoyUI.createActionLabel(
                               CWhirleds.msgs.detailJoin(), new PromptPopup(
                                   CWhirleds.msgs.detailJoinPrompt(), joinGroup()).setContext(
                                       CWhirleds.msgs.detailJoinContext(_group.name))));
            }
        }
        // leave this whirled & invite others to it
        else {
            actions.add(MsoyUI.createActionLabel(
                           CWhirleds.msgs.detailLeave(), new PromptPopup(
                               CWhirleds.msgs.detailLeavePrompt(_group.name),
                               removeMember(CWhirleds.getMemberId()))));
        }
        if (Group.canInvite(detail.group.policy, detail.myRank)) {
            String args = Args.compose("w", "g", ""+_detail.group.groupId);
            actions.add(MsoyUI.createActionLabel(
                CWhirleds.msgs.detailInvite(), Link.createListener(Page.MAIL, args)));
        }

        // shop
        if (_extras.catalogTag != null && !_extras.catalogTag.equals("")) {
            String args = ShopUtil.composeArgs(
                _extras.catalogItemType, _extras.catalogTag, null, 0);
            actions.add(MsoyUI.createActionLabel(
                CWhirleds.msgs.detailShop(), Link.createListener(Page.SHOP, args)));
        }

        // read charter
        Label readCharter = new Label(CWhirleds.msgs.detailReadCharter());
        actions.add(readCharter);
        readCharter.addStyleName("actionLabel");
        ClickListener charterClick = new ClickListener() {
            public void onClick (Widget sender) {
                _contentPanel.showCharter();
            }
        };
        readCharter.addClickListener(charterClick);

        // edit this whirled & manage rooms
        if (_detail.myRank == GroupMembership.RANK_MANAGER) {
            FlowPanel managerActions = new FlowPanel();
            managerActions.setStyleName("ManagerActions");
            actions.add(managerActions);

            String args = Args.compose("edit", _group.groupId);
            Label editWhirled = MsoyUI.createActionLabel(
                CWhirleds.msgs.detailEdit(), Link.createListener(Page.WHIRLEDS, args));
            editWhirled.addStyleName("inline");
            managerActions.add(editWhirled);

            managerActions.add(new InlineLabel(" | "));

            InlineLabel manageRooms = new InlineLabel(CWhirleds.msgs.detailManageRooms());
            managerActions.add(manageRooms);
            manageRooms.addStyleName("actionLabel");
            ClickListener roomsClick = new ClickListener() {
                public void onClick (Widget sender) {
                    _contentPanel.showRooms();
                }
            };
            manageRooms.addClickListener(roomsClick);
        }

        // screenshot, #online, blurb
        FlowPanel screenshot = new FlowPanel();
        screenshot.setStyleName("ScreenshotBox");
        mainDetails.add(screenshot);

        // display a screenshot of the whirled that can be clicked for a live view
        screenshot.add(_whirledViewPanel = new SimplePanel());
        _whirledViewPanel.setStyleName("Screenshot");
        ClickListener liveVewClick = new ClickListener() {
            public void onClick (Widget sender) {
                // replace the image with a live view
                WorldClient.displayFeaturedPlace(_group.homeSceneId, _whirledViewPanel);
            }
        };
        final Image clickToPlayImage = MsoyUI.createActionImage(
                "/images/landing/whirled_click_here.jpg", "", liveVewClick);
        _whirledViewPanel.add(clickToPlayImage);

        if (_detail.population > 0) {
            HTML online = new HTML(CWhirleds.msgs.featuredOnline(""+_detail.population));
            online.setStyleName("Online");
            screenshot.add(online);
        }

        if (_group.blurb != null) {
            HTML blurb = new HTML(_group.blurb);
            blurb.setStyleName("Blurb");
            screenshot.add(blurb);
        }

        HorizontalPanel lowerArea = new HorizontalPanel();
        add(lowerArea);
        // content panel defaults to discussions
        lowerArea.add(_contentPanel = new ContentPanel());
        _contentPanel.showDiscussions();
        // list managers and some members
        lowerArea.add(new TopMembersPanel());
    }

    protected Command removeMember (final int memberId)
    {
        return new Command() {
            public void execute () {
                CWhirleds.groupsvc.leaveGroup(CWhirleds.ident, _group.groupId, memberId, refresh());
            }
        };
    }

    protected Command joinGroup ()
    {
        return new Command() {
            public void execute () {
                CWhirleds.groupsvc.joinGroup(CWhirleds.ident, _group.groupId, refresh());
            }
        };
    }

    protected MsoyCallback<Void> refresh ()
    {
        return new MsoyCallback<Void>() {
            public void onSuccess (Void result) {
                loadGroup(_group.groupId);
            }
        };
    }

    /**
     * Displays multiple panels: Discussions, Charter, Members, Rooms.  Panels
     * are cached and not regenerated when swapping back to one.
     */
    protected class ContentPanel extends FlowPanel
    {
        public ContentPanel () {
            setStyleName("ContentPanel");
            add(_title = new SimplePanel());
            _title.setStyleName("ContentPanelTitle");

            // contains content and discussions button for css min-height
            FlowPanel container = new FlowPanel();
            container.setStyleName("ContentPanelContainer");
            add(container);
            container.add(_content = new SimplePanel());
            _content.setStyleName("ContentPanelContent");

            // back to discussions button hidden by default
            container.add(_backButton = new Label(CWhirleds.msgs.detailBackToDiscussions()));
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
            _title.setWidget(new Label(CWhirleds.msgs.detailTabDiscussions()));
            if (_discussions == null) {
                _discussions = new WhirledDiscussionsPanel(_detail);
            }
            _content.setWidget(_discussions);
            _backButton.setVisible(false);
        }

        public void showCharter () {
            if (_charter != null && _content.getWidget() == _charter) {
                return;
            }
            _title.setWidget(new Label(CWhirleds.msgs.detailTabCharter()));
            if (_charter == null) {
                String charterText = (_extras.charter == null) ?
                    CWhirleds.msgs.detailNoCharter() : _extras.charter;
                _charter = new PrettyTextPanel(charterText);
            }
            _content.setWidget(_charter);
            _backButton.setVisible(true);
        }

        public void showMembers () {
            if (_members != null && _content.getWidget() == _members) {
                return;
            }
            _title.setWidget(new Label(CWhirleds.msgs.detailTabMembers()));
            if (_members == null) {
                _members = new WhirledMembersPanel(_detail);
            }
            _content.setWidget(_members);
            _backButton.setVisible(true);
        }

        public void showRooms () {
            if (_rooms != null && _content.getWidget() == _rooms) {
                return;
            }
            _title.setWidget(new Label(CWhirleds.msgs.detailTabRooms()));
            if (_rooms == null) {
                _rooms = new WhirledRoomsPanel(_detail);
            }
            _content.setWidget(_rooms);
            _backButton.setVisible(true);
        }

        protected SimplePanel _title;
        protected SimplePanel _content;
        protected Label _backButton;
        protected WhirledDiscussionsPanel _discussions;
        protected PrettyTextPanel _charter;
        protected WhirledMembersPanel _members;
        protected WhirledRoomsPanel _rooms;
    }

    /**
     * Displays managers and most recently online members
     */
    protected class TopMembersPanel extends FlowPanel
    {
        public TopMembersPanel () {
            setStyleName("TopMembersPanel");
            add(MsoyUI.createSimplePanel("Title",
                new HTML(CWhirleds.msgs.detailTopMembersTitle())));

            FlowPanel content = new FlowPanel();
            content.setStyleName("Content");
            add(content);

            SmartTable members = new SmartTable("Members", 0, 0);
            content.add(members);
            for (int ii = 0; ii < _detail.topMembers.size(); ii++) {
                GroupMemberCard member = _detail.topMembers.get(ii);
                ClickListener iconClick = Link.createListener(
                    Page.PEOPLE, "" + member.name.getMemberId());
                ThumbBox icon =
                    new ThumbBox(member.photo, MediaDesc.HALF_THUMBNAIL_SIZE, iconClick);
                members.setWidget(ii * 2, 0, icon, 1, "Icon");
                members.getFlexCellFormatter().setRowSpan(ii * 2, 0, 2);
                if (member.rank == GroupMembership.RANK_MANAGER) {
                    members.setHTML(
                        ii * 2, 1, CWhirleds.msgs.detailTopMembersManager(), 1, "Manager");
                }
                SimplePanel name = MsoyUI.createSimplePanel(
                  "Name", Link.memberView("" + member.name, member.name.getMemberId()));
                members.setWidget((ii * 2) + 1, 0, name);
            }

            // see all opens in contentPanel
            Label seeAllLink = new Label(CWhirleds.msgs.detailTopMembersSeeAll());
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
    protected SimplePanel _whirledViewPanel;
    protected ContentPanel _contentPanel;

    protected static final SimpleDateFormat _efmt = new SimpleDateFormat("MMM dd, yyyy");
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}

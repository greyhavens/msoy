//
// $Id$

package client.groups;

import java.util.Collections;
import java.util.List;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.PagedWidget;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.VizMemberName;
import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.data.all.Medal;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;
import com.threerings.msoy.group.gwt.GroupService.MedalOwners;
import com.threerings.msoy.group.gwt.GroupService.MedalsResult;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.ui.RoundBox;
import client.util.Link;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

public class MedalListPanel extends FlowPanel
{
    public MedalListPanel (int groupId)
    {
        setStyleName("medalListPanel");
        _groupId = groupId;
        _groupsvc.getAwardedMedals(groupId, new MsoyCallback<MedalsResult>() {
            public void onSuccess (MedalsResult result) {
                _groupName = result.groupName;
                _rank = result.rank;
                displayMedals(result.medals);
            }
        });
    }

    protected void displayMedals (List<MedalOwners> medals)
    {
        Collections.sort(medals);

        AbsolutePanel header = MsoyUI.createAbsolutePanel("Header");
        header.add(new Image("/images/group/medal_title.png"), 30, 0);
        header.add(MsoyUI.createLabel(_msgs.medalListIntro(), "Intro"), 235, 10);
        header.add(new Image("/images/group/medal_tofu.png"), 466, 0);
        add(header);

        RoundBox medalList = new RoundBox(RoundBox.MEDIUM_BLUE);
        add(medalList);
        medalList.addStyleName("MedalList");
        HorizontalPanel title = new HorizontalPanel();
        title.setStyleName("MedalTitle");
        title.add(MsoyUI.createActionLabel(_groupName.toString(), "GroupName", Link.createListener(
            Pages.GROUPS, GroupsPage.Nav.DETAIL.composeArgs(_groupName.getGroupId()))));
        if (CShell.isSupport() || _rank == GroupMembership.RANK_MANAGER) {
            title.add(MsoyUI.createActionLabel(
                _msgs.medalListAddMedal(), "AddMedal", Link.createListener(
                    Pages.GROUPS, GroupsPage.Nav.CREATEMEDAL.composeArgs(_groupId))));
        }
        medalList.add(title);

        PagedGrid<MedalOwners> grid =
            new PagedGrid<MedalOwners>(MEDALS_ROWS, MEDALS_COLS, PagedWidget.NAV_ON_BOTTOM) {
                @Override protected Widget createWidget (MedalOwners owners) {
                    return new MedalOwnerWidget(owners);
                }

                @Override protected String getEmptyMessage () {
                    return _msgs.medalListEmptyList();
                }
            };
        grid.setModel(new SimpleDataModel<MedalOwners>(medals), 0);
        medalList.add(grid);
    }

    protected class MedalOwnerWidget extends HorizontalPanel
        implements ClickListener
    {
        public MedalOwnerWidget (MedalOwners owners)
        {
            setStyleName("MedalOwnerWidget");
            add(new MedalWidget(owners.medal));

            _ownerWidgets = new FlowPanel();
            _ownerWidgets.setStyleName("OwnerWidgets");
            add(_ownerWidgets);
            setCellWidth(_ownerWidgets, "100%");
            if (owners.owners.size() == 0) {
                _ownerWidgets.add(MsoyUI.createLabel(_msgs.medalListNotAwarded(), "NotAwarded"));
                return;
            }

            for (VizMemberName owner : owners.owners) {
                FlowPanel ownerWidget = new FlowPanel();
                ownerWidget.setStyleName("OwnerWidget");
                ownerWidget.add(MediaUtil.createMediaView(
                    owner.getPhoto(), MediaDesc.THUMBNAIL_SIZE,
                    Link.createListener(Pages.PEOPLE, ""+owner.getMemberId())));
                ownerWidget.add(Link.create(owner.toString(), "OwnerLink", Pages.PEOPLE,
                    ""+owner.getMemberId(), false));
                _ownerWidgets.add(ownerWidget);
                if (_ownerWidgets.getWidgetCount() > 4) {
                    ownerWidget.addStyleName("Hidden");
                }
            }

            if (_ownerWidgets.getWidgetCount() > 4) {
                _ownerWidgets.add(MsoyUI.createActionLabel(
                    _msgs.medalListSeeAll(""+_ownerWidgets.getWidgetCount()), "SeeAllLink", this));
            }
        }

        public void onClick (Widget sender)
        {
            int ownerCount = _ownerWidgets.getWidgetCount();
            if (_hidden) {
                for (int ii = 0; ii < ownerCount; ii++) {
                    _ownerWidgets.getWidget(ii).removeStyleName("Hidden");
                }

            } else {
                for (int ii = 4; ii < ownerCount; ii++) {
                    Widget ownerWidget = _ownerWidgets.getWidget(ii);
                    if (!(ownerWidget instanceof Label)) {
                        ownerWidget.addStyleName("Hidden");
                    }
                }
            }

            _hidden = !_hidden;
            ((Label)sender).setText(
                _hidden ? _msgs.medalListSeeAll(""+(ownerCount - 1)) : _msgs.medalListHide());
        }

        protected FlowPanel _ownerWidgets;
        protected boolean _hidden = true;
    }

    protected class MedalWidget extends FlowPanel
    {
        public MedalWidget (Medal medal)
        {
            setStyleName("MedalWidget");
            Widget icon = MediaUtil.createMediaView(medal.icon, MediaDesc.THUMBNAIL_SIZE);
            icon.addStyleName("Icon");
            add(icon);
            add(MsoyUI.createLabel(medal.name, "Name"));
            add(MsoyUI.createLabel(medal.description, "Description"));
            if (CShell.isSupport() || _rank == GroupMembership.RANK_MANAGER) {
                add(MsoyUI.createActionLabel(_msgs.medalListEdit(), "Edit", Link.createListener(
                    Pages.GROUPS, GroupsPage.Nav.EDITMEDAL.composeArgs(medal.medalId))));
            }
        }
    }

    protected int _groupId;
    protected GroupName _groupName;
    protected byte _rank;

    protected static final int MEDALS_COLS = 1;
    protected static final int MEDALS_ROWS = 6;

    protected static final GroupsMessages _msgs = GWT.create(GroupsMessages.class);
    protected static final GroupServiceAsync _groupsvc = (GroupServiceAsync)ServiceUtil.bind(
        GWT.create(GroupService.class), GroupService.ENTRY_POINT);
}

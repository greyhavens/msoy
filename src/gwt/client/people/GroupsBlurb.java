//
// $Id$

package client.people;

import java.util.List;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.client.ProfileService;
import com.threerings.msoy.web.data.GroupCard;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.SimpleDataModel;

import client.msgs.GroupInvite;
import client.msgs.MailComposition;
import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.ClickCallback;
import client.util.MediaUtil;
import client.util.MsoyUI;

/**
 * Displays a list of the groups of which a person is a member.
 */
public class GroupsBlurb extends Blurb
{
    // @Override // from Blurb
    public boolean shouldDisplay (ProfileService.ProfileResult pdata)
    {
        return (pdata.groups != null);
    }

    // @Override // from Blurb
    protected void didInit (ProfileService.ProfileResult pdata)
    {
        setHeader(CPeople.msgs.groupsTitle());
        setContent(new GroupsGrid(pdata.groups));
    }

    protected static Widget createEmptyTable (String message, String link, String page, String args)
    {
        FlowPanel bits = new FlowPanel();
        bits.add(WidgetUtil.makeShim(25, 50));
        bits.add(new HTML(message));
        bits.add(WidgetUtil.makeShim(25, 25));
        bits.add(Application.createLink(link, page, args));
        bits.setStyleName("Empty");
        return bits;
    }

    protected class GroupsGrid extends PagedGrid
    {
        public GroupsGrid (List groups) {
            super(GROUP_ROWS, GROUP_COLUMNS, PagedGrid.NAV_ON_BOTTOM);
            addStyleName("groupsBlurb");
            addStyleName("dottedGrid");
            setModel(new SimpleDataModel(groups), 0);
        }

        // @Override // from PagedGrid
        protected String getEmptyMessage ()
        {
            return CPeople.msgs.notInGroupsOther();
        }

        // @Override // from PagedGrid
        protected Widget createEmptyContents ()
        {
            if (CPeople.getMemberId() != _name.getMemberId()) {
                return super.createEmptyContents();
            }
            return createEmptyTable(CPeople.msgs.notInGroupsSelf(),
                                    CPeople.msgs.notInGroupsJoin(), Page.WHIRLEDS, "");
        }

        // @Override // from PagedGrid
        protected boolean displayNavi (int items)
        {
            return true;
        }

        // @Override // from PagedGrid
        protected Widget createWidget (Object item)
        {
            return new GroupWidget((GroupCard)item);
        }

        // @Override // from PagedGrid
        protected void addCustomControls (FlexTable controls)
        {
            if (CPeople.getMemberId() > 0 && CPeople.getMemberId() != _name.getMemberId()) {
                Button inviteButton = new Button(CPeople.msgs.inviteToGroup());
                new ClickCallback(inviteButton) {
                    public boolean callService () {
                        CPeople.groupsvc.getMembershipGroups(
                            CPeople.ident, CPeople.getMemberId(), true, this);
                        return true;
                    }
                    public boolean gotResult (Object result) {
                        List inviteGroups = (List) result;
                        if (inviteGroups.size() == 0) {
                            MsoyUI.infoNear(CPeople.msgs.haveNoGroups(), _trigger);
                        } else {
                            new MailComposition(_name, "Join this group!",
                                                new GroupInvite.Composer(inviteGroups),
                                                "Check out this scrumptious group.").show();
                        }
                        return true;
                    }
                };
                controls.setWidget(0, 0, inviteButton);
            }
        }

        // @Override // from PagedGrid
        protected boolean padToFullPage ()
        {
            return true;
        }
    }

    protected class GroupWidget extends FlexTable
    {
        public GroupWidget (final GroupCard card) {
            setStyleName("profileWidget");
            setCellPadding(0);
            setCellSpacing(0);

            ClickListener profileClick = new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.WHIRLEDS, Args.compose("d", card.name.getGroupId()));
                }
            };

            // avoid forcing this table column to 80 pixels
            SimplePanel photoPanel = new SimplePanel();
            photoPanel.setStyleName("Photo");
            if (card != null) {
                photoPanel.add(MediaUtil.createMediaView(
                                   card.logo, MediaDesc.THUMBNAIL_SIZE, profileClick));
            }
            setWidget(0, 0, photoPanel);
            getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_CENTER);

            Label nameLabel =  new Label(card == null ? "" : card.name.toString());
            nameLabel.setStyleName("Name");
            if (card != null) {
                nameLabel.addStyleName("actionLabel");
                nameLabel.addClickListener(profileClick);
            }
            setWidget(1, 0, nameLabel);
            getFlexCellFormatter().setHorizontalAlignment(1, 0, HasAlignment.ALIGN_CENTER);
        }
    }

    protected static final int GROUP_COLUMNS = 3;
    protected static final int GROUP_ROWS = 2;
}

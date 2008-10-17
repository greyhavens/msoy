//
// $Id$

package client.people;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.profile.gwt.ProfileService;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.ui.ThumbBox;
import client.util.Link;

/**
 * Displays a person's friends list.
 */
public class FriendsBlurb extends Blurb
{
    @Override // from Blurb
    public boolean shouldDisplay (ProfileService.ProfileResult pdata)
    {
        return (pdata.friends != null);
    }

    @Override // from Blurb
    public void init (ProfileService.ProfileResult pdata)
    {
        super.init(pdata);
        setHeader(_msgs.friendsTitle());

        if (pdata.friends.size() == 0) {
            if (CShell.getMemberId() != _name.getMemberId()) {
                setContent(new Label(_msgs.noFriendsOther()));
            } else {
                setContent(GroupsBlurb.createEmptyTable(
                               _msgs.noFriendsSelf(), _msgs.noFriendsFindEm(),
                               Pages.PEOPLE, "search"));
            }
        } else {
            SmartTable grid = new SmartTable();
            for (int ii = 0; ii < pdata.friends.size(); ii++) {
                grid.setWidget(0, ii, new FriendWidget(pdata.friends.get(ii)));
            }
            setContent(grid);
        }

        setFooterLink(_msgs.seeAllFriends("" + pdata.totalFriendCount),
                      Pages.PEOPLE, Args.compose("f", pdata.name.getMemberId()));
    }

    protected class FriendWidget extends FlowPanel
    {
        public FriendWidget (final MemberCard card)
        {
            setStyleName("Friend");
            ClickListener profileClick = new ClickListener() {
                public void onClick (Widget sender) {
                    Link.go(Pages.PEOPLE, "" + card.name.getMemberId());
                }
            };
            add(new ThumbBox(card.photo, profileClick));
            add(MsoyUI.createActionLabel(card.name.toString(), profileClick));
        }
    }

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
}

//
// $Id$

package client.people;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.person.gwt.ProfileService;
import com.threerings.msoy.web.data.MemberCard;

import client.shell.Args;
import client.shell.Page;
import client.util.Link;
import client.util.MsoyUI;
import client.util.ThumbBox;

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
        setHeader(CPeople.msgs.friendsTitle());

        if (pdata.friends.size() == 0) {
            if (CPeople.getMemberId() != _name.getMemberId()) {
                setContent(new Label(CPeople.msgs.noFriendsOther()));
            } else {
                setContent(GroupsBlurb.createEmptyTable(
                               CPeople.msgs.noFriendsSelf(), CPeople.msgs.noFriendsFindEm(),
                               Page.PEOPLE, "search"));
            }
        } else {
            SmartTable grid = new SmartTable();
            for (int ii = 0; ii < pdata.friends.size(); ii++) {
                grid.setWidget(0, ii, new FriendWidget((MemberCard)pdata.friends.get(ii)));
            }
            setContent(grid);
        }

        setFooterLink(CPeople.msgs.seeAllFriends("" + pdata.totalFriendCount),
                      Page.PEOPLE, Args.compose("f", pdata.name.getMemberId()));
    }

    protected class FriendWidget extends FlowPanel
    {
        public FriendWidget (final MemberCard card)
        {
            setStyleName("Friend");
            ClickListener profileClick = new ClickListener() {
                public void onClick (Widget sender) {
                    Link.go(Page.PEOPLE, "" + card.name.getMemberId());
                }
            };
            add(new ThumbBox(card.photo, profileClick));
            add(MsoyUI.createActionLabel(card.name.toString(), profileClick));
        }
    }
}

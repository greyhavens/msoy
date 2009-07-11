//
// $Id$

package client.facebook;

import client.ui.MsoyUI;
import client.ui.ThumbBox;
import client.util.Link;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbsolutePanel;

import com.threerings.msoy.data.all.MediaDesc;

import com.threerings.msoy.web.gwt.Pages;

import com.threerings.msoy.facebook.gwt.FacebookFriendInfo;
import com.threerings.msoy.facebook.gwt.FacebookFriendInfo.Thumbnail;

/**
 * Displays a single {@link FacebookFriendInfo}.
 */
public class FBFriendPanel extends AbsolutePanel
{
    /**
     * Creates a new friend panel.
     */
    public FBFriendPanel (FacebookFriendInfo info, int rank)
    {
        setStyleName("friendInfo");
        addStyleName(rank % 2 == 0 ? "friendInfoEven" : "friendInfoOdd");
        if (info == null) {
            add(MsoyUI.createLabel("TODO:", null));
            add(MsoyUI.createLabel("Invite", null));
            return;
        }

        int halfSize = MediaDesc.HALF_THUMBNAIL_SIZE;
        String uid = String.valueOf(info.facebookUid);
        add(MsoyUI.createFlowPanel("Name",
            new FBMLPanel("name", "uid", uid, "linked", "false", "firstnameonly", "true")));
        // TODO: medal graphics for 1,2 & 3
        add(MsoyUI.createLabel("" + rank, "Rank"));
        add(MsoyUI.createFlowPanel("Photo",
            new FBMLPanel("profile-pic", "uid", uid, "linked", "false", "size", "square")));
        add(MsoyUI.createLabel(convertToText(info.coins), "Coins"));
        add(MsoyUI.createFlowPanel("CoinIcon"));
        Thumbnail lastGame = info.lastGame;
        if (lastGame != null) {
            add(new ThumbBox(lastGame.media, halfSize, Pages.GAMES, "d", lastGame.id));
            add(Link.create(lastGame.name, "Game", Pages.GAMES, "d", lastGame.id));
        }
        add(MsoyUI.createLabel(convertToText(info.trophyCount), "Trophies"));
        add(MsoyUI.createFlowPanel("TrophyIcon"));
    }

    protected static String convertToText (int number)
    {
        if (number < 1000) {
            return String.valueOf(number);
        }
        if (number < 1000000) {
            return _msgs.kilo(String.valueOf(number / 1000));
        }
        if (number < 1000000000) {
            return _msgs.mega(String.valueOf(number / 1000000));
        }
        return _msgs.giga(String.valueOf(number / 1000000000));
    }

    protected static final FacebookMessages _msgs = GWT.create(FacebookMessages.class);
}

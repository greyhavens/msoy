//
// $Id$

package client.facebook;

import client.ui.MsoyUI;
import client.ui.ThumbBox;

import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.msoy.data.all.MediaDesc;

import com.threerings.msoy.web.gwt.Pages;

import com.threerings.msoy.facebook.gwt.FacebookFriendInfo;
import com.threerings.msoy.facebook.gwt.FacebookFriendInfo.Thumbnail;

/**
 * Displays a single {@link FacebookFriendInfo}.
 */
public class FBFriendPanel extends FlowPanel
{
    /**
     * Creates a new friend panel.
     */
    public FBFriendPanel (FacebookFriendInfo info, int rank)
    {
        setStyleName("FriendInfo");
        int halfSize = MediaDesc.HALF_THUMBNAIL_SIZE;
        String uid = String.valueOf(info.facebookUid);
        add(new FBMLPanel("name", "uid", uid, "linked", "false"));
        add(MsoyUI.createLabel("Rank " + (rank + 1), null));
        add(new FBMLPanel("profile-pic", "uid", uid, "linked", "false"));
        add(MsoyUI.createLabel("Level " + info.level, null));
        add(MsoyUI.createLabel("Coins " + info.coins, null));
        Thumbnail lastGame = info.lastGame;
        if (lastGame != null) {
            add(new ThumbBox(lastGame.media, halfSize, Pages.GAMES, "d", lastGame.id));
            add(MsoyUI.createLabel(lastGame.name, null));
        }
        add(MsoyUI.createLabel("Trophies " + info.trophyCount, null));
    }
}

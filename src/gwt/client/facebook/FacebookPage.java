//
// $Id$

package client.facebook;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.Page;

/**
 * Displays Facebook stuff like a list of friends who have played whirled and some status.
 */
public class FacebookPage extends Page
{
    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        String action = args.get(0, "");
        if (action.equals("friends")) {
            setContent("Friends", new FBFriendBar());

        } else if (action.equals("game")) {
            // TODO: specific friends bar mode for games
            // int gameId = args.get(1, 0);
            setContent("Friends", new FBFriendBar());

        } else if (action.equals("invite")) {
            setContent("Invite", FBInvitePanel.createGeneric());

        } else if (action.equals("challenge")) {
            int gameId = args.get(1, 0);
            setContent("Challenge", FBInvitePanel.createChallenge(gameId));
        }
    }

    @Override // from Page
    public Pages getPageId ()
    {
        return Pages.FACEBOOK;
    }
}

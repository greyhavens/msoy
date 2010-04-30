//
// $Id$

package client.facebook;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import com.threerings.msoy.facebook.gwt.FacebookGame;
import com.threerings.msoy.facebook.gwt.FacebookService;
import com.threerings.msoy.facebook.gwt.FacebookServiceAsync;
import com.threerings.msoy.facebook.gwt.FacebookService.InviteInfo;

import client.shell.Page;
import client.shell.ShellMessages;

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
            setContent("Invite", FBRequestPanel.createInvite());
        }
    }

    @Override // from Page
    public Pages getPageId ()
    {
        return Pages.FACEBOOK;
    }

    // we just need to query this once
    protected FacebookGame _game;
    protected InviteInfo _gameInviteInfo;

    protected FacebookServiceAsync _fbsvc = GWT.create(FacebookService.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}

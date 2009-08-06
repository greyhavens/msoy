//
// $Id$

package client.facebook;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.web.gwt.ArgNames;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import com.threerings.msoy.facebook.gwt.FacebookGame;
import com.threerings.msoy.facebook.gwt.FacebookService;
import com.threerings.msoy.facebook.gwt.FacebookServiceAsync;
import com.threerings.msoy.facebook.gwt.FacebookService.InviteInfo;

import client.shell.Page;
import client.shell.ShellMessages;
import client.ui.MsoyUI;
import client.util.InfoCallback;

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

        } else if (action.equals(ArgNames.FB_GAME_CHALLENGE)) {
            showChallenge(new FacebookGame(args.get(1, 0)), args.get(2, ""));

        } else if (action.equals(ArgNames.FB_MOCHI_CHALLENGE)) {
            showChallenge(new FacebookGame(args.get(1, "")), args.get(2, ""));
        }
    }

    @Override // from Page
    public Pages getPageId ()
    {
        return Pages.FACEBOOK;
    }

    protected void showChallenge (final FacebookGame game, final String mode)
    {
        if (_gameInviteInfo == null || !_game.equals(game)) {
            setContent(MsoyUI.createLabel(_cmsgs.tagLoading(), "Loading"));
            _game = game;
            _gameInviteInfo = null;
            _fbsvc.getInviteInfo(_game.id, new InfoCallback<InviteInfo>() {
                public void onSuccess (InviteInfo info) {
                    _gameInviteInfo = info;
                    showChallenge(_game, mode);
                }
            });
            return;
        }

        // see which phase of the challenge flow we are in and show the appropriate screen
        // in production, we just go straight to pick mode and show a request form for now
        // TODO: finish the other options and enable in production

        if (mode.equals(ArgNames.FB_CHALLENGE_FRIENDS)) {
            // TODO
        } else if (mode.equals(ArgNames.FB_CHALLENGE_APP_FRIENDS)) {
            // TODO
        } else if (true || mode.equals(ArgNames.FB_CHALLENGE_PICK)) {
            setContent("Challenge",
                FBRequestPanel.createChallenge(game, _gameInviteInfo));
        } else {
            setContent(new FBChallengeSelectPanel(game, _gameInviteInfo.gameName));
        }
    }

    // we just need to query this once
    protected FacebookGame _game;
    protected InviteInfo _gameInviteInfo;

    protected FacebookServiceAsync _fbsvc = GWT.create(FacebookService.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}

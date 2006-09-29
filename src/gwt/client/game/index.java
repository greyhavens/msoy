//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.web.data.LaunchConfig;
import com.threerings.msoy.web.data.WebCreds;

import client.MsoyEntryPoint;

/**
 * Displays a page that allows a player to play a particular game. If it's
 * single player the game is shown, if it's multiplayer the lobby is first
 * shown where the player can find opponents against which to play.
 */
public class index extends MsoyEntryPoint
    implements HistoryListener
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public MsoyEntryPoint createEntryPoint () {
                return new index();
            }
        };
    }

    // @Override // from MsoyEntryPoint
    public void onPageLoad ()
    {
        History.addHistoryListener(this);
        String initToken = History.getToken();
        if (initToken.length() > 0) {
            onHistoryChanged(initToken);
        } else {
            // TODO: display a list of this player's games
        }
    }

    // from interface HistoryListener
    public void onHistoryChanged (String token)
    {
        try {
            displayGamePage(Integer.parseInt(token));
        } catch (Exception e) {
            // TODO: display error
        }
    }

    // @Override // from MsoyEntryPoint
    protected void didLogon (WebCreds creds)
    {
        super.didLogon(creds);

        // TODO: pass credentials to game if appropriate
    }

    protected void displayGamePage (int gameId)
    {
        // load up the information needed to launch the game
        _ctx.gamesvc.loadLaunchConfig(_ctx.creds, gameId, new AsyncCallback() {
            public void onSuccess (Object result) {
                setContent(new GamePanel(_ctx, (LaunchConfig)result));
            }

            public void onFailure (Throwable cause) {
                // TODO: display friendly error
                GWT.log("Failed to load game", cause);
            }
        });
    }
}

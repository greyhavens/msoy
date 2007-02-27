//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

import com.threerings.msoy.web.client.GameService;
import com.threerings.msoy.web.client.GameServiceAsync;
import com.threerings.msoy.web.data.LaunchConfig;
import com.threerings.msoy.web.data.WebCreds;

import client.shell.MsoyEntryPoint;

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
    protected String getPageId ()
    {
        return "game";
    }

    // @Override // from MsoyEntryPoint
    protected void initContext ()
    {
        super.initContext();

        // wire up our remote services
        CGame.gamesvc = (GameServiceAsync)GWT.create(GameService.class);
        ((ServiceDefTarget)CGame.gamesvc).setServiceEntryPoint("/gamesvc");

        // load up our translation dictionaries
        CGame.msgs = (GameMessages)GWT.create(GameMessages.class);
        CGame.dmsgs = (GameDynMessages)GWT.create(GameDynMessages.class);
    }

    // @Override from MsoyEntryPoint
    protected void onPageLoad ()
    {
        History.addHistoryListener(this);
    }

    // @Override from MsoyEntryPoint
    protected boolean didLogon (WebCreds creds)
    {
        boolean header = super.didLogon(creds);
        onHistoryChanged(History.getToken());
        return header;
    }

    // @Override from MsoyEntryPoint
    protected void didLogoff ()
    {
        super.didLogoff();
        onHistoryChanged(History.getToken());
    }

    protected void displayGamePage (int gameId)
    {
        // load up the information needed to launch the game
        CGame.gamesvc.loadLaunchConfig(CGame.creds, gameId, new AsyncCallback() {
            public void onSuccess (Object result) {
                setContent(new GamePanel((LaunchConfig)result));
            }
            public void onFailure (Throwable cause) {
                CGame.serverError(cause);
            }
        });
    }
}

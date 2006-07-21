package com.threerings.msoy.client {

import mx.controls.Button;

import com.threerings.util.Controller;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.ClientObserver;

import com.threerings.msoy.data.MsoyCredentials;

public class MsoyController extends Controller
    implements ClientObserver
{
    /** Command to log us on. */
    public static const LOGON :String = "Logon";

    /** Command to display the friends list. */
    public static const SHOW_FRIENDS :String = "ShowFriends";

    /**
     * Create the msoy controller.
     */
    public function MsoyController (ctx :MsoyContext, topPanel :TopPanel)
    {
        _ctx = ctx;
        _ctx.getClient().addClientObserver(this);
        _topPanel = topPanel;
        setControlledPanel(topPanel);
    }

    /**
     * Handle the SHOW_FRIENDS command.
     */
    public function handleShowFriends (btn :Button) :void
    {
        _topPanel.showFriends(btn.selected);
    }

    /**
     * Handle the LOGON command.
     */
    public function handleLogon (creds :MsoyCredentials) :void
    {
        _ctx.getClient().logoff(false);
        _topPanel.callLater(function () :void {
            var client :Client = _ctx.getClient();
            if (creds != null) {
                client.setCredentials(creds);
            }
            client.logon();
        });
    }

    // from ClientObserver
    public function clientDidLogon (event :ClientEvent) :void
    {
        // TODO
        // for now, all we do is move to a starter scene
        _ctx.getSceneDirector().moveTo(1);
    }

    // from ClientObserver
    public function clientObjectDidChange (event :ClientEvent) :void
    {
        // nada
    }

    // from ClientObserver
    public function clientDidLogoff (event :ClientEvent) :void
    {
        _topPanel.setPlaceView(new DisconnectedPanel(_ctx, _logoffMessage));
        _logoffMessage = null;
    }

    // from ClientObserver
    public function clientFailedToLogon (event :ClientEvent) :void
    {
        // nada
    }

    // from ClientObserver
    public function clientConnectionFailed (event :ClientEvent) :void
    {
        _logoffMessage = _ctx.xlate("m.lost_connection");
    }

    // from ClientObserver
    public function clientWillLogoff (event :ClientEvent) :void
    {
        // nada
    }

    /** Provides access to client-side directors and services. */
    protected var _ctx :MsoyContext;

    /** The topmost panel in the msoy client. */
    protected var _topPanel :TopPanel;

    /** A special logoff message to use when we disconnect. */
    protected var _logoffMessage :String;
}
}

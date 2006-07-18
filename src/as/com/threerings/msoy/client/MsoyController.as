package com.threerings.msoy.client {

import com.threerings.util.Controller;

public class MsoyController extends Controller
{
    /** Command to display the friends list. */
    public static const SHOW_FRIENDS :String = "showFriends";

    /**
     * Create the msoy controller.
     */
    public function MsoyController (ctx :MsoyContext, topPanel :TopPanel)
    {
        _ctx = ctx;
        _topPanel = topPanel;
        setControlledPanel(topPanel);
    }

    override public function handleAction (cmd :String, arg :Object) :Boolean
    {
        if (cmd == SHOW_FRIENDS) {
            _topPanel.showFriends();

        } else {
            return super.handleAction(cmd, arg);
        }

        return true; // for handled commands
    }

    /** Provides access to client-side directors and services. */
    protected var _ctx :MsoyContext;

    /** The topmost panel in the msoy client. */
    protected var _topPanel :TopPanel;
}
}

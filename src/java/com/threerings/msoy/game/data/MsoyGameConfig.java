//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.util.ActionScript;

import com.threerings.crowd.client.PlaceController;

import com.threerings.ezgame.data.EZGameConfig;

/**
 * A game config for a metasoy game.
 */
public class MsoyGameConfig extends EZGameConfig
{
    /** The controller to use, or null for MsoyGameController. */
    public String controller;

    /** The manager to use, or null for MsoyGameManager. */
    public String manager;

    @Override
    public PlaceController createController ()
    {
        try {
            return (PlaceController) Class.forName(controller).newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override @ActionScript(omit=true)
    public String getManagerClassName ()
    {
        return (manager != null) ? manager : "com.threerings.msoy.game.server.MsoyGameManager";
    }
}

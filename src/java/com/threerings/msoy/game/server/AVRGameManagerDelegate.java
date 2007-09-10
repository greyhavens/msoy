//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.parlor.game.server.GameManager;
import com.threerings.parlor.game.server.GameManagerDelegate;

import com.threerings.msoy.server.MsoyServer;

public class AVRGameManagerDelegate extends GameManagerDelegate
{
    public AVRGameManagerDelegate (GameManager gmgr)
    {
        super(gmgr);
    }

    @Override
    public void didStartup (PlaceObject plobj)
    {
        super.didStartup(plobj);

        // remember the gameOid so that we have it in didShutdown()
        _gameOid = plobj.getOid();

        // register the game in the registry of world games
        MsoyServer.worldGameReg.gameStartup((GameManager) _plmgr, _gameOid);
    }

    @Override
    public void didShutdown ()
    {
        // remove the game from the registry of world games
        MsoyServer.worldGameReg.gameShutdown((GameManager) _plmgr, _gameOid);
        super.didShutdown();
    }

    /** The game oid. */
    protected int _gameOid;
}

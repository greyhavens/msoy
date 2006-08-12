//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.presents.dobj.AccessController;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.parlor.game.server.GameManager;

import com.threerings.msoy.server.MsoyObjectAccess;

import com.threerings.msoy.game.data.FlashGameObject;

/**
 * A manager for "flash" games in msoy.
 */
public class FlashGameManager extends GameManager
{
    @Override
    protected Class<? extends PlaceObject> getPlaceObjectClass ()
    {
        return FlashGameObject.class;
    }

    @Override
    protected AccessController getAccessController ()
    {
        return MsoyObjectAccess.GAME;
    }
}

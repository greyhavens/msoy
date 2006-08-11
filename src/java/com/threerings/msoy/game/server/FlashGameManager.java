//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.parlor.game.server.GameManager;

import com.threerings.msoy.game.data.FlashGameObject;

/**
 * A manager for "flash" games in msoy.
 */
public class FlashGameManager extends GameManager
{
    protected Class<? extends PlaceObject> getPlaceObjectClass ()
    {
        return FlashGameObject.class;
    }
}

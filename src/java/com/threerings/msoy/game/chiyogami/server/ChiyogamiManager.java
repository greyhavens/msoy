//
// $Id$

package com.threerings.msoy.game.chiyogami.server;

import com.samskivert.util.RandomUtil;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.parlor.game.server.GameManager;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.item.web.StaticMediaDesc;

import com.threerings.msoy.game.server.WorldGameManagerDelegate;
import com.threerings.msoy.game.chiyogami.data.ChiyogamiObject;

/**
 * Manages a game of Chiyogami dance battle.
 */
public class ChiyogamiManager extends GameManager
{
    public ChiyogamiManager ()
    {
        addDelegate(_worldDelegate = new WorldGameManagerDelegate(this));
    }

    @Override
    protected PlaceObject createPlaceObject ()
    {
        return new ChiyogamiObject();
    }

    @Override
    protected void didStartup ()
    {
        _gameobj = (ChiyogamiObject) _plobj;

        super.didStartup();
    }

    @Override
    protected void gameWillStart ()
    {
        super.gameWillStart();

        pickNewBoss();
    }

    /**
     * Pick a new boss.
     */
    protected void pickNewBoss ()
    {
        String boss = RandomUtil.pickRandom(BOSSES);
        _gameobj.startTransaction();
        try {
            _gameobj.setBossHealth(1f);
            _gameobj.setBoss(new StaticMediaDesc(
                MediaDesc.APPLICATION_SHOCKWAVE_FLASH, Item.AVATAR,
                "chiyogami/" + boss));
        } finally {
            _gameobj.commitTransaction();
        }
    }

    /** Our world delegate. */
    protected WorldGameManagerDelegate _worldDelegate;

    /** A casted ref to our gameobject, this hides our superclass _gameobj. */
    protected ChiyogamiObject _gameobj;

    /** TEMP: The filenames of current boss avatars. */
    protected static final String[] BOSSES = { "bboy" };
}

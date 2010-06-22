//
// $Id$

package com.threerings.msoy.game.server;

import java.util.Map;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.util.Name;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.server.PresentsDObjectMgr;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.server.BodyLocator;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.game.data.PlayerObject;

/**
 * Customizes the {@link BodyLocator} and provides a means to lookup a player by id.
 */
@Singleton
public class PlayerLocator extends BodyLocator
{
    /**
     * Returns the player object for the user identified by the given ID if they are resolved on
     * this game server currently, null otherwise.
     */
    @EventThread
    public PlayerObject lookupPlayer (int playerId)
    {
        _omgr.requireEventThread();
        return _online.get(playerId);
    }

    /**
     * Returns the player object for the user identified by the given name if they are resolved
     * on this game server currently, null otherwise.
     */
    @EventThread
    public PlayerObject lookupPlayer (MemberName name)
    {
        return lookupPlayer(name.getMemberId());
    }

    /**
     * Called when a player starts their session to associate the name with the player's
     * distributed object.
     */
    @EventThread
    public void playerLoggedOn (PlayerObject plobj)
    {
        _online.put(plobj.memberName.getMemberId(), plobj);
    }

    /**
     * Called when a player ends their session to clear their name to player object mapping.
     */
    @EventThread
    public void playerLoggedOff (PlayerObject plobj)
    {
        _online.remove(plobj.memberName.getMemberId());
    }

    @Override // from BodyLocator
    public BodyObject lookupBody (Name visibleName)
    {
        _omgr.requireEventThread();
        return _online.get(((MemberName) visibleName).getMemberId());
    }

    /** A mapping from member name to member object for all online members. */
    protected Map<Integer, PlayerObject> _online = Maps.newHashMap();

    @Inject protected PresentsDObjectMgr _omgr;
}

//
// $Id$

package com.threerings.msoy.game.server;

import java.util.Map;

import com.google.inject.Inject;
import com.samskivert.util.HashIntMap;

import com.threerings.parlor.game.server.GameManagerDelegate;
import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.server.MsoyEventLogger;

import static com.threerings.msoy.Log.log;

/**
 * Delegate that keeps track of whether the game played is single- or multi-player,
 * and keeps track of time spent in it for game metrics tracking purposes.
 *
 * Note: game time semantics are different than those used for flow awards:
 * this logs the entire time from joining the table to leaving it,
 * whether or not a 'game' is active or not.
 */
public class EventLoggingDelegate extends GameManagerDelegate
{
    public EventLoggingDelegate (GameContent content)
    {
        _content = content;
    }

    @Override
    public void bodyEntered (int bodyOid)
    {
        super.bodyEntered(bodyOid);

        // track when this occupant entered
        _entries.put(bodyOid, System.currentTimeMillis());

        final MsoyGameManager gmgr = (MsoyGameManager)_plmgr;
        final PlayerObject plobj = (PlayerObject)_omgr.getObject(bodyOid);

        int memberId = plobj.memberName.getMemberId();
        String tracker = (plobj.visitorInfo != null) ? plobj.visitorInfo.id : null;

        _eventLog.gameEntered(memberId, gmgr.isMultiplayer(), tracker);
    }

    @Override
    public void bodyLeft (int bodyOid)
    {
        super.bodyLeft(bodyOid);

        final Long entry = _entries.remove(bodyOid);
        final PlayerObject plobj = (PlayerObject)_omgr.getObject(bodyOid);

        if (entry == null || plobj == null) {
            log.warning("Unknown game player just left!", "bodyOid", bodyOid);
            return;
        }

        // now that they left, log their info
        int memberId = plobj.memberName.getMemberId();
        int seconds = (int)((System.currentTimeMillis() - entry) / 1000);

        final MsoyGameManager gmgr = (MsoyGameManager)_plmgr;
        final String tracker = (plobj.visitorInfo != null) ? plobj.visitorInfo.id : null;
        if (tracker == null) {
            log.warning("Game finished without referral info", "memberId", memberId);
        }

        _eventLog.gameLeft(memberId, _content.game.genre, _content.game.gameId, seconds,
                           gmgr.isMultiplayer(), tracker);
    }

    /** Game description. */
    protected final GameContent _content;

    /** Mapping from player oid to their entry timestamp. */
    Map<Integer, Long> _entries = new HashIntMap<Long>();

    // dependencies
    @Inject protected MsoyEventLogger _eventLog;
}

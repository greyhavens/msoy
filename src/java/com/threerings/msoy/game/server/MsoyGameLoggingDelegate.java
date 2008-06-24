package com.threerings.msoy.game.server;

import java.util.Map;
import java.util.TreeMap;

import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.server.PlaceManagerDelegate;
import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.parlor.game.data.GameConfig;
import com.threerings.parlor.game.server.GameManager;

import static com.threerings.msoy.Log.log;

/**
 * Delegate that keeps track of whether the game played is single- or multi-player, 
 * and keeps track of time spent in it for game metrics tracking purposes.
 * 
 * Note: game time semantics are different than those used for flow awards: 
 * this logs the entire time from joining the table to leaving it, 
 * whether or not a 'game' is active or not. 
 */
public class MsoyGameLoggingDelegate extends PlaceManagerDelegate
{
    public MsoyGameLoggingDelegate (GameContent content, MsoyEventLogger eventLog) 
    {
        _content = content;
        _eventLog = eventLog;
    }
    
    @Override // from PlaceManagerDelegate
    public void didInit (PlaceConfig config)
    {
        super.didInit(config);
        _gmgr = (GameManager)_plmgr;
    }

    @Override
    public void bodyEntered (int bodyOid)
    {
        super.bodyEntered(bodyOid);
        
        // track when this occupant entered, and how many people are playing
        final PlayerObject plobj = (PlayerObject) MsoyGameServer.omgr.getObject(bodyOid);
        final EntryDetails entry = new EntryDetails(plobj);
        entries.put(bodyOid, entry);
    }
    
    @Override 
    public void bodyLeft (int bodyOid)
    {
        super.bodyLeft(bodyOid);

        // now that they left, log their info
        final EntryDetails details = entries.get(bodyOid);
        
        if (details == null) {
            log.warning("Unknown game player just left!", "bodyOid", bodyOid);
            return;
        }
        
        int seconds = (int)((System.currentTimeMillis() - details.entryTimestamp) / 1000);
        int memberId = details.plobj.memberName.getMemberId();
        
        _eventLog.gameLeft(memberId, _content.game.genre, _content.game.gameId, 
                seconds, details.isMultiplayer);
    }

    
    protected MsoyGameManager getGameManager () 
    {
        return (MsoyGameManager) _plmgr;
    }
    
    /** Describes the game player as they enter. */
    protected class EntryDetails {
        /** Player info */
        final PlayerObject plobj;
        /** Time of entry, as provided by {@link System#currentTimeMillis()}. */
        final long entryTimestamp;
        /** True if this game is multiplayer (either a Party game, or with many players). */
        final boolean isMultiplayer;
        
        public EntryDetails (PlayerObject plobj) {
            this.plobj = plobj;
            this.entryTimestamp = System.currentTimeMillis();

            final GameConfig config = _gmgr.getGameConfig();
            this.isMultiplayer = 
                // party games are multiplayer; nobody tracks players' comings and goings
                config.getMatchType() == GameConfig.PARTY ||
                // otherwise, how many people do we have sitting at the table?
                config.players.length > 1;
        }
    }
    
    /** Game description. */
    final protected GameContent _content;
    
    /** Event logger. */
    final protected MsoyEventLogger _eventLog;

    /** Mapping from players to their entry details. */
    Map<Integer, EntryDetails> entries = new TreeMap<Integer, EntryDetails>();
    
    /** An appropriately casted reference to our GameManager. */
    protected GameManager _gmgr;
}

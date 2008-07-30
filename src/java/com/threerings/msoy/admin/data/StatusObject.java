//
// $Id$

package com.threerings.msoy.admin.data;

import com.threerings.presents.data.ConMgrStats;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;

/**
 * Contains server status information that is communicated to admins via the Server Dashboard
 * display.
 */
public class StatusObject extends DObject
{
    /** Used to keep tabs on games in play. */
    public static class GameInfo implements DSet.Entry
    {
        /** The oid of the game object. */
        public Integer gameOid;

        /** The number of human players in the game. */
        public int players;

        // documentation inherited from interface DSet.Entry
        public Comparable<?> getKey () {
            return gameOid;
        }
    }

    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>membersOnline</code> field. */
    public static final String MEMBERS_ONLINE = "membersOnline";

    /** The field name of the <code>serverStartTime</code> field. */
    public static final String SERVER_START_TIME = "serverStartTime";

    /** The field name of the <code>games</code> field. */
    public static final String GAMES = "games";

    /** The field name of the <code>connStats</code> field. */
    public static final String CONN_STATS = "connStats";

    /** The field name of the <code>serverRebootTime</code> field. */
    public static final String SERVER_REBOOT_TIME = "serverRebootTime";
    // AUTO-GENERATED: FIELDS END

    /** The number of members online on this server. */
    public int membersOnline;

    /** The time at which the server started up. */
    public long serverStartTime;

    /** Information on all active games. */
    public DSet<GameInfo> games = new DSet<GameInfo>();

    /** Stats on our connection manager. */
    public ConMgrStats connStats;

    /** The time at which a reboot is scheduled or 0L. */
    public long serverRebootTime;

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>membersOnline</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setMembersOnline (int value)
    {
        int ovalue = this.membersOnline;
        requestAttributeChange(
            MEMBERS_ONLINE, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.membersOnline = value;
    }

    /**
     * Requests that the <code>serverStartTime</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setServerStartTime (long value)
    {
        long ovalue = this.serverStartTime;
        requestAttributeChange(
            SERVER_START_TIME, Long.valueOf(value), Long.valueOf(ovalue));
        this.serverStartTime = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>games</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToGames (StatusObject.GameInfo elem)
    {
        requestEntryAdd(GAMES, games, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>games</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromGames (Comparable<?> key)
    {
        requestEntryRemove(GAMES, games, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>games</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateGames (StatusObject.GameInfo elem)
    {
        requestEntryUpdate(GAMES, games, elem);
    }

    /**
     * Requests that the <code>games</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setGames (DSet<StatusObject.GameInfo> value)
    {
        requestAttributeChange(GAMES, value, this.games);
        DSet<StatusObject.GameInfo> clone = (value == null) ? null : value.typedClone();
        this.games = clone;
    }

    /**
     * Requests that the <code>connStats</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setConnStats (ConMgrStats value)
    {
        ConMgrStats ovalue = this.connStats;
        requestAttributeChange(
            CONN_STATS, value, ovalue);
        this.connStats = value;
    }

    /**
     * Requests that the <code>serverRebootTime</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setServerRebootTime (long value)
    {
        long ovalue = this.serverRebootTime;
        requestAttributeChange(
            SERVER_REBOOT_TIME, Long.valueOf(value), Long.valueOf(ovalue));
        this.serverRebootTime = value;
    }
    // AUTO-GENERATED: METHODS END
}

//
// $Id$

package com.threerings.msoy.server;

import java.util.logging.Level;

import com.samskivert.util.Invoker;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.net.BootstrapData;

import com.threerings.stats.data.Stat;
import com.threerings.stats.data.StatSet;

import com.threerings.whirled.server.WhirledClient;

import com.threerings.msoy.admin.server.RuntimeConfig;

import com.threerings.msoy.data.LurkerName;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyBootstrapData;
import com.threerings.msoy.data.MsoyCredentials;
import com.threerings.msoy.data.MsoyTokenRing;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MsoyEventLogger;

import static com.threerings.msoy.Log.log;

/**
 * Represents an attached Msoy client on the server-side.
 */
public class MsoyClient extends WhirledClient
{
    public MsoyClient (MsoyEventLogger eventLog)
    {
        _eventLog = eventLog;
    }

    /**
     * Called by the peer manager to let us know that our session was forwarded to another server.
     * When this session ends we'll know that it's not *really* the end of the user's session,
     * they're just moving to another server.
     */
    public void setSessionForwarded (boolean sessionForwarded)
    {
        _sessionForwarded = sessionForwarded;
    }

    @Override // from PresentsClient
    protected BootstrapData createBootstrapData ()
    {
        return new MsoyBootstrapData();
    }

    @Override // from PresentsClient
    protected void sessionWillStart ()
    {
        super.sessionWillStart();

        _memobj = (MemberObject) _clobj;
        _memobj.addListener(_idleTracker);

        MsoyAuthenticator.Account acct = (MsoyAuthenticator.Account) _authdata;
        if (acct != null) {
            _memobj.setTokens(acct.tokens);
        } else {
            _memobj.setTokens(new MsoyTokenRing());
        }

        // flag viewing-only clients that way.
        _memobj.viewOnly = ((MsoyCredentials) getCredentials()).featuredPlaceView;

        // start active/idle metrics on this server - the player starts out active
        _memobj.metrics.idle.init(true);
        
        // let our various server entities know that this member logged on
        MsoyServer.memberLoggedOn(_memobj);
    }

    @Override // from PresentsClient
    protected void sessionConnectionClosed ()
    {
        super.sessionConnectionClosed();

        // end our session on disconnect, it's easy enough to get back to where you were with a
        // browser reload
        if (_memobj != null) {
            safeEndSession();
        }
    }

    @Override // from PresentsClient
    protected void sessionDidEnd ()
    {
        super.sessionDidEnd();

        if (_memobj == null) {
            return;
        }

        // let our various server entities know that this member logged off
        MsoyServer.memberLoggedOff(_memobj);
        final int idleSeconds = _idleTracker.getIdleTime();
        final int activeSeconds = _connectTime - idleSeconds;

        // if this is a real logoff event, and this was a player, log what they did; (otherwise, if
        // it's not a logoff, do nothing - the memobj was already forwarded to the new host.)
        if (! _sessionForwarded && ! (_memobj.username instanceof LurkerName)) {
            String sessTok = ((MsoyCredentials)getCredentials()).sessionToken;
            _memobj.metrics.save(_memobj);
            _eventLog.logPlayerMetrics(_memobj, sessTok); 
        }

        // remove our idle tracker
        _memobj.removeListener(_idleTracker);

        // nothing more needs doing for guests
        if (_memobj.isGuest()) {
            _memobj = null;
            return;
        }

        // update session related stats in their MemberRecord
        final MemberName name = _memobj.memberName;
        final StatSet stats = _memobj.stats;
        final int activeMins = Math.round(activeSeconds / 60f);
        MsoyServer.invoker.postUnit(new Invoker.Unit("sessionDidEnd:" + name) {
            public boolean invoke () {
                try {
                    // write out any modified stats
                    Stat[] statArr = new Stat[stats.size()];
                    stats.toArray(statArr);
                    MsoyServer.statRepo.writeModified(name.getMemberId(), statArr);
                    MsoyServer.memberRepo.noteSessionEnded(
                        name.getMemberId(), activeMins, RuntimeConfig.server.humanityReassessment);
                } catch (Exception e) {
                    log.warning("Failed to note ended session [member=" + name + "].", e);
                }
                return false;
            }
        });

        // finally clear out our _memobj reference
        _memobj = null;
    }

    protected class IdleTracker implements AttributeChangeListener
    {
        public int getIdleTime () {
            int idleTime = _idleTime, idleCount = _idleCount;
            if (isIdle(_memobj.status)) {
                idleTime += (int)((System.currentTimeMillis() - _memobj.statusTime) / 1000L);
                idleCount++;
            }
            // TODO: add (idleCount * IDLE_TIMEOUT) to account for the time that a player was
            // *actually* idle before we discovered it?
            return idleTime;
        }

        public void attributeChanged (AttributeChangedEvent event) {
            if (event.getName().equals(MemberObject.STATUS)) {
                boolean idle = isIdle((Byte)event.getValue());
                
                _memobj.metrics.idle.save(_memobj);
                _memobj.metrics.idle.init(!idle);
                
                if (idle) {
                    // log.info(_memobj.who() + " is idle.");
                    _idleStamp = _memobj.statusTime;
                } else if (_idleStamp > 0) {
                    int idleSecs = (int)((_memobj.statusTime - _idleStamp) / 1000L);
                    // log.info(_memobj.who() + " was idle for " + idleSecs + " seconds.");
                    _idleTime += idleSecs;
                    _idleCount++;
                    _idleStamp = 0L;
                }
            }
        }

        protected boolean isIdle (byte status) {
            return (status == OccupantInfo.IDLE || status == MemberObject.AWAY);
        }

        protected int _idleTime;
        protected int _idleCount;
        protected long _idleStamp;
    }

    /** A casted reference to the userobject. */
    protected MemberObject _memobj;

    /** Tracks the time this client spends being idle. */
    protected IdleTracker _idleTracker = new IdleTracker();

    /** We generate events to this fellow. */
    protected MsoyEventLogger _eventLog;

    /** Only valid in {@link #sessionDidEnd}, lets us know if the session is truly over or if the
     * member just went to another server. */
    protected boolean _sessionForwarded;
}

//
// $Id$

package com.threerings.msoy.server;

import com.google.inject.Inject;

import com.samskivert.jdbc.WriteOnlyUnit;
import com.samskivert.util.Invoker;

import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.net.BootstrapData;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.stats.data.Stat;
import com.threerings.stats.data.StatSet;
import com.threerings.stats.server.persist.StatRepository;

import com.threerings.whirled.server.WhirledClient;

import com.threerings.msoy.admin.server.RuntimeConfig;

import com.threerings.msoy.data.LurkerName;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyBootstrapData;
import com.threerings.msoy.data.MsoyCredentials;
import com.threerings.msoy.data.MsoyTokenRing;
import com.threerings.msoy.data.StatType;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.persist.MemberRepository;

import static com.threerings.msoy.Log.log;

/**
 * Represents an attached Msoy client on the server-side.
 */
public class MsoyClient extends WhirledClient
{
    /**
     * Called by the peer manager to let us know that our session was forwarded to another server.
     * When this session ends we'll know that it's not *really* the end of the user's session,
     * they're just moving to another server.
     */
    public void setSessionForwarded (boolean sessionForwarded)
    {
        _sessionForwarded = sessionForwarded;
    }

    /**
     * Returns the number of non-idle seconds this client has passed during this session.
     * <em>Note:</em> this value is only valid <em>before</em> the session has actually ended.
     */
    public int getSessionSeconds ()
    {
        int connectTime = (int)((System.currentTimeMillis() - _networkStamp) / 1000L);
        return connectTime - _idleTracker.getIdleTime();
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
        MsoyCredentials credentials = (MsoyCredentials) getCredentials();

        if (acct != null) {
            _memobj.setTokens(acct.tokens);
        } else {
            _memobj.setTokens(new MsoyTokenRing());
        }

        // if we didn't get referral info from the database already, pull it from
        // our authentication credentials (ie. from the flash / browser cookies)
        if (_memobj.referral == null) {
            _memobj.setReferral(credentials.referral);
        }

        // flag viewing-only clients that way.
        _memobj.viewOnly = credentials.featuredPlaceView;

        // start active/idle metrics on this server - the player starts out active
        _memobj.metrics.idle.init(true);

        // let our various server entities know that this member logged on
        _locator.memberLoggedOn(_memobj);
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
        _locator.memberLoggedOff(_memobj);

        // remove our idle tracker
        _memobj.removeListener(_idleTracker);

        // if this session was forwarded to another server, we can stop here
        if (_sessionForwarded) {
            _memobj = null;
            return;
        }

        // if this this was a player or guest (but not a lurker), log their stats
        if (!(_memobj.username instanceof LurkerName)) {
            String sessTok = ((MsoyCredentials)getCredentials()).sessionToken;
            _memobj.metrics.save(_memobj);
            _eventLog.logPlayerMetrics(_memobj, sessTok);
        }

        // if this was a member, record some end of session related info to the database
        if (!_memobj.isGuest()) {
            final int activeMins = Math.round((_memobj.sessionSeconds + _connectTime -
                                               _idleTracker.getIdleTime()) / 60f);
            final int memberId = _memobj.getMemberId();
            final StatSet stats = _memobj.stats;
            log.info("Session ended [id=" + memberId + ", amins=" + activeMins + "].");
            stats.incrementStat(StatType.MINUTES_ACTIVE, activeMins);
            _invoker.postUnit(new WriteOnlyUnit("sessionDidEnd:" + _memobj.memberName) {
                public void invokePersist () throws Exception {
                    // write out any modified stats
                    _statRepo.writeModified(memberId, stats.toArray(new Stat[stats.size()]));
                    // increment their session and minutes online counters
                    _memberRepo.noteSessionEnded(
                        memberId, activeMins, RuntimeConfig.server.humanityReassessment);
                }
            });
        }

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

    /** Only valid in {@link #sessionDidEnd}, lets us know if the session is truly over or if the
     * member just went to another server. */
    protected boolean _sessionForwarded;

    // dependent services
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected MemberLocator _locator;
    @Inject protected StatRepository _statRepo;
    @Inject protected MemberRepository _memberRepo;
}
